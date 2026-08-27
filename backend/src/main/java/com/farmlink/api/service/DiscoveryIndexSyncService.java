package com.farmlink.api.service;

import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class DiscoveryIndexSyncService {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryIndexSyncService.class);
    private static final int BOUNDED_MAX_SCAN = 50000;
    private static final int BATCH_SIZE = 1000;

    private final Firestore firestore;

    public DiscoveryIndexSyncService(Firestore firestore) {
        this.firestore = firestore;
    }

    @CacheEvict(value = {"states", "districts", "markets", "crops", "marketCrops"}, allEntries = true)
    public Map<String, Integer> syncDiscoveryIndexFromObservedData() {
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("statesIndexed", 0);
        metrics.put("districtsIndexed", 0);
        metrics.put("marketsIndexed", 0);
        metrics.put("cropsIndexed", 0);

        if (firestore == null) {
            logger.warn("Firestore instance is null. Skipping discovery index sync.");
            return metrics;
        }

        try {
            logger.info("Starting bounded discovery index sync from existing Firestore price records...");

            Set<String> uniqueStates = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, Set<String>> stateDistrictsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, Map<String, Object>> marketMap = new LinkedHashMap<>();
            Map<String, Map<String, Object>> cropMap = new LinkedHashMap<>();
            Set<String> marketCropKeys = new HashSet<>();

            // Bounded scan over marketPrices using pagination to prevent memory overflow
            DocumentSnapshot lastDoc = null;
            int totalProcessed = 0;

            while (totalProcessed < BOUNDED_MAX_SCAN) {
                Query query = firestore.collection("marketPrices")
                        .limit(BATCH_SIZE);

                if (lastDoc != null) {
                    query = query.startAfter(lastDoc);
                }

                QuerySnapshot snapshot = query.get().get();
                if (snapshot == null || snapshot.isEmpty()) {
                    break;
                }

                List<QueryDocumentSnapshot> docs = snapshot.getDocuments();
                for (QueryDocumentSnapshot doc : docs) {
                    totalProcessed++;
                    String obsState = doc.getString("observedState");
                    String obsDistrict = doc.getString("observedDistrict");
                    String marketId = doc.getString("marketId");
                    String obsMarketName = doc.getString("observedMarketName");
                    String cropId = doc.getString("cropId");
                    String obsCommodity = doc.getString("observedCommodityName");

                    if (obsState != null && !obsState.trim().isEmpty()) {
                        String stClean = obsState.trim();
                        uniqueStates.add(stClean);
                        if (obsDistrict != null && !obsDistrict.trim().isEmpty()) {
                            stateDistrictsMap.computeIfAbsent(stClean, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
                                    .add(obsDistrict.trim());
                        }
                    }

                    if (marketId != null && !marketId.trim().isEmpty() && obsMarketName != null) {
                        if (!marketMap.containsKey(marketId)) {
                            Map<String, Object> mData = new HashMap<>();
                            mData.put("name", obsMarketName);
                            mData.put("code", "OBS-" + Math.abs(marketId.hashCode()));
                            mData.put("type", "MANDI");
                            mData.put("status", "ACTIVE");
                            Map<String, String> loc = new HashMap<>();
                            loc.put("state", obsState != null ? obsState.trim() : "");
                            loc.put("district", obsDistrict != null ? obsDistrict.trim() : "");
                            mData.put("location", loc);
                            mData.put("updatedAt", Instant.now().toString());
                            marketMap.put(marketId, mData);
                        }
                    }

                    if (cropId != null && !cropId.trim().isEmpty() && obsCommodity != null) {
                        if (!cropMap.containsKey(cropId)) {
                            Map<String, Object> cData = new HashMap<>();
                            cData.put("name", obsCommodity);
                            cData.put("category", "AGRICULTURAL_COMMODITY");
                            cData.put("status", "ACTIVE");
                            cData.put("updatedAt", Instant.now().toString());
                            cropMap.put(cropId, cData);
                        }
                    }

                    if (marketId != null && cropId != null) {
                        marketCropKeys.add(marketId + "_" + cropId);
                    }
                }

                lastDoc = docs.get(docs.size() - 1);
                if (docs.size() < BATCH_SIZE) {
                    break;
                }
            }

            int districtCount = stateDistrictsMap.values().stream().mapToInt(Set::size).sum();

            // Upsert into lightweight collections in Firestore
            writeLightweightLocations(uniqueStates, stateDistrictsMap);
            writeLightweightMarkets(marketMap);
            writeLightweightCrops(cropMap);
            writeLightweightMarketCrops(marketCropKeys);

            metrics.put("statesIndexed", uniqueStates.size());
            metrics.put("districtsIndexed", districtCount);
            metrics.put("marketsIndexed", marketMap.size());
            metrics.put("cropsIndexed", cropMap.size());

            logger.info("Bounded discovery index sync complete. Processed {} price records. Unique States: {}, Districts: {}, Markets: {}, Crops: {}",
                    totalProcessed, uniqueStates.size(), districtCount, marketMap.size(), cropMap.size());

        } catch (Exception e) {
            logger.error("Error executing bounded discovery index sync: {}", e.getMessage(), e);
        }

        return metrics;
    }

    private void writeLightweightLocations(Set<String> states, Map<String, Set<String>> stateDistrictsMap) {
        for (Map.Entry<String, Set<String>> entry : stateDistrictsMap.entrySet()) {
            String state = entry.getKey();
            for (String district : entry.getValue()) {
                String docId = "loc-" + Math.abs((state + "_" + district).hashCode());
                Map<String, Object> doc = new HashMap<>();
                doc.put("state", state);
                doc.put("district", district);
                doc.put("updatedAt", Instant.now().toString());
                try {
                    firestore.collection("locations").document(docId).set(doc, SetOptions.merge());
                } catch (Exception ignored) {}
            }
        }
    }

    private void writeLightweightMarkets(Map<String, Map<String, Object>> marketMap) {
        WriteBatch batch = firestore.batch();
        int count = 0;
        for (Map.Entry<String, Map<String, Object>> entry : marketMap.entrySet()) {
            batch.set(firestore.collection("markets").document(entry.getKey()), entry.getValue(), SetOptions.merge());
            count++;
            if (count % 400 == 0) {
                try {
                    batch.commit().get();
                    batch = firestore.batch();
                } catch (Exception ignored) {}
            }
        }
        if (count % 400 != 0) {
            try {
                batch.commit().get();
            } catch (Exception ignored) {}
        }
    }

    private void writeLightweightCrops(Map<String, Map<String, Object>> cropMap) {
        WriteBatch batch = firestore.batch();
        int count = 0;
        for (Map.Entry<String, Map<String, Object>> entry : cropMap.entrySet()) {
            batch.set(firestore.collection("crops").document(entry.getKey()), entry.getValue(), SetOptions.merge());
            count++;
            if (count % 400 == 0) {
                try {
                    batch.commit().get();
                    batch = firestore.batch();
                } catch (Exception ignored) {}
            }
        }
        if (count % 400 != 0) {
            try {
                batch.commit().get();
            } catch (Exception ignored) {}
        }
    }

    private void writeLightweightMarketCrops(Set<String> marketCropKeys) {
        WriteBatch batch = firestore.batch();
        int count = 0;
        for (String key : marketCropKeys) {
            String[] parts = key.split("_", 2);
            if (parts.length == 2) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("marketId", parts[0]);
                doc.put("cropId", parts[1]);
                doc.put("status", "ACTIVE");
                doc.put("updatedAt", Instant.now().toString());
                batch.set(firestore.collection("marketCrops").document(key), doc, SetOptions.merge());
                count++;
                if (count % 400 == 0) {
                    try {
                        batch.commit().get();
                        batch = firestore.batch();
                    } catch (Exception ignored) {}
                }
            }
        }
        if (count % 400 != 0) {
            try {
                batch.commit().get();
            } catch (Exception ignored) {}
        }
    }
}
