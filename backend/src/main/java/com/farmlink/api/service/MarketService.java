package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MarketService {

    private static final Logger logger = LoggerFactory.getLogger(MarketService.class);

    public static final String TYPE_MANDI = "MANDI";
    public static final String TYPE_RYTHU_BAZAAR = "RYTHU_BAZAAR";
    public static final String TYPE_WHOLESALE_MARKET = "WHOLESALE_MARKET";
    public static final String TYPE_LOCAL_MARKET = "LOCAL_MARKET";
    public static final String TYPE_OTHER = "OTHER";

    public static final Set<String> VALID_TYPES = Set.of(
            TYPE_MANDI, TYPE_RYTHU_BAZAAR, TYPE_WHOLESALE_MARKET, TYPE_LOCAL_MARKET, TYPE_OTHER
    );

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private final Firestore firestore;
    private final CropMasterService cropMasterService;
    private final FirestoreQuotaGuard quotaGuard;
    private final AtomicReference<Map<String, MarketResponse>> marketMapCache = new AtomicReference<>();

    public MarketService(Firestore firestore, CropMasterService cropMasterService) {
        this(firestore, cropMasterService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MarketService(Firestore firestore, CropMasterService cropMasterService, @org.springframework.beans.factory.annotation.Autowired(required = false) FirestoreQuotaGuard quotaGuard) {
        this.firestore = firestore;
        this.cropMasterService = cropMasterService;
        this.quotaGuard = quotaGuard;
    }

    public List<MarketSummaryResponse> getMarkets(
            String state,
            String district,
            String mandal,
            String type,
            String status,
            String cropId,
            Integer limit
    ) {
        List<MarketResponse> fullList = fetchAllMarkets();
        List<MarketSummaryResponse> summaries = new ArrayList<>();

        int maxResults = (limit != null && limit > 0 && limit <= 100) ? limit : 50;

        for (MarketResponse m : fullList) {
            if (m == null) {
                continue;
            }

            LocationDto loc = m.getLocation() != null ? m.getLocation() : new LocationDto();
            String mState = loc.getState();
            String mDistrict = loc.getDistrict();
            String mMandal = loc.getMandal();
            String mType = m.getType();
            String mStatus = m.getStatus();

            if (state != null && !state.trim().isEmpty()) {
                if (mState == null || !mState.equalsIgnoreCase(state.trim())) {
                    continue;
                }
            }
            if (district != null && !district.trim().isEmpty()) {
                if (mDistrict == null || !mDistrict.equalsIgnoreCase(district.trim())) {
                    continue;
                }
            }
            if (mandal != null && !mandal.trim().isEmpty()) {
                if (mMandal == null || !mMandal.equalsIgnoreCase(mandal.trim())) {
                    continue;
                }
            }
            if (type != null && !type.trim().isEmpty()) {
                if (mType == null || !mType.equalsIgnoreCase(type.trim())) {
                    continue;
                }
            }
            if (status != null && !status.trim().isEmpty()) {
                if (mStatus == null || !mStatus.equalsIgnoreCase(status.trim())) {
                    continue;
                }
            }

            // Perform crop filter check only when cropId is requested to prevent unnecessary N+1 queries
            int cropCount = 0;
            if (cropId != null && !cropId.trim().isEmpty()) {
                List<MarketCropResponse> crops = getMarketCrops(m.getId());
                boolean supportsCrop = crops != null && crops.stream().anyMatch(c ->
                        c != null && c.getCropId() != null && c.getCropId().equalsIgnoreCase(cropId.trim())
                );
                if (!supportsCrop) {
                    continue;
                }
                cropCount = crops != null ? crops.size() : 0;
            }

            summaries.add(new MarketSummaryResponse(
                    m.getId(),
                    m.getName() != null ? m.getName() : "Unnamed Market",
                    m.getCode() != null ? m.getCode() : m.getId(),
                    mType != null ? mType : TYPE_OTHER,
                    mState,
                    mDistrict,
                    mMandal,
                    mStatus != null ? mStatus : STATUS_ACTIVE,
                    cropCount
            ));

            if (summaries.size() >= maxResults) {
                break;
            }
        }

        return summaries;
    }

    @Cacheable(value = "marketMap", sync = true)
    public Map<String, MarketResponse> getMarketMap() {
        Map<String, MarketResponse> cached = marketMapCache.get();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<MarketResponse> list = fetchAllMarkets();
        Map<String, MarketResponse> map = new HashMap<>();
        for (MarketResponse m : list) {
            if (m != null && m.getId() != null) {
                map.put(m.getId().toLowerCase(Locale.ROOT), m);
            }
        }
        Map<String, MarketResponse> unmodifiable = Collections.unmodifiableMap(map);
        marketMapCache.compareAndSet(null, unmodifiable);
        return marketMapCache.get() != null ? marketMapCache.get() : unmodifiable;
    }

    public MarketResponse getMarketById(String marketId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Market ID cannot be empty.");
        }

        String targetId = marketId.trim().toLowerCase(Locale.ROOT);
        Map<String, MarketResponse> map = getMarketMap();
        MarketResponse cached = map.get(targetId);
        if (cached != null) {
            return cached;
        }

        // obs-mkt-* observation markets or unmapped IDs: do NOT issue failing Firestore queries
        throw new NoSuchElementException("Market not found with ID: " + marketId);
    }

    @Cacheable(value = "marketCrops", key = "#marketId", sync = true)
    public List<MarketCropResponse> getMarketCrops(String marketId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<MarketCropResponse> marketCrops = new ArrayList<>();
        if (firestore != null) {
            try {
                var colRef = firestore.collection("marketCrops");
                if (colRef != null) {
                    var future = colRef.whereEqualTo("marketId", marketId).get();
                    if (future != null) {
                        QuerySnapshot snapshot = future.get();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                if (doc == null) continue;
                                String id = doc.getId();
                                String cropId = doc.getString("cropId");
                                if (cropId == null || cropId.trim().isEmpty()) continue;

                                String status = doc.getString("status") != null ? doc.getString("status") : STATUS_ACTIVE;
                                String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
                                String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

                                CropResponse crop = cropMasterService.getCropById(cropId);
                                String cropName = crop != null && crop.getName() != null ? crop.getName() : cropId;
                                String cropCategory = crop != null && crop.getCategory() != null ? crop.getCategory() : "AGRICULTURAL_COMMODITY";
                                String cropScientific = crop != null && crop.getScientificName() != null ? crop.getScientificName() : "";

                                marketCrops.add(new MarketCropResponse(id, marketId, cropId, cropName, cropCategory, cropScientific, status, createdAt, updatedAt));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching marketCrops for marketId {} from Firestore: {}", marketId, e.getMessage());
                throw new RuntimeException("Could not fetch market crops from Firestore: " + e.getMessage(), e);
            }
        }

        return marketCrops;
    }

    @Cacheable(value = "markets", sync = true)
    public List<MarketResponse> fetchAllMarkets() {
        Map<String, MarketResponse> cachedMap = marketMapCache.get();
        if (cachedMap != null && !cachedMap.isEmpty()) {
            return new ArrayList<>(cachedMap.values());
        }

        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        if (firestore == null) {
            return Collections.emptyList();
        }

        List<MarketResponse> list = new ArrayList<>();
        try {
            var colRef = firestore.collection("markets");
            if (colRef != null) {
                var future = colRef.get();
                if (future != null) {
                    QuerySnapshot snapshot = future.get();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                MarketResponse m = mapDocToMarketResponse(doc);
                                if (m != null) {
                                    list.add(m);
                                }
                            } catch (Exception e) {
                                logger.warn("Skipping malformed market document ID {}: {}", doc.getId(), e.getMessage());
                            }
                        }
                    }
                }
            }
            Map<String, MarketResponse> map = new HashMap<>();
            for (MarketResponse m : list) {
                if (m != null && m.getId() != null) {
                    map.put(m.getId().toLowerCase(Locale.ROOT), m);
                }
            }
            marketMapCache.compareAndSet(null, Collections.unmodifiableMap(map));
            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
            return list;
        } catch (Exception e) {
            if (quotaGuard != null && isQuotaExhaustedError(e)) {
                quotaGuard.recordQuotaExhaustion(e);
            }
            logger.error("Could not fetch markets from Firestore: {}", e.getMessage());
            throw new RuntimeException("Could not fetch markets from Firestore: " + e.getMessage(), e);
        }
    }

    private boolean isQuotaExhaustedError(Throwable t) {
        if (t == null) return false;
        String msg = t.getMessage();
        if (msg != null && (msg.contains("RESOURCE_EXHAUSTED") || msg.contains("Quota exceeded"))) {
            return true;
        }
        return isQuotaExhaustedError(t.getCause());
    }

    private MarketResponse mapDocToMarketResponse(DocumentSnapshot doc) {
        if (doc == null) return null;
        String id = doc.getId();
        String name = doc.getString("name");
        String code = doc.getString("code");
        String type = doc.getString("type");
        Double lat = getDoubleValue(doc, "latitude");
        Double lng = getDoubleValue(doc, "longitude");
        String status = doc.getString("status") != null ? doc.getString("status") : STATUS_ACTIVE;

        LocationDto loc;
        Object locObj = doc.get("location");
        if (locObj instanceof Map<?, ?> locMap) {
            loc = new LocationDto(
                    locMap.get("state") != null ? locMap.get("state").toString() : null,
                    locMap.get("district") != null ? locMap.get("district").toString() : null,
                    locMap.get("mandal") != null ? locMap.get("mandal").toString() : null,
                    locMap.get("village") != null ? locMap.get("village").toString() : null,
                    locMap.get("pincode") != null ? locMap.get("pincode").toString() : null
            );
        } else {
            loc = new LocationDto();
        }

        String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
        String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

        return new MarketResponse(id, name, code, type, loc, lat, lng, status, createdAt, updatedAt);
    }

    private Double getDoubleValue(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
