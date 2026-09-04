package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MarketTrendService {

    private static final Logger logger = LoggerFactory.getLogger(MarketTrendService.class);

    private final MarketPriceService marketPriceService;
    private final MarketService marketService;
    private final CropMasterService cropMasterService;
    private final PriceUnitConversionService conversionService;

    @Value("${trend.rising-threshold-percent:2.0}")
    private double risingThresholdPercent = 2.0;

    @Value("${trend.falling-threshold-percent:2.0}")
    private double fallingThresholdPercent = 2.0;

    public MarketTrendService(
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

    public MarketTrendResponse calculateTrend(
            String cropId,
            String marketId,
            String period,
            String startDate,
            String endDate,
            String unit
    ) {
        String targetCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : null;
        String targetMarketId = (marketId != null && !marketId.trim().isEmpty()) ? marketId.trim() : null;
        String targetUnit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : MarketPriceService.UNIT_QUINTAL;

        // 1. Resolve & Validate Date Bounds
        ResolvedDateRange dateRange = resolveAndValidateDateRange(period, startDate, endDate);

        // 2. Fetch Crop and Market Names for Response
        String cropName = resolveCropName(targetCropId);
        String marketName = resolveMarketName(targetMarketId);

        // 3. Fetch Bounded Verified Observations via MarketPriceService
        List<MarketPriceSummaryResponse> summaries = marketPriceService.getMarketPrices(
                targetMarketId,
                targetCropId,
                null, // priceDate
                dateRange.getStartDate(),
                dateRange.getEndDate(),
                MarketPriceService.QUALITY_VERIFIED,
                targetUnit,
                null, // state
                null, // district
                100   // limit
        );

        if (summaries == null) {
            summaries = Collections.emptyList();
        }

        // 4. Normalize unit & filter invalid price observations
        List<MarketPriceSummaryResponse> valid = new ArrayList<>();
        for (MarketPriceSummaryResponse s : summaries) {
            if (s == null || s.getPriceDate() == null || s.getPriceDate().trim().isEmpty()) {
                continue;
            }
            if (s.getCurrency() != null && !s.getCurrency().equalsIgnoreCase(MarketPriceService.CURRENCY_INR)) {
                continue;
            }
            if (!MarketPriceService.validatePriceRecord(s.getMinPrice(), s.getMaxPrice(), s.getModalPrice())) {
                logger.warn("Skipping invalid price observation: min={}, modal={}, max={}", s.getMinPrice(), s.getModalPrice(), s.getMaxPrice());
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
            valid.add(s);
        }

        // 5. Chronological Sort (Oldest to Newest)
        valid.sort(Comparator.comparing(MarketPriceSummaryResponse::getPriceDate));

        // 6. Build Trend Response Objects
        MarketTrendResponse response = new MarketTrendResponse();
        response.setCropId(targetCropId);
        response.setCropName(cropName);
        response.setMarketId(targetMarketId);
        response.setMarketName(marketName);
        response.setCurrency(MarketPriceService.CURRENCY_INR);
        response.setUnit(targetUnit);
        response.setPeriod(dateRange.getPeriodCode());
        response.setStartDate(dateRange.getStartDate());
        response.setEndDate(dateRange.getEndDate());

        int count = valid.size();
        response.setObservationCount(count);

        List<PriceTrendPointDto> points = new ArrayList<>();
        for (MarketPriceSummaryResponse s : valid) {
            points.add(new PriceTrendPointDto(s.getPriceDate(), s.getModalPrice(), s.getMinPrice(), s.getMaxPrice()));
        }
        response.setPoints(points);

        if (count == 0) {
            response.setTrendDirection("INSUFFICIENT_DATA");
            response.setVolatility("INSUFFICIENT_DATA");
            response.setDataQuality("INSUFFICIENT");
            response.setFreshnessStatus("UNAVAILABLE");
            response.setCurrentVsAverageStatement("No verified market price observations found for the selected period.");
            return response;
        }

        // 7. Statistical Calculations using BigDecimal
        double earliestModal = valid.get(0).getModalPrice();
        double latestModal = valid.get(count - 1).getModalPrice();
        double minModal = valid.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).min().orElse(0.0);
        double maxModal = valid.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).max().orElse(0.0);
        double sumModal = valid.stream().mapToDouble(MarketPriceSummaryResponse::getModalPrice).sum();
        double avgModal = round(sumModal / count);

        double absChange = round(latestModal - earliestModal);
        Double pctChange = null;
        if (earliestModal > 0.0) {
            pctChange = round(((latestModal - earliestModal) / earliestModal) * 100.0);
        }

        double priceRange = round(maxModal - minModal);

        response.setEarliestPrice(round(earliestModal));
        response.setLatestPrice(round(latestModal));
        response.setMinPrice(round(minModal));
        response.setMaxPrice(round(maxModal));
        response.setAvgPrice(avgModal);
        response.setPriceRange(priceRange);
        response.setAbsoluteChange(absChange);
        response.setPercentageChange(pctChange);

        // 8. Classify Trend Direction
        if (count < 2) {
            response.setTrendDirection("INSUFFICIENT_DATA");
        } else if (pctChange != null) {
            if (pctChange > risingThresholdPercent) {
                response.setTrendDirection("RISING");
            } else if (pctChange < -fallingThresholdPercent) {
                response.setTrendDirection("FALLING");
            } else {
                response.setTrendDirection("STABLE");
            }
        } else {
            response.setTrendDirection("STABLE");
        }

        // 9. Classify Volatility
        VolatilityResult volResult = calculateVolatility(valid.stream().map(MarketPriceSummaryResponse::getModalPrice).toList());
        response.setVolatility(volResult.getCategory());
        response.setVolatilityCvPercent(volResult.getCvPercent());

        // 10. Classify Data Quality
        if (count < 2) {
            response.setDataQuality("INSUFFICIENT");
        } else if (count < 5) {
            response.setDataQuality("LIMITED");
        } else {
            response.setDataQuality("GOOD");
        }

        // 11. Classify Freshness Status
        String latestObsDate = valid.get(count - 1).getPriceDate();
        response.setLatestObservationDate(latestObsDate);
        boolean isFresh = isDateFresh(latestObsDate, 7);
        response.setFreshnessStatus(isFresh ? "FRESH" : "STALE");

        // 12. Current vs Average Comparison
        double diffFromAvg = round(latestModal - avgModal);
        Double pctDiffFromAvg = (avgModal > 0.0) ? round(((latestModal - avgModal) / avgModal) * 100.0) : null;
        response.setCurrentVsAveragePctDiff(pctDiffFromAvg);

        String statement;
        if (pctDiffFromAvg != null) {
            if (Math.abs(pctDiffFromAvg) < 0.1) {
                statement = String.format("The latest verified price (₹%,.2f / %s) is equal to the selected period average (₹%,.2f / %s).",
                        latestModal, targetUnit, avgModal, targetUnit);
            } else if (pctDiffFromAvg > 0) {
                statement = String.format("The latest verified price (₹%,.2f / %s) is approximately %.2f%% (+₹%,.2f) above the selected period average (₹%,.2f / %s).",
                        latestModal, targetUnit, pctDiffFromAvg, diffFromAvg, avgModal, targetUnit);
            } else {
                statement = String.format("The latest verified price (₹%,.2f / %s) is approximately %.2f%% (₹%,.2f) below the selected period average (₹%,.2f / %s).",
                        latestModal, targetUnit, Math.abs(pctDiffFromAvg), diffFromAvg, avgModal, targetUnit);
            }
        } else {
            statement = String.format("The latest verified price is ₹%,.2f / %s.", latestModal, targetUnit);
        }
        response.setCurrentVsAverageStatement(statement);

        return response;
    }

    public VolatilityResult calculateVolatility(List<Double> prices) {
        if (prices == null || prices.size() < 3) {
            return new VolatilityResult("INSUFFICIENT_DATA", null);
        }

        int n = prices.size();
        double sum = 0.0;
        for (double p : prices) {
            sum += p;
        }
        double mean = sum / n;

        if (mean <= 0.0) {
            return new VolatilityResult("LOW", 0.0);
        }

        double sumSqDiff = 0.0;
        for (double p : prices) {
            double diff = p - mean;
            sumSqDiff += diff * diff;
        }
        double variance = sumSqDiff / n;
        double stdDev = Math.sqrt(variance);
        double cv = round((stdDev / mean) * 100.0);

        String category;
        if (cv < 5.0) {
            category = "LOW";
        } else if (cv <= 15.0) {
            category = "MEDIUM";
        } else {
            category = "HIGH";
        }

        return new VolatilityResult(category, cv);
    }

    public ResolvedDateRange resolveAndValidateDateRange(String period, String startDate, String endDate) {
        String pCode = (period != null && !period.trim().isEmpty()) ? period.trim().toUpperCase() : "30D";
        LocalDate today = LocalDate.now();

        if ("CUSTOM".equalsIgnoreCase(pCode)) {
            if (startDate == null || startDate.trim().isEmpty() || endDate == null || endDate.trim().isEmpty()) {
                throw new IllegalArgumentException("Custom period requires both startDate and endDate parameters in YYYY-MM-DD format.");
            }
            LocalDate start;
            LocalDate end;
            try {
                start = LocalDate.parse(startDate.trim());
                end = LocalDate.parse(endDate.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD format.");
            }

            if (start.isAfter(end)) {
                throw new IllegalArgumentException("startDate cannot be after endDate.");
            }
            if (end.isAfter(today)) {
                throw new IllegalArgumentException("endDate cannot be in the future.");
            }
            long daysSpan = ChronoUnit.DAYS.between(start, end);
            if (daysSpan > 365) {
                throw new IllegalArgumentException("Custom date range cannot exceed 365 days.");
            }
            return new ResolvedDateRange("CUSTOM", start.toString(), end.toString());
        }

        int days;
        switch (pCode) {
            case "7D":
                days = 7;
                break;
            case "90D":
                days = 90;
                break;
            case "6M":
                days = 180;
                break;
            case "1Y":
                days = 365;
                break;
            case "30D":
            default:
                days = 30;
                pCode = "30D";
                break;
        }

        LocalDate start = today.minusDays(days);
        return new ResolvedDateRange(pCode, start.toString(), today.toString());
    }

    private String resolveCropName(String cropId) {
        if (cropId == null) return "All Crops";
        try {
            CropResponse c = cropMasterService.getCropById(cropId);
            if (c != null && c.getName() != null) return c.getName();
        } catch (Exception ignored) {
        }
        return cropId;
    }

    private String resolveMarketName(String marketId) {
        if (marketId == null) return "All Markets";
        try {
            MarketResponse m = marketService.getMarketById(marketId);
            if (m != null && m.getName() != null) return m.getName();
        } catch (Exception ignored) {
        }
        return marketId;
    }

    private boolean isDateFresh(String dateStr, int maxDays) {
        if (dateStr == null || dateStr.trim().isEmpty()) return false;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim());
            long daysOld = ChronoUnit.DAYS.between(d, LocalDate.now());
            return daysOld >= 0 && daysOld <= maxDays;
        } catch (Exception e) {
            return false;
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static class VolatilityResult {
        private final String category;
        private final Double cvPercent;

        public VolatilityResult(String category, Double cvPercent) {
            this.category = category;
            this.cvPercent = cvPercent;
        }

        public String getCategory() {
            return category;
        }

        public Double getCvPercent() {
            return cvPercent;
        }
    }

    public static class ResolvedDateRange {
        private final String periodCode;
        private final String startDate;
        private final String endDate;

        public ResolvedDateRange(String periodCode, String startDate, String endDate) {
            this.periodCode = periodCode;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getPeriodCode() {
            return periodCode;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }
    }
}
