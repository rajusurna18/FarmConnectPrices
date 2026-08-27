package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketPriceService {

    private static final Logger logger = LoggerFactory.getLogger(MarketPriceService.class);

    public static final String QUALITY_VERIFIED = "VERIFIED";
    public static final String QUALITY_UNVERIFIED = "UNVERIFIED";
    public static final String QUALITY_REJECTED = "REJECTED";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    public static final String CURRENCY_INR = "INR";
    public static final String UNIT_QUINTAL = "QUINTAL";
    public static final String UNIT_KG = "KG";
    public static final String UNIT_TONNE = "TONNE";

    private final Firestore firestore;
    private final MarketService marketService;
    private final CropMasterService cropMasterService;
    private final PriceUnitConversionService conversionService;

    // Reference Development Seed Data (Clearly marked DEVELOPMENT DATA, never fake government data)
    public static final List<MarketPriceResponse> DEFAULT_PRICES;

    static {
        MarketPriceSourceDto seedSource = new MarketPriceSourceDto(
                "IMPORTED_DATA",
                "Development Reference Seed Data",
                "ref-dev-seed-2026"
        );

        List<MarketPriceResponse> list = new ArrayList<>();

        // Guntur Mandi - Red Chilli (Verified)
        list.add(new MarketPriceResponse(
                "prc-gnt-chilli-20260826",
                new MarketSummaryResponse("mkt-guntur-mandi", "Guntur Agricultural Market", "GNT-MND-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4),
                new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"),
                "2026-08-26",
                "2026-08-26T06:00:00Z",
                18500.0, 22500.0, 20500.0,
                CURRENCY_INR, UNIT_QUINTAL, UNIT_QUINTAL, false, 1.0,
                seedSource,
                QUALITY_VERIFIED, STATUS_ACTIVE,
                "2026-08-26T06:00:00Z", "2026-08-26T06:00:00Z"
        ));

        // Guntur Mandi - Red Chilli (Historical Day 2)
        list.add(new MarketPriceResponse(
                "prc-gnt-chilli-20260825",
                new MarketSummaryResponse("mkt-guntur-mandi", "Guntur Agricultural Market", "GNT-MND-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4),
                new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"),
                "2026-08-25",
                "2026-08-25T06:00:00Z",
                18200.0, 22100.0, 20200.0,
                CURRENCY_INR, UNIT_QUINTAL, UNIT_QUINTAL, false, 1.0,
                seedSource,
                QUALITY_VERIFIED, STATUS_ACTIVE,
                "2026-08-25T06:00:00Z", "2026-08-25T06:00:00Z"
        ));

        // Enumamula Warangal - Paddy / Rice (Verified)
        list.add(new MarketPriceResponse(
                "prc-wgl-paddy-20260826",
                new MarketSummaryResponse("mkt-enumamula-warangal", "Enumamula Market Yard", "WGL-MND-002", "MANDI", "Telangana", "Warangal", "Warangal Urban", "ACTIVE", 4),
                new CropResponse("crop-paddy", "Rice / Paddy", "CEREAL", "Oryza sativa", "ACTIVE"),
                "2026-08-26",
                "2026-08-26T07:30:00Z",
                2180.0, 2450.0, 2320.0,
                CURRENCY_INR, UNIT_QUINTAL, UNIT_QUINTAL, false, 1.0,
                seedSource,
                QUALITY_VERIFIED, STATUS_ACTIVE,
                "2026-08-26T07:30:00Z", "2026-08-26T07:30:00Z"
        ));

        // Malakpet Hyderabad - Tomato (Unverified observation)
        list.add(new MarketPriceResponse(
                "prc-hyd-tomato-20260826",
                new MarketSummaryResponse("mkt-malakpet-hyderabad", "Malakpet Wholesale Market", "HYD-WHL-003", "WHOLESALE_MARKET", "Telangana", "Hyderabad", "Bahadurpura", "ACTIVE", 4),
                new CropResponse("crop-tomato", "Tomato", "VEGETABLE", "Solanum lycopersicum", "ACTIVE"),
                "2026-08-26",
                "2026-08-26T08:00:00Z",
                1400.0, 1900.0, 1650.0,
                CURRENCY_INR, UNIT_QUINTAL, UNIT_QUINTAL, false, 1.0,
                seedSource,
                QUALITY_UNVERIFIED, STATUS_ACTIVE,
                "2026-08-26T08:00:00Z", "2026-08-26T08:00:00Z"
        ));

        // Devanahalli Bengaluru - Onion (Verified)
        list.add(new MarketPriceResponse(
                "prc-blr-onion-20260826",
                new MarketSummaryResponse("mkt-devanahalli-bengaluru", "Devanahalli Rythu Bazaar", "BLR-RYT-004", "RYTHU_BAZAAR", "Karnataka", "Bengaluru Rural", "Devanahalli", "ACTIVE", 3),
                new CropResponse("crop-onion", "Onion", "VEGETABLE", "Allium cepa", "ACTIVE"),
                "2026-08-26",
                "2026-08-26T09:15:00Z",
                2400.0, 3100.0, 2750.0,
                CURRENCY_INR, UNIT_QUINTAL, UNIT_QUINTAL, false, 1.0,
                seedSource,
                QUALITY_VERIFIED, STATUS_ACTIVE,
                "2026-08-26T09:15:00Z", "2026-08-26T09:15:00Z"
        ));

        DEFAULT_PRICES = Collections.unmodifiableList(list);
    }

    public MarketPriceService(
            Firestore firestore,
            MarketService marketService,
            CropMasterService cropMasterService,
            PriceUnitConversionService conversionService
    ) {
        this.firestore = firestore;
        this.marketService = marketService;
        this.cropMasterService = cropMasterService;
        this.conversionService = conversionService;
    }

    public static boolean validatePriceRecord(double minPrice, double maxPrice, double modalPrice) {
        if (minPrice < 0 || maxPrice < 0 || modalPrice < 0) {
            return false;
        }
        return minPrice <= modalPrice && modalPrice <= maxPrice;
    }

    public List<MarketPriceSummaryResponse> getMarketPrices(
            String marketId,
            String cropId,
            String priceDate,
            String fromDate,
            String toDate,
            String qualityStatus,
            String unit,
            String state,
            String district,
            Integer limit
    ) {
        List<MarketPriceResponse> fullPrices = fetchAllMarketPrices();

        int maxResults = (limit != null && limit > 0 && limit <= 100) ? limit : 50;

        List<MarketPriceResponse> filtered = fullPrices.stream().filter(p -> {
            // Exclude REJECTED unless specifically requested
            if (qualityStatus != null && !qualityStatus.trim().isEmpty()) {
                if (!p.getQualityStatus().equalsIgnoreCase(qualityStatus.trim())) {
                    return false;
                }
            } else {
                if (QUALITY_REJECTED.equalsIgnoreCase(p.getQualityStatus())) {
                    return false;
                }
            }

            if (marketId != null && !marketId.trim().isEmpty() && !p.getMarket().getId().equalsIgnoreCase(marketId.trim())) {
                return false;
            }
            if (cropId != null && !cropId.trim().isEmpty() && !p.getCrop().getId().equalsIgnoreCase(cropId.trim())) {
                return false;
            }
            if (priceDate != null && !priceDate.trim().isEmpty() && !p.getPriceDate().equals(priceDate.trim())) {
                return false;
            }
            if (fromDate != null && !fromDate.trim().isEmpty() && p.getPriceDate().compareTo(fromDate.trim()) < 0) {
                return false;
            }
            if (toDate != null && !toDate.trim().isEmpty() && p.getPriceDate().compareTo(toDate.trim()) > 0) {
                return false;
            }
            // Unit filtering: keep observation if source unit and target unit are mutually convertible
            if (unit != null && !unit.trim().isEmpty()) {
                String targetUnit = unit.trim();
                String sourceUnit = p.getSourceUnit() != null ? p.getSourceUnit() : p.getUnit();
                boolean targetSupported = conversionService.isSupportedUnit(targetUnit);
                boolean sourceSupported = conversionService.isSupportedUnit(sourceUnit);
                if (!targetSupported || !sourceSupported) {
                    if (!sourceUnit.equalsIgnoreCase(targetUnit)) {
                        return false;
                    }
                }
            }
            if (state != null && !state.trim().isEmpty() && p.getMarket().getState() != null && !p.getMarket().getState().equalsIgnoreCase(state.trim())) {
                return false;
            }
            if (district != null && !district.trim().isEmpty() && p.getMarket().getDistrict() != null && !p.getMarket().getDistrict().equalsIgnoreCase(district.trim())) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());

        // Apply conversion
        List<MarketPriceResponse> converted = filtered.stream()
                .map(p -> applyConversion(p, unit))
                .collect(Collectors.toList());

        // Deterministic Sorting: priceDate descending, observedAt descending
        converted.sort((a, b) -> {
            int cmpDate = b.getPriceDate().compareTo(a.getPriceDate());
            if (cmpDate != 0) return cmpDate;
            return b.getObservedAt().compareTo(a.getObservedAt());
        });

        return converted.stream()
                .limit(maxResults)
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public MarketPriceResponse getLatestMarketPrice(String marketId, String cropId) {
        if (marketId == null || marketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Market ID is required.");
        }
        if (cropId == null || cropId.trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }

        List<MarketPriceResponse> fullPrices = fetchAllMarketPrices();

        List<MarketPriceResponse> matching = fullPrices.stream()
                .filter(p -> p.getMarket().getId().equalsIgnoreCase(marketId.trim())
                        && p.getCrop().getId().equalsIgnoreCase(cropId.trim())
                        && !QUALITY_REJECTED.equalsIgnoreCase(p.getQualityStatus()))
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            throw new NoSuchElementException("No verified or unverified market price record found for marketId: " + marketId + " and cropId: " + cropId);
        }

        // Deterministic sort: priceDate desc, observedAt desc
        matching.sort((a, b) -> {
            int cmpDate = b.getPriceDate().compareTo(a.getPriceDate());
            if (cmpDate != 0) return cmpDate;
            return b.getObservedAt().compareTo(a.getObservedAt());
        });

        return matching.get(0);
    }

    public List<MarketPriceResponse> getMarketPriceHistory(
            String marketId,
            String cropId,
            String fromDate,
            String toDate
    ) {
        if (marketId == null || marketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Market ID is required.");
        }
        if (cropId == null || cropId.trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }

        List<MarketPriceResponse> fullPrices = fetchAllMarketPrices();

        List<MarketPriceResponse> matching = fullPrices.stream()
                .filter(p -> p.getMarket().getId().equalsIgnoreCase(marketId.trim())
                        && p.getCrop().getId().equalsIgnoreCase(cropId.trim())
                        && !QUALITY_REJECTED.equalsIgnoreCase(p.getQualityStatus()))
                .filter(p -> {
                    if (fromDate != null && !fromDate.trim().isEmpty() && p.getPriceDate().compareTo(fromDate.trim()) < 0) {
                        return false;
                    }
                    if (toDate != null && !toDate.trim().isEmpty() && p.getPriceDate().compareTo(toDate.trim()) > 0) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Deterministic Sort: newest to oldest (priceDate desc, observedAt desc)
        matching.sort((a, b) -> {
            int cmpDate = b.getPriceDate().compareTo(a.getPriceDate());
            if (cmpDate != 0) return cmpDate;
            return b.getObservedAt().compareTo(a.getObservedAt());
        });

        return matching;
    }

    public MarketPriceResponse getMarketPriceById(String priceId) {
        if (priceId == null || priceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Price ID cannot be empty.");
        }

        if (firestore != null) {
            try {
                DocumentSnapshot doc = firestore.collection("marketPrices").document(priceId).get().get();
                if (doc.exists()) {
                    return mapDocToMarketPriceResponse(doc);
                }
            } catch (Exception e) {
                logger.warn("Error fetching marketPrice ID {} from Firestore: {}", priceId, e.getMessage());
            }
        }

        for (MarketPriceResponse p : DEFAULT_PRICES) {
            if (p.getId().equalsIgnoreCase(priceId.trim())) {
                return p;
            }
        }

        throw new NoSuchElementException("Market price record not found with ID: " + priceId);
    }

    public MarketPriceResponse applyConversion(MarketPriceResponse p, String targetUnit) {
        if (targetUnit == null || targetUnit.trim().isEmpty()) {
            return p;
        }
        String srcUnit = p.getSourceUnit() != null ? p.getSourceUnit() : p.getUnit();
        PriceUnitConversionService.ConvertedPriceResult result = conversionService.convert(
                p.getMinPrice(), p.getModalPrice(), p.getMaxPrice(),
                srcUnit,
                targetUnit
        );
        return new MarketPriceResponse(
                p.getId(),
                p.getMarket(),
                p.getCrop(),
                p.getPriceDate(),
                p.getObservedAt(),
                result.getMinPrice(),
                result.getMaxPrice(),
                result.getModalPrice(),
                p.getCurrency(),
                result.getDisplayUnit(),
                result.getSourceUnit(),
                result.isConversionApplied(),
                result.getConversionFactor(),
                p.getSource(),
                p.getQualityStatus(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private List<MarketPriceResponse> fetchAllMarketPrices() {
        List<MarketPriceResponse> list = new ArrayList<>();
        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("marketPrices").get().get();
                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        MarketPriceResponse p = mapDocToMarketPriceResponse(doc);
                        if (p != null) {
                            list.add(p);
                        }
                    }
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch marketPrices from Firestore: {}", e.getMessage());
            }
        }
        return DEFAULT_PRICES;
    }

    private MarketPriceResponse mapDocToMarketPriceResponse(DocumentSnapshot doc) {
        String id = doc.getId();
        String marketId = doc.getString("marketId");
        String cropId = doc.getString("cropId");
        String priceDate = doc.getString("priceDate");
        String observedAt = doc.get("observedAt") != null ? doc.get("observedAt").toString() : Instant.now().toString();

        Double minPrice = doc.getDouble("minPrice");
        Double maxPrice = doc.getDouble("maxPrice");
        Double modalPrice = doc.getDouble("modalPrice");

        String currency = doc.getString("currency") != null ? doc.getString("currency") : CURRENCY_INR;
        String unit = doc.getString("unit") != null ? doc.getString("unit") : UNIT_QUINTAL;
        String sourceUnit = doc.getString("sourceUnit") != null ? doc.getString("sourceUnit") : unit;
        String qualityStatus = doc.getString("qualityStatus") != null ? doc.getString("qualityStatus") : QUALITY_UNVERIFIED;
        String status = doc.getString("status") != null ? doc.getString("status") : STATUS_ACTIVE;

        Map<String, Object> srcMap = (Map<String, Object>) doc.get("source");
        MarketPriceSourceDto source = srcMap != null ? new MarketPriceSourceDto(
                (String) srcMap.get("type"),
                (String) srcMap.get("name"),
                (String) srcMap.get("reference")
        ) : new MarketPriceSourceDto("OTHER", "Unknown Source", null);

        String createdAt = doc.get("createdAt") != null ? doc.get("createdAt").toString() : Instant.now().toString();
        String updatedAt = doc.get("updatedAt") != null ? doc.get("updatedAt").toString() : Instant.now().toString();

        // Safe Market mapping (Canonical or Observed)
        MarketSummaryResponse mSummary;
        try {
            MarketResponse m = marketService.getMarketById(marketId);
            mSummary = new MarketSummaryResponse(
                    m.getId(), m.getName(), m.getCode(), m.getType(),
                    m.getLocation().getState(), m.getLocation().getDistrict(), m.getLocation().getMandal(),
                    m.getStatus(), 0
            );
        } catch (Exception e) {
            String obsState = doc.getString("observedState") != null ? doc.getString("observedState") : "Unknown State";
            String obsDistrict = doc.getString("observedDistrict") != null ? doc.getString("observedDistrict") : "Unknown District";
            String obsMarketName = doc.getString("observedMarketName") != null ? doc.getString("observedMarketName") : marketId;
            mSummary = new MarketSummaryResponse(
                    marketId, obsMarketName, "OBS-" + Math.abs(marketId.hashCode()), "OBSERVED_MANDI",
                    obsState, obsDistrict, null, STATUS_ACTIVE, 0
            );
        }

        // Safe Crop mapping (Canonical or Observed)
        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(cropId);
        } catch (Exception e) {
            String obsCommodity = doc.getString("observedCommodityName") != null ? doc.getString("observedCommodityName") : cropId;
            crop = new CropResponse(cropId, obsCommodity, "AGRICULTURAL_COMMODITY", null, STATUS_ACTIVE);
        }

        return new MarketPriceResponse(
                id, mSummary, crop, priceDate, observedAt,
                minPrice != null ? minPrice : 0.0,
                maxPrice != null ? maxPrice : 0.0,
                modalPrice != null ? modalPrice : 0.0,
                currency, unit, sourceUnit, false, 1.0, source, qualityStatus, status,
                createdAt, updatedAt
        );
    }


    private MarketPriceSummaryResponse mapToSummary(MarketPriceResponse p) {
        return new MarketPriceSummaryResponse(
                p.getId(),
                p.getMarket().getId(),
                p.getMarket().getName(),
                p.getCrop().getId(),
                p.getCrop().getName(),
                p.getPriceDate(),
                p.getMinPrice(),
                p.getMaxPrice(),
                p.getModalPrice(),
                p.getCurrency(),
                p.getUnit(),
                p.getSourceUnit(),
                p.isConversionApplied(),
                p.getConversionFactor(),
                p.getSource() != null ? p.getSource().getType() : "OTHER",
                p.getSource() != null ? p.getSource().getName() : "Unknown",
                p.getQualityStatus(),
                p.getStatus()
        );
    }
}
