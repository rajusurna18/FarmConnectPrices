package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // Reference Seed Markets (Used for development reference & offline fallback)
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

    // Reference Market-Crop Mappings
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

    // ==========================================
    // READ-ONLY MARKET DISCOVERY OPERATIONS
    // ==========================================

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
            if (state != null && !state.trim().isEmpty() && !state.equalsIgnoreCase(m.getLocation().getState())) {
                continue;
            }
            if (district != null && !district.trim().isEmpty() && !district.equalsIgnoreCase(m.getLocation().getDistrict())) {
                continue;
            }
            if (mandal != null && !mandal.trim().isEmpty() && !mandal.equalsIgnoreCase(m.getLocation().getMandal())) {
                continue;
            }
            if (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase(m.getType())) {
                continue;
            }
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase(m.getStatus())) {
                continue;
            }
            if (cropId != null && !cropId.trim().isEmpty()) {
                List<MarketCropResponse> crops = getMarketCrops(m.getId());
                boolean supportsCrop = crops.stream().anyMatch(c -> c.getCropId().equalsIgnoreCase(cropId.trim()));
                if (!supportsCrop) {
                    continue;
                }
            }

            int cropCount = getMarketCrops(m.getId()).size();
            summaries.add(new MarketSummaryResponse(
                    m.getId(),
                    m.getName(),
                    m.getCode(),
                    m.getType(),
                    m.getLocation().getState(),
                    m.getLocation().getDistrict(),
                    m.getLocation().getMandal(),
                    m.getStatus(),
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
                DocumentSnapshot doc = firestore.collection("markets").document(marketId).get().get();
                if (doc.exists()) {
                    return mapDocToMarketResponse(doc);
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

    public List<MarketCropResponse> getMarketCrops(String marketId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<MarketCropResponse> marketCrops = new ArrayList<>();

        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("marketCrops")
                        .whereEqualTo("marketId", marketId)
                        .get().get();

                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String cropId = doc.getString("cropId");
                        String status = doc.getString("status") != null ? doc.getString("status") : STATUS_ACTIVE;
                        String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
                        String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

                        CropResponse crop = cropMasterService.getCropById(cropId);
                        String cropName = crop != null ? crop.getName() : cropId;
                        String cropCategory = crop != null ? crop.getCategory() : "OTHER";
                        String cropScientific = crop != null ? crop.getScientificName() : "";

                        marketCrops.add(new MarketCropResponse(id, marketId, cropId, cropName, cropCategory, cropScientific, status, createdAt, updatedAt));
                    }
                    return marketCrops;
                }
            } catch (Exception e) {
                logger.warn("Error fetching marketCrops for marketId {} from Firestore: {}", marketId, e.getMessage());
            }
        }

        // Fallback reference seed mappings
        List<String> seedCropIds = SEED_MARKET_CROPS.getOrDefault(marketId, Collections.emptyList());
        String nowIso = Instant.now().toString();
        for (String cId : seedCropIds) {
            CropResponse crop = cropMasterService.getCropById(cId);
            if (crop != null) {
                String deterministicId = marketId + "_" + crop.getId();
                marketCrops.add(new MarketCropResponse(
                        deterministicId,
                        marketId,
                        crop.getId(),
                        crop.getName(),
                        crop.getCategory(),
                        crop.getScientificName(),
                        STATUS_ACTIVE,
                        nowIso, nowIso
                ));
            }
        }

        return marketCrops;
    }

    // ==========================================
    // HELPERS & DATA ACCESS
    // ==========================================

    private List<MarketResponse> fetchAllMarkets() {
        List<MarketResponse> list = new ArrayList<>();
        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("markets").get().get();
                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        list.add(mapDocToMarketResponse(doc));
                    }
                    return list;
                }
            } catch (Exception e) {
                logger.warn("Could not fetch markets from Firestore: {}", e.getMessage());
            }
        }
        return DEFAULT_MARKETS;
    }

    private MarketResponse mapDocToMarketResponse(DocumentSnapshot doc) {
        String id = doc.getId();
        String name = doc.getString("name");
        String code = doc.getString("code");
        String type = doc.getString("type");
        Double lat = doc.getDouble("latitude");
        Double lng = doc.getDouble("longitude");
        String status = doc.getString("status") != null ? doc.getString("status") : STATUS_ACTIVE;

        Map<String, Object> locMap = (Map<String, Object>) doc.get("location");
        LocationDto loc = locMap != null ? new LocationDto(
                (String) locMap.get("state"),
                (String) locMap.get("district"),
                (String) locMap.get("mandal"),
                (String) locMap.get("village"),
                (String) locMap.get("pincode")
        ) : new LocationDto();

        String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
        String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

        return new MarketResponse(id, name, code, type, loc, lat, lng, status, createdAt, updatedAt);
    }
}
