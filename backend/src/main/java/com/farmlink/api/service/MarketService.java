package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

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

    public static final List<MarketResponse> DEFAULT_MARKETS = List.of(
            new MarketResponse(
                    "mkt-guntur-mandi",
                    "Guntur Agricultural Market",
                    "GNT-MND-001",
                    TYPE_MANDI,
                    new LocationDto("Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
                    16.2974, 80.4398,
                    STATUS_ACTIVE,
                    "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
            ),
            new MarketResponse(
                    "mkt-enumamula-warangal",
                    "Enumamula Market Yard",
                    "WGL-MND-002",
                    TYPE_MANDI,
                    new LocationDto("Telangana", "Warangal", "Warangal Urban", "Enumamula", "506002"),
                    17.9784, 79.6000,
                    STATUS_ACTIVE,
                    "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
            ),
            new MarketResponse(
                    "mkt-malakpet-hyderabad",
                    "Malakpet Wholesale Market",
                    "HYD-WHL-003",
                    TYPE_WHOLESALE_MARKET,
                    new LocationDto("Telangana", "Hyderabad", "Bahadurpura", "Malakpet", "500036"),
                    17.3753, 78.4983,
                    STATUS_ACTIVE,
                    "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
            ),
            new MarketResponse(
                    "mkt-devanahalli-bengaluru",
                    "Devanahalli Rythu Bazaar",
                    "BLR-RYT-004",
                    TYPE_RYTHU_BAZAAR,
                    new LocationDto("Karnataka", "Bengaluru Rural", "Devanahalli", "Devanahalli Village", "562110"),
                    13.2458, 77.7124,
                    STATUS_ACTIVE,
                    "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
            )
    );

    private static final Map<String, List<String>> SEED_MARKET_CROPS = Map.of(
            "mkt-guntur-mandi", List.of("crop-chilli", "crop-paddy", "crop-cotton", "crop-turmeric"),
            "mkt-enumamula-warangal", List.of("crop-chilli", "crop-cotton", "crop-maize", "crop-paddy"),
            "mkt-malakpet-hyderabad", List.of("crop-tomato", "crop-onion", "crop-paddy", "crop-mango"),
            "mkt-devanahalli-bengaluru", List.of("crop-tomato", "crop-onion", "crop-maize")
    );

    public MarketService(Firestore firestore, CropMasterService cropMasterService) {
        this.firestore = firestore;
        this.cropMasterService = cropMasterService;
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

    public MarketResponse getMarketById(String marketId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Market ID cannot be empty.");
        }

        if (firestore != null) {
            try {
                var colRef = firestore.collection("markets");
                if (colRef != null) {
                    DocumentSnapshot doc = colRef.document(marketId).get().get();
                    if (doc != null && doc.exists()) {
                        MarketResponse response = mapDocToMarketResponse(doc);
                        if (response != null) {
                            return response;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error fetching market ID {} from Firestore: {}", marketId, e.getMessage());
            }
        }

        for (MarketResponse m : DEFAULT_MARKETS) {
            if (m.getId().equalsIgnoreCase(marketId.trim())) {
                return m;
            }
        }

        throw new NoSuchElementException("Market not found with ID: " + marketId);
    }

    @Cacheable(value = "marketCrops", key = "#marketId")
    public List<MarketCropResponse> getMarketCrops(String marketId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<MarketCropResponse> marketCrops = new ArrayList<>();
        if (firestore != null) {
            try {
                var colRef = firestore.collection("marketCrops");
                if (colRef != null) {
                    QuerySnapshot snapshot = colRef
                            .whereEqualTo("marketId", marketId)
                            .get().get();

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
            } catch (Exception e) {
                logger.warn("Error fetching marketCrops for marketId {} from Firestore: {}", marketId, e.getMessage());
            }
        }

        if (marketCrops.isEmpty()) {
            List<String> seedCropIds = SEED_MARKET_CROPS.getOrDefault(marketId, Collections.emptyList());
            String nowIso = Instant.now().toString();
            for (String cId : seedCropIds) {
                CropResponse crop = cropMasterService.getCropById(cId);
                if (crop != null) {
                    marketCrops.add(new MarketCropResponse(
                            marketId + "_" + crop.getId(), marketId, crop.getId(),
                            crop.getName(), crop.getCategory(), crop.getScientificName(),
                            STATUS_ACTIVE, nowIso, nowIso
                    ));
                }
            }
        }

        return marketCrops;
    }

    @Cacheable(value = "markets")
    public List<MarketResponse> fetchAllMarkets() {
        if (firestore == null) {
            return DEFAULT_MARKETS;
        }

        List<MarketResponse> list = new ArrayList<>();
        try {
            var colRef = firestore.collection("markets");
            if (colRef != null) {
                QuerySnapshot snapshot = colRef.get().get();
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
        } catch (Exception e) {
            logger.error("Could not fetch markets from Firestore: {}", e.getMessage());
        }

        if (list.isEmpty()) {
            return DEFAULT_MARKETS;
        }
        return list;
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

