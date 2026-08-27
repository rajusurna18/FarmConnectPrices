package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketIntelligenceService {

    private static final Logger logger = LoggerFactory.getLogger(MarketIntelligenceService.class);

    private final MarketPriceService marketPriceService;
    private final MarketService marketService;
    private final CropMasterService cropMasterService;
    private final PriceUnitConversionService conversionService;

    public MarketIntelligenceService(
            MarketPriceService marketPriceService,
            MarketService marketService,
            CropMasterService cropMasterService,
            PriceUnitConversionService conversionService
    ) {
        this.marketPriceService = marketPriceService;
        this.marketService = marketService;
        this.cropMasterService = cropMasterService;
        this.conversionService = conversionService;
    }

    public MarketComparisonResponse compareMarkets(
            String cropId, String date, String fromDate, String toDate,
            String state, String district, String unit
    ) {
        String targetCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : "crop-chilli";
        String targetUnit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : MarketPriceService.UNIT_QUINTAL;
        String qualityStatus = MarketPriceService.QUALITY_VERIFIED;

        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(targetCropId);
        } catch (Exception e) {
            logger.warn("Crop not found for comparison: {}. Returning empty comparison.", targetCropId);
            crop = new CropResponse(targetCropId, targetCropId, "GENERAL", "", "ACTIVE");
        }

        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                null,           // marketId
                targetCropId,   // cropId
                date,           // priceDate
                fromDate,       // fromDate
                toDate,         // toDate
                qualityStatus,  // qualityStatus
                targetUnit,     // unit
                state,          // state
                district,       // district
                100             // limit
        );

        if (summaries == null) {
            summaries = Collections.emptyList();
        }

        // Normalize each summary to targetUnit using conversionService
        List<MarketPriceSummaryResponse> normalized = new ArrayList<>();
        for (MarketPriceSummaryResponse s : summaries) {
            if (s.getCurrency() != null && !s.getCurrency().equalsIgnoreCase(MarketPriceService.CURRENCY_INR)) {
                continue;
            }
            String srcUnit = s.getSourceUnit() != null ? s.getSourceUnit() : s.getUnit();
            PriceUnitConversionService.ConvertedPriceResult conv = conversionService.convert(
                    s.getMinPrice(), s.getModalPrice(), s.getMaxPrice(), srcUnit, targetUnit
            );
            if (conversionService.isSupportedUnit(targetUnit) && !conversionService.isSupportedUnit(srcUnit) && !srcUnit.equalsIgnoreCase(targetUnit)) {
                continue;
            }

            s.setMinPrice(conv.getMinPrice());
            s.setModalPrice(conv.getModalPrice());
            s.setMaxPrice(conv.getMaxPrice());
            s.setUnit(conv.getDisplayUnit());
            s.setSourceUnit(conv.getSourceUnit());
            s.setConversionApplied(conv.isConversionApplied());
            s.setConversionFactor(conv.getConversionFactor());
            normalized.add(s);
        }

        // Group by MarketId to keep latest observation per market
        Map<String, MarketPriceSummaryResponse> latestPerMarket = new LinkedHashMap<>();
        for (MarketPriceSummaryResponse s : normalized) {
            if (s.getMarketId() != null && !latestPerMarket.containsKey(s.getMarketId())) {
                latestPerMarket.put(s.getMarketId(), s);
            }
        }

        List<MarketComparisonItemDto> items = new ArrayList<>();
        for (MarketPriceSummaryResponse s : latestPerMarket.values()) {
            MarketSummaryResponse m = findMarket(s.getMarketId());
            items.add(new MarketComparisonItemDto(
                    s.getMarketId(),
                    s.getMarketName() != null ? s.getMarketName() : "Market " + s.getMarketId(),
                    m != null ? m.getState() : (state != null ? state : ""),
                    m != null ? m.getDistrict() : (district != null ? district : ""),
                    m != null ? m.getMandal() : "",
                    s.getMinPrice(),
                    s.getMaxPrice(),
                    s.getModalPrice(),
                    s.getCurrency(),
                    s.getUnit(),
                    s.getSourceUnit(),
                    s.isConversionApplied(),
                    s.getConversionFactor(),
                    s.getPriceDate() != null ? s.getPriceDate() : "",
                    s.getSourceName() != null ? s.getSourceName() : "AGMARKNET",
                    s.getQualityStatus() != null ? s.getQualityStatus() : MarketPriceService.QUALITY_VERIFIED
            ));
        }

        // Sort items by modalPrice descending for market ranking
        items.sort(Comparator.comparingDouble(MarketComparisonItemDto::getModalPrice).reversed());

        MarketComparisonItemDto highest = !items.isEmpty() ? items.get(0) : null;
        MarketComparisonItemDto lowest = !items.isEmpty() ? items.get(items.size() - 1) : null;

        double priceDiff = 0.0;
        Double pctDiff = null;

        if (highest != null && lowest != null) {
            priceDiff = highest.getModalPrice() - lowest.getModalPrice();
            if (lowest.getModalPrice() > 0.0) {
                pctDiff = ((highest.getModalPrice() - lowest.getModalPrice()) / lowest.getModalPrice()) * 100.0;
            }
        }

        String effectiveDate = (date != null && !date.trim().isEmpty()) ? date.trim() :
                (!items.isEmpty() && items.get(0).getPriceDate() != null ? items.get(0).getPriceDate() : "");

        String cropName = crop != null ? crop.getName() : "this crop";
        String observationSummary;
        if (items.size() > 1 && highest != null && lowest != null) {
            if (pctDiff != null) {
                observationSummary = String.format("%s currently has the highest observed price for %s among the selected markets at ₹%,.0f / %s, which is ₹%,.0f (+%.1f%%) higher than %s.",
                        highest.getMarketName(), cropName, highest.getModalPrice(), targetUnit, priceDiff, pctDiff, lowest.getMarketName());
            } else {
                observationSummary = String.format("%s currently has the highest observed price for %s among the selected markets at ₹%,.0f / %s.",
                        highest.getMarketName(), cropName, highest.getModalPrice(), targetUnit);
            }
        } else if (items.size() == 1 && highest != null) {
            observationSummary = String.format("%s is currently the only market with a verified price observation for %s at ₹%,.0f / %s.",
                    highest.getMarketName(), cropName, highest.getModalPrice(), targetUnit);
        } else {
            observationSummary = "No verified market price observations found matching the selected criteria.";
        }

        return new MarketComparisonResponse(
                crop,
                effectiveDate,
                MarketPriceService.CURRENCY_INR,
                targetUnit,
                items,
                highest,
                lowest,
                priceDiff,
                pctDiff,
                "COMMODITY_LEVEL",
                observationSummary
        );
    }

    public MarketIntelligenceSummaryResponse getSummary(
            String cropId, String marketId, String fromDate, String toDate,
            String state, String district, String unit
    ) {
        String targetUnit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : MarketPriceService.UNIT_QUINTAL;
        
        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                marketId,
                cropId,
                null,
                fromDate,
                toDate,
                MarketPriceService.QUALITY_VERIFIED,
                targetUnit,
                state,
                district,
                200
        );

        if (summaries == null) {
            summaries = Collections.emptyList();
        }

        List<MarketPriceSummaryResponse> normalized = new ArrayList<>();
        for (MarketPriceSummaryResponse s : summaries) {
            if (s.getPriceDate() == null) continue;
            if (s.getCurrency() != null && !s.getCurrency().equalsIgnoreCase(MarketPriceService.CURRENCY_INR)) continue;

            String srcUnit = s.getSourceUnit() != null ? s.getSourceUnit() : s.getUnit();
            PriceUnitConversionService.ConvertedPriceResult conv = conversionService.convert(
                    s.getMinPrice(), s.getModalPrice(), s.getMaxPrice(), srcUnit, targetUnit
            );
            if (conversionService.isSupportedUnit(targetUnit) && !conversionService.isSupportedUnit(srcUnit) && !srcUnit.equalsIgnoreCase(targetUnit)) {
                continue;
            }

            s.setMinPrice(conv.getMinPrice());
            s.setModalPrice(conv.getModalPrice());
            s.setMaxPrice(conv.getMaxPrice());
            s.setUnit(conv.getDisplayUnit());
            s.setSourceUnit(conv.getSourceUnit());
            s.setConversionApplied(conv.isConversionApplied());
            s.setConversionFactor(conv.getConversionFactor());
            normalized.add(s);
        }

        normalized.sort(Comparator.comparing(MarketPriceSummaryResponse::getPriceDate));

        int count = normalized.size();
        if (count == 0) {
            return new MarketIntelligenceSummaryResponse(
                    0, 0.0, 0.0, 0.0, 0.0,
                    "", "", 0.0, null, "INSUFFICIENT_DATA",
                    MarketPriceService.CURRENCY_INR, targetUnit
            );
        }

        double latestModal = normalized.get(count - 1).getModalPrice();
        double firstModal = normalized.get(0).getModalPrice();
        double minModal = normalized.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).min().orElse(0.0);
        double maxModal = normalized.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).max().orElse(0.0);
        double sumModal = normalized.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).sum();
        double avgModal = sumModal / count;

        String firstDate = normalized.get(0).getPriceDate();
        String latestDate = normalized.get(count - 1).getPriceDate();

        double absChange = latestModal - firstModal;
        Double pctChange = (firstModal > 0.0) ? ((latestModal - firstModal) / firstModal) * 100.0 : null;

        String trend = "STABLE";
        if (count < 2) {
            trend = "INSUFFICIENT_DATA";
        } else if (latestModal > firstModal) {
            trend = "INCREASING";
        } else if (latestModal < firstModal) {
            trend = "DECREASING";
        }

        return new MarketIntelligenceSummaryResponse(
                count, latestModal, minModal, maxModal, avgModal,
                firstDate, latestDate, absChange, pctChange, trend,
                MarketPriceService.CURRENCY_INR, targetUnit
        );
    }

    public PriceTrendResponse getTrends(
            String cropId, String marketId, String fromDate, String toDate, String unit
    ) {
        String targetCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : "crop-chilli";
        String targetUnit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : MarketPriceService.UNIT_QUINTAL;

        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(targetCropId);
        } catch (Exception e) {
            crop = new CropResponse(targetCropId, targetCropId, "GENERAL", "", "ACTIVE");
        }

        MarketResponse market = null;
        if (marketId != null && !marketId.trim().isEmpty()) {
            try {
                market = marketService.getMarketById(marketId);
            } catch (Exception ignored) {
            }
        }

        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                marketId,
                targetCropId,
                null,
                fromDate,
                toDate,
                MarketPriceService.QUALITY_VERIFIED,
                targetUnit,
                null,
                null,
                100
        );

        if (summaries == null) {
            summaries = Collections.emptyList();
        }

        List<MarketPriceSummaryResponse> normalized = new ArrayList<>();
        for (MarketPriceSummaryResponse s : summaries) {
            if (s.getPriceDate() == null) continue;
            if (s.getCurrency() != null && !s.getCurrency().equalsIgnoreCase(MarketPriceService.CURRENCY_INR)) continue;

            String srcUnit = s.getSourceUnit() != null ? s.getSourceUnit() : s.getUnit();
            PriceUnitConversionService.ConvertedPriceResult conv = conversionService.convert(
                    s.getMinPrice(), s.getModalPrice(), s.getMaxPrice(), srcUnit, targetUnit
            );
            if (conversionService.isSupportedUnit(targetUnit) && !conversionService.isSupportedUnit(srcUnit) && !srcUnit.equalsIgnoreCase(targetUnit)) {
                continue;
            }

            s.setMinPrice(conv.getMinPrice());
            s.setModalPrice(conv.getModalPrice());
            s.setMaxPrice(conv.getMaxPrice());
            s.setUnit(conv.getDisplayUnit());
            s.setSourceUnit(conv.getSourceUnit());
            s.setConversionApplied(conv.isConversionApplied());
            s.setConversionFactor(conv.getConversionFactor());
            normalized.add(s);
        }

        normalized.sort(Comparator.comparing(MarketPriceSummaryResponse::getPriceDate));

        List<PriceTrendPointDto> points = new ArrayList<>();
        for (MarketPriceSummaryResponse s : normalized) {
            points.add(new PriceTrendPointDto(s.getPriceDate(), s.getModalPrice(), s.getMinPrice(), s.getMaxPrice()));
        }

        int count = points.size();
        String trend = "STABLE";
        double absChange = 0.0;
        Double pctChange = null;

        if (count < 2) {
            trend = "INSUFFICIENT_DATA";
        } else {
            double firstModal = points.get(0).getModalPrice();
            double latestModal = points.get(count - 1).getModalPrice();
            absChange = latestModal - firstModal;
            if (firstModal > 0.0) {
                pctChange = (absChange / firstModal) * 100.0;
            }
            if (latestModal > firstModal) {
                trend = "INCREASING";
            } else if (latestModal < firstModal) {
                trend = "DECREASING";
            }
        }

        return new PriceTrendResponse(
                targetCropId,
                crop != null ? crop.getName() : targetCropId,
                marketId,
                market != null ? market.getName() : "All Markets",
                MarketPriceService.CURRENCY_INR,
                targetUnit,
                points,
                trend,
                absChange,
                pctChange
        );
    }

    private MarketSummaryResponse findMarket(String marketId) {
        if (marketId == null) return null;
        try {
            MarketResponse m = marketService.getMarketById(marketId);
            if (m != null && m.getLocation() != null) {
                return new MarketSummaryResponse(
                        m.getId(), m.getName(), m.getCode(), m.getType(),
                        m.getLocation().getState(), m.getLocation().getDistrict(), m.getLocation().getMandal(),
                        m.getStatus(), 0
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
