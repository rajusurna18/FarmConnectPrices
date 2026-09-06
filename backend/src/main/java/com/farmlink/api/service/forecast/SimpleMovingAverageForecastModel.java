package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.ForecastHorizon;
import com.farmlink.api.dto.forecast.ForecastModelType;
import com.farmlink.api.dto.forecast.ForecastRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleMovingAverageForecastModel implements ForecastModel {

    public static final String VERSION = "1.0.0";
    public static final int MAX_OBSERVATIONS = 14;

    @Override
    public ForecastModelType getModelType() {
        return ForecastModelType.SIMPLE_MOVING_AVERAGE_V1;
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
            return null;
        }

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

        int steps = horizon.getDays();
        List<Double> series = new ArrayList<>(recentPrices);

        for (int step = 1; step <= steps; step++) {
            int k = Math.min(series.size(), MAX_OBSERVATIONS);
            int start = series.size() - k;
            double sum = 0.0;
            for (int i = start; i < series.size(); i++) {
                sum += series.get(i);
            }
            double nextStep = sum / k;
            series.add(nextStep);
        }

        double finalForecast = series.get(series.size() - 1);
        return BigDecimal.valueOf(finalForecast).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
