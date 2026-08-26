package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketIntelligenceService {

    private static final Logger logger = LoggerFactory.getLogger(MarketIntelligenceService.class);

    private final MarketPriceService marketPriceService;
    private final MarketService marketService;
    private final CropMasterService cropMasterService;

    public MarketIntelligenceService(
            MarketPriceService marketPriceService,
            MarketService marketService,
            CropMasterService cropMasterService
    ) {
        this.marketPriceService = marketPriceService;
        this.marketService = marketService;
        this.cropMasterService = cropMasterService;
    }

    public MarketComparisonResponse compareMarkets(
            String cropId, String date, String fromDate, String toDate,
            String state, String district, String unit
    ) {
        String targetCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : "crop-chilli";
        String targetUnit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : MarketPriceService.UNIT_QUINTAL;
        String qualityStatus = MarketPriceService.QUALITY_VERIFIED;

        CropResponse crop = cropMasterService.getCropById(targetCropId);

        // Fetch verified observations
        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                state, district, null, targetCropId, date, fromDate, toDate,
                qualityStatus, MarketPriceService.STATUS_ACTIVE, 100
        );

        // Ensure strict Unit and Currency Consistency
        List<MarketPriceSummaryResponse> compatible = summaries.stream()
                .filter(s -> s.getUnit() != null && s.getUnit().equalsIgnoreCase(targetUnit))
                .filter(s -> s.getCurrency() != null && s.getCurrency().equalsIgnoreCase(MarketPriceService.CURRENCY_INR))
                .toList();

        // Group by MarketId to keep latest observation per market
        Map<String, MarketPriceSummaryResponse> latestPerMarket = new LinkedHashMap<>();
        for (MarketPriceSummaryResponse s : compatible) {
            if (!latestPerMarket.containsKey(s.getMarketId())) {
                latestPerMarket.put(s.getMarketId(), s);
            }
        }

        List<MarketComparisonItemDto> items = new ArrayList<>();
        for (MarketPriceSummaryResponse s : latestPerMarket.values()) {
            MarketSummaryResponse m = findMarket(s.getMarketId());
            items.add(new MarketComparisonItemDto(
                    s.getMarketId(),
                    s.getMarketName() != null ? s.getMarketName() : "Market " + s.getMarketId(),
                    m != null ? m.getState() : "",
                    m != null ? m.getDistrict() : "",
                    m != null ? m.getMandal() : "",
                    s.getMinPrice(),
                    s.getMaxPrice(),
                    s.getModalPrice(),
                    s.getCurrency(),
                    s.getUnit(),
                    s.getPriceDate(),
                    s.getSourceName(),
                    s.getQualityStatus()
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
                (!items.isEmpty() ? items.get(0).getPriceDate() : "");

        return new MarketComparisonResponse(
                crop,
                effectiveDate,
                MarketPriceService.CURRENCY_INR,
                targetUnit,
                items,
                highest,
                lowest,
                priceDiff,
                pctDiff
        );
    }

    public MarketIntelligenceSummaryResponse getSummary(
            String cropId, String marketId, String fromDate, String toDate,
            String state, String district, String unit
    ) {
        String targetUnit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : MarketPriceService.UNIT_QUINTAL;
        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                state, district, marketId, cropId, null, fromDate, toDate,
                MarketPriceService.QUALITY_VERIFIED, MarketPriceService.STATUS_ACTIVE, 200
        );

        List<MarketPriceSummaryResponse> compatible = summaries.stream()
                .filter(s -> s.getUnit() != null && s.getUnit().equalsIgnoreCase(targetUnit))
                .sorted(Comparator.comparing(MarketPriceSummaryResponse::getPriceDate))
                .toList();

        int count = compatible.size();
        if (count == 0) {
            return new MarketIntelligenceSummaryResponse(
                    0, 0.0, 0.0, 0.0, 0.0,
                    "", "", 0.0, null, "INSUFFICIENT_DATA",
                    MarketPriceService.CURRENCY_INR, targetUnit
            );
        }

        double latestModal = compatible.get(count - 1).getModalPrice();
        double firstModal = compatible.get(0).getModalPrice();
        double minModal = compatible.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).min().orElse(0.0);
        double maxModal = compatible.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).max().orElse(0.0);
        double sumModal = compatible.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).sum();
        double avgModal = sumModal / count;

        String firstDate = compatible.get(0).getPriceDate();
        String latestDate = compatible.get(count - 1).getPriceDate();

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

        CropResponse crop = cropMasterService.getCropById(targetCropId);
        MarketResponse market = (marketId != null && !marketId.trim().isEmpty()) ? marketService.getMarketById(marketId) : null;

        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                null, null, marketId, targetCropId, null, fromDate, toDate,
                MarketPriceService.QUALITY_VERIFIED, MarketPriceService.STATUS_ACTIVE, 100
        );

        List<MarketPriceSummaryResponse> compatible = summaries.stream()
                .filter(s -> s.getUnit() != null && s.getUnit().equalsIgnoreCase(targetUnit))
                .sorted(Comparator.comparing(MarketPriceSummaryResponse::getPriceDate))
                .toList();

        List<PriceTrendPointDto> points = new ArrayList<>();
        for (MarketPriceSummaryResponse s : compatible) {
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
