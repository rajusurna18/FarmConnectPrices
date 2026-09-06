package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.ForecastHorizon;
import com.farmlink.api.dto.forecast.ForecastModelType;
import com.farmlink.api.dto.forecast.ForecastRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class WeightedMovingAverageForecastModel implements ForecastModel {

    private static final Logger log = LoggerFactory.getLogger(WeightedMovingAverageForecastModel.class);
    public static final String VERSION = "1.0.0";
    public static final int MAX_OBSERVATIONS = 14;

    @Override
    public ForecastModelType getModelType() {
        return ForecastModelType.WEIGHTED_MOVING_AVERAGE_V1;
    }

    @Override
    public String getModelVersion() {
        return VERSION;
    }

    @Override
    public int getMinimumObservations(ForecastHorizon horizon) {
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

    @Override
    public Double forecastPrice(List<MarketPriceSummaryResponse> observations, ForecastRequest request) {
        ForecastHorizon horizon = ForecastHorizon.fromCode(request != null ? request.getHorizon() : null);
        int minRequired = getMinimumObservations(horizon);

        if (observations == null || observations.size() < minRequired) {
            log.debug("Insufficient observations for WMA forecast. Required={}, actual={}",
                    minRequired, observations != null ? observations.size() : 0);
            return null;
        }

        // Take up to MAX_OBSERVATIONS (14) most recent observations from chronologically sorted list (oldest to newest)
        int size = observations.size();
        int startIndex = Math.max(0, size - MAX_OBSERVATIONS);
        List<Double> recentPrices = new ArrayList<>();
        for (int i = startIndex; i < size; i++) {
            MarketPriceSummaryResponse p = observations.get(i);
            if (p != null && p.getModalPrice() > 0) {
                recentPrices.add(p.getModalPrice());
            }
        }

        if (recentPrices.size() < minRequired) {
            return null;
        }

        // Pure Recursive Multi-Step Forecasting
        int steps = horizon.getDays();
        List<Double> series = new ArrayList<>(recentPrices);

        for (int step = 1; step <= steps; step++) {
            double nextStepPred = calculateSingleStepWma(series);
            series.add(nextStepPred);
        }

        // Final prediction for target horizon h is the last value in series
        double finalForecast = series.get(series.size() - 1);
        BigDecimal bd = BigDecimal.valueOf(finalForecast).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Calculates 1-step WMA using up to 14 most recent values in series.
     * Weights: 1, 2, ..., K (newest has weight K)
     * Normalized weight bar_w_i = i / sum(1..K)
     */
    public double calculateSingleStepWma(List<Double> series) {
        int k = Math.min(series.size(), MAX_OBSERVATIONS);
        int start = series.size() - k;

        double sumWeights = (k * (k + 1.0)) / 2.0;
        double weightedSum = 0.0;

        for (int i = 0; i < k; i++) {
            double price = series.get(start + i);
            double weight = (i + 1); // 1-indexed weight
            weightedSum += price * weight;
        }

        return weightedSum / sumWeights;
    }
}
