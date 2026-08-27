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
    private final FirestoreQuotaGuard quotaGuard;

    public MarketPriceService(
            Firestore firestore,
            MarketService marketService,
            CropMasterService cropMasterService,
            PriceUnitConversionService conversionService
    ) {
        this(firestore, marketService, cropMasterService, conversionService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MarketPriceService(
            Firestore firestore,
            MarketService marketService,
            CropMasterService cropMasterService,
            PriceUnitConversionService conversionService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.marketService = marketService;
        this.cropMasterService = cropMasterService;
        this.conversionService = conversionService;
        this.quotaGuard = quotaGuard;
    }

    public List<MarketPriceResponse> getMarketPrices(
            String state,
            String district,
            String marketId,
            String cropId,
            String priceDate,
            String qualityStatus,
            String unit,
            String sortBy,
            String sortDirection,
            Integer page,
            Integer pageSize,
            Integer limit
    ) {
        List<MarketPriceResponse> fullPrices = fetchTargetedMarketPrices(marketId, cropId, priceDate, state, district, limit);

        List<MarketPriceResponse> filtered = fullPrices.stream().filter(p -> {
            if (p == null) return false;
            MarketSummaryResponse mkt = p.getMarket();
            CropResponse crp = p.getCrop();

            if (state != null && !state.trim().isEmpty()) {
                if (mkt == null || mkt.getState() == null || !mkt.getState().equalsIgnoreCase(state.trim())) {
                    return false;
                }
            }
            if (district != null && !district.trim().isEmpty()) {
                if (mkt == null || mkt.getDistrict() == null || !mkt.getDistrict().equalsIgnoreCase(district.trim())) {
                    return false;
                }
            }
            if (marketId != null && !marketId.trim().isEmpty()) {
                if (mkt == null || mkt.getId() == null || !mkt.getId().equalsIgnoreCase(marketId.trim())) {
                    return false;
                }
            }
            if (cropId != null && !cropId.trim().isEmpty()) {
                if (crp == null || crp.getId() == null || !crp.getId().equalsIgnoreCase(cropId.trim())) {
                    return false;
                }
            }
            if (priceDate != null && !priceDate.trim().isEmpty()) {
                if (p.getPriceDate() == null || !p.getPriceDate().equalsIgnoreCase(priceDate.trim())) {
                    return false;
                }
            }
            if (qualityStatus != null && !qualityStatus.trim().isEmpty()) {
                if (p.getQualityStatus() == null || !p.getQualityStatus().equalsIgnoreCase(qualityStatus.trim())) {
                    return false;
                }
            } else {
                if (QUALITY_REJECTED.equalsIgnoreCase(p.getQualityStatus())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        // Apply conversion
        List<MarketPriceResponse> converted = filtered.stream()
                .map(p -> applyConversion(p, unit))
                .collect(Collectors.toList());

        // Deterministic Sorting: priceDate descending, observedAt descending
        boolean asc = "asc".equalsIgnoreCase(sortDirection);
        Comparator<MarketPriceResponse> comp = (a, b) -> {
            int cmpDate = Objects.toString(b.getPriceDate(), "").compareTo(Objects.toString(a.getPriceDate(), ""));
            if (cmpDate != 0) return cmpDate;
            return Objects.toString(b.getObservedAt(), "").compareTo(Objects.toString(a.getObservedAt(), ""));
        };
        if (asc) {
            comp = comp.reversed();
        }
        converted.sort(comp);

        int pSize = (pageSize != null && pageSize > 0 && pageSize <= 100) ? pageSize : (limit != null && limit > 0 ? limit : 50);
        int pNum = (page != null && page >= 1) ? page : 1;
        int fromIdx = (pNum - 1) * pSize;
        if (fromIdx >= converted.size()) {
            return Collections.emptyList();
        }

        int toIdx = Math.min(fromIdx + pSize, converted.size());
        return converted.subList(fromIdx, toIdx);
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
        List<MarketPriceResponse> responses = getMarketPrices(
                state, district, marketId, cropId, priceDate, qualityStatus, unit, "priceDate", "desc", 1, limit, limit
        );

        List<MarketPriceSummaryResponse> summaries = new ArrayList<>();
        for (MarketPriceResponse p : responses) {
            if (p == null) continue;
            if (fromDate != null && !fromDate.trim().isEmpty() && p.getPriceDate().compareTo(fromDate.trim()) < 0) {
                continue;
            }
            if (toDate != null && !toDate.trim().isEmpty() && p.getPriceDate().compareTo(toDate.trim()) > 0) {
                continue;
            }
            summaries.add(mapToSummary(p));
        }
        return summaries;
    }

    public MarketPriceResponse getMarketPriceById(String priceId) {
        if (priceId == null || priceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Price ID cannot be empty.");
        }

        if (firestore != null) {
            try {
                DocumentSnapshot doc = firestore.collection("marketPrices").document(priceId).get().get();
                if (doc != null && doc.exists()) {
                    MarketPriceResponse response = mapDocToMarketPriceResponse(doc);
                    if (response != null) {
                        return response;
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching marketPrice ID {} from Firestore: {}", priceId, e.getMessage());
                throw new RuntimeException("Could not fetch market price from Firestore: " + e.getMessage(), e);
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

    private List<MarketPriceResponse> fetchTargetedMarketPrices(
            String marketId,
            String cropId,
            String priceDate,
            String state,
            String district,
            Integer limit
    ) {
        if (quotaGuard != null) {
            quotaGuard.checkQuotaAvailability();
        }

        if (firestore == null) {
            return Collections.emptyList();
        }

        List<MarketPriceResponse> list = new ArrayList<>();
        try {
            var colRef = firestore.collection("marketPrices");
            if (colRef == null) {
                return Collections.emptyList();
            }

            int fetchLimit = (limit != null && limit > 0 && limit <= 100) ? Math.min(limit * 4, 300) : 100;
            Query query = colRef;

            if (marketId != null && !marketId.trim().isEmpty()) {
                query = query.whereEqualTo("marketId", marketId.trim());
            }
            if (cropId != null && !cropId.trim().isEmpty()) {
                query = query.whereEqualTo("cropId", cropId.trim());
            }
            if (priceDate != null && !priceDate.trim().isEmpty()) {
                query = query.whereEqualTo("priceDate", priceDate.trim());
            }
            if (state != null && !state.trim().isEmpty()) {
                query = query.whereEqualTo("observedState", state.trim());
            }
            if (district != null && !district.trim().isEmpty()) {
                query = query.whereEqualTo("observedDistrict", district.trim());
            }

            query = query.limit(fetchLimit);

            var future = query.get();
            if (future != null) {
                QuerySnapshot snapshot = future.get();
                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        MarketPriceResponse p = mapDocToMarketPriceResponse(doc);
                        if (p != null) {
                            list.add(p);
                        }
                    }
                }
            }
            if (quotaGuard != null) {
                quotaGuard.recordSuccess();
            }
            return list;
        } catch (Exception e) {
            if (quotaGuard != null && isQuotaExhaustedError(e)) {
                quotaGuard.recordQuotaExhaustion(e);
            }
            logger.error("Could not query marketPrices from Firestore: {}", e.getMessage());
            throw new RuntimeException("Could not query marketPrices from Firestore: " + e.getMessage(), e);
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

    private List<MarketPriceResponse> fetchAllMarketPrices() {
        return fetchTargetedMarketPrices(null, null, null, null, null, 50);
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
