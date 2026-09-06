package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ForecastConfidenceService {

    public static class ConfidenceEvaluationResult {
        private ForecastConfidence confidence;
        private ForecastDirection direction;
        private String dataQuality;
        private Double lowerBound;
        private Double upperBound;
        private Double sampleStandardDeviation;
        private long freshnessGapDays;
        private boolean fresh;

        public ForecastConfidence getConfidence() { return confidence; }
        public void setConfidence(ForecastConfidence confidence) { this.confidence = confidence; }

        public ForecastDirection getDirection() { return direction; }
        public void setDirection(ForecastDirection direction) { this.direction = direction; }

        public String getDataQuality() { return dataQuality; }
        public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }

        public Double getLowerBound() { return lowerBound; }
        public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }

        public Double getUpperBound() { return upperBound; }
        public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }

        public Double getSampleStandardDeviation() { return sampleStandardDeviation; }
        public void setSampleStandardDeviation(Double sampleStandardDeviation) { this.sampleStandardDeviation = sampleStandardDeviation; }

        public long getFreshnessGapDays() { return freshnessGapDays; }
        public void setFreshnessGapDays(long freshnessGapDays) { this.freshnessGapDays = freshnessGapDays; }

        public boolean isFresh() { return fresh; }
        public void setFresh(boolean fresh) { this.fresh = fresh; }
    }

    public ConfidenceEvaluationResult evaluateConfidenceAndBounds(
            Double forecastPrice,
            Double currentPrice,
            List<MarketPriceSummaryResponse> observations,
            ForecastHorizon horizon,
            ForecastBacktestDto backtestDto,
            List<Double> residuals,
            LocalDate currentDate
    ) {
        ConfidenceEvaluationResult result = new ConfidenceEvaluationResult();

        if (observations == null || observations.isEmpty() || currentDate == null) {
            result.setConfidence(ForecastConfidence.INSUFFICIENT_DATA);
            result.setDataQuality("INSUFFICIENT");
            result.setDirection(ForecastDirection.UNCERTAIN);
            return result;
        }

        // 1. Determine Freshness Gap
        MarketPriceSummaryResponse latestObs = observations.get(observations.size() - 1);
        String latestDateStr = latestObs.getPriceDate();
        LocalDate latestDate;
        try {
            latestDate = LocalDate.parse(latestDateStr.trim());
        } catch (Exception e) {
            latestDate = currentDate;
        }

        long gapDays = ChronoUnit.DAYS.between(latestDate, currentDate);
        result.setFreshnessGapDays(gapDays);
        result.setFresh(gapDays <= 2);

        int obsCount = observations.size();
        int minObs = getMinimumObservations(horizon);

        // 2. Data Quality Rating
        if (gapDays > 7 || obsCount < minObs) {
            result.setDataQuality("INSUFFICIENT");
        } else if (gapDays > 3 || obsCount < 10) {
            result.setDataQuality("LIMITED");
        } else {
            result.setDataQuality("GOOD");
        }

        // Precedence Step 1: Data Sufficiency Overrides
        if (obsCount < minObs || gapDays > 7 || forecastPrice == null || forecastPrice <= 0) {
            result.setConfidence(ForecastConfidence.INSUFFICIENT_DATA);
            result.setDirection(ForecastDirection.UNCERTAIN);
            return result;
        }

        // 3. Compute Price Volatility (Coefficient of Variation CV = sigma / mu)
        double sum = 0.0;
        for (MarketPriceSummaryResponse p : observations) {
            sum += p.getModalPrice();
        }
        double mean = sum / obsCount;

        double sumSqDiff = 0.0;
        for (MarketPriceSummaryResponse p : observations) {
            double diff = p.getModalPrice() - mean;
            sumSqDiff += diff * diff;
        }
        double obsStdDev = obsCount > 1 ? Math.sqrt(sumSqDiff / (obsCount - 1)) : 0.0;
        double cv = mean > 0 ? (obsStdDev / mean) : 0.0;

        Double backtestMape = backtestDto != null ? backtestDto.getMape() : null;
        int residualCount = residuals != null ? residuals.size() : 0;

        // 4. Compute Base Confidence Tier
        ForecastConfidence confidence;
        if (obsCount >= 10 && gapDays <= 2 && (backtestMape == null || backtestMape <= 5.0) && cv <= 0.15 && horizon != ForecastHorizon.FOURTEEN_DAYS) {
            confidence = ForecastConfidence.HIGH;
        } else if (backtestMape != null && backtestMape > 12.0) {
            confidence = ForecastConfidence.LOW;
        } else {
            confidence = ForecastConfidence.MEDIUM;
        }

        // Precedence Step 2: Freshness & Residual Bounds Overrides
        if (gapDays > 3 && confidence == ForecastConfidence.HIGH) {
            confidence = ForecastConfidence.MEDIUM;
        }
        if (residualCount < 5 || (backtestMape != null && backtestMape > 15.0)) {
            confidence = ForecastConfidence.LOW;
        }

        // Precedence Step 3: Volatility & Horizon Degradation
        if (cv > 0.20 && confidence == ForecastConfidence.HIGH) {
            confidence = ForecastConfidence.MEDIUM;
        } else if (cv > 0.20 && confidence == ForecastConfidence.MEDIUM) {
            confidence = ForecastConfidence.LOW;
        }

        if (horizon == ForecastHorizon.FOURTEEN_DAYS && confidence == ForecastConfidence.HIGH) {
            confidence = ForecastConfidence.MEDIUM;
        }

        result.setConfidence(confidence);

        // 5. Compute Empirical Residual Uncertainty Bounds
        Double lowerBound = null;
        Double upperBound = null;
        Double residualStdDev = null;

        if (residualCount >= 5) {
            double resSum = 0.0;
            for (double r : residuals) {
                resSum += r;
            }
            double resMean = resSum / residualCount;

            double resSqDiff = 0.0;
            for (double r : residuals) {
                double diff = r - resMean;
                resSqDiff += diff * diff;
            }
            residualStdDev = Math.sqrt(resSqDiff / (residualCount - 1));
            result.setSampleStandardDeviation(residualStdDev);

            double margin = 1.96 * residualStdDev;
            double lower = Math.max(0.0, forecastPrice - margin);
            double upper = Math.max(0.0, forecastPrice + margin);

            lowerBound = BigDecimal.valueOf(lower).setScale(2, RoundingMode.HALF_UP).doubleValue();
            upperBound = BigDecimal.valueOf(upper).setScale(2, RoundingMode.HALF_UP).doubleValue();
        } else {
            // Insufficient residuals: Force LOW confidence if not already INSUFFICIENT
            if (confidence != ForecastConfidence.INSUFFICIENT_DATA) {
                result.setConfidence(ForecastConfidence.LOW);
            }
        }

        result.setLowerBound(lowerBound);
        result.setUpperBound(upperBound);

        // 6. Direction Classification & Precedence Rules
        double currentVal = currentPrice != null && currentPrice > 0 ? currentPrice : mean;
        double pctChange = ((forecastPrice - currentVal) / currentVal) * 100.0;
        double relUncertainty = residualStdDev != null && currentVal > 0 ? (residualStdDev / currentVal) : 1.0;

        ForecastDirection direction;
        if (result.getConfidence() == ForecastConfidence.LOW
                || result.getConfidence() == ForecastConfidence.INSUFFICIENT_DATA
                || lowerBound == null
                || relUncertainty > 0.15) {
            direction = ForecastDirection.UNCERTAIN;
        } else if (pctChange > 1.5) {
            direction = ForecastDirection.UP;
        } else if (pctChange < -1.5) {
            direction = ForecastDirection.DOWN;
        } else {
            direction = ForecastDirection.STABLE;
        }

        result.setDirection(direction);
        return result;
    }

    private int getMinimumObservations(ForecastHorizon horizon) {
        if (horizon == null) return 5;
        switch (horizon) {
            case ONE_DAY:
            case THREE_DAYS:
                return 5;
            case SEVEN_DAYS:
                return 7;
            case FOURTEEN_DAYS:
                return 10;
            default:
                return 5;
        }
    }
}
