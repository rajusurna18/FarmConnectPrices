package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.ForecastBacktestDto;
import com.farmlink.api.dto.forecast.ForecastHorizon;
import com.farmlink.api.dto.forecast.ForecastRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ForecastBacktestService {

    private static final Logger log = LoggerFactory.getLogger(ForecastBacktestService.class);

    public static class DetailedBacktestResult {
        private final ForecastBacktestDto dto;
        private final List<Double> residuals; // e_m,h = y_actual - y_pred

        public DetailedBacktestResult(ForecastBacktestDto dto, List<Double> residuals) {
            this.dto = dto;
            this.residuals = residuals != null ? residuals : Collections.emptyList();
        }

        public ForecastBacktestDto getDto() {
            return dto;
        }

        public List<Double> getResiduals() {
            return residuals;
        }
    }

    /**
     * Executes origin-relative walk-forward out-of-sample backtesting for horizon h.
     */
    public DetailedBacktestResult executeBacktest(
            ForecastModel model,
            List<MarketPriceSummaryResponse> observations,
            ForecastRequest request,
            ForecastHorizon horizon
    ) {
        if (model == null || observations == null || horizon == null) {
            return new DetailedBacktestResult(new ForecastBacktestDto(0, null, null, horizon != null ? horizon.getCode() : null), Collections.emptyList());
        }

        int minObs = model.getMinimumObservations(horizon);
        if (observations.size() < minObs + 1) {
            return new DetailedBacktestResult(new ForecastBacktestDto(0, null, null, horizon.getCode()), Collections.emptyList());
        }

        // Map priceDate (YYYY-MM-DD) -> MarketPriceSummaryResponse for quick lookup of actuals
        Map<String, MarketPriceSummaryResponse> dateToObs = new HashMap<>();
        for (MarketPriceSummaryResponse obs : observations) {
            if (obs.getPriceDate() != null) {
                dateToObs.put(obs.getPriceDate().trim(), obs);
            }
        }

        List<Double> absoluteErrors = new ArrayList<>();
        List<Double> percentageErrors = new ArrayList<>();
        List<Double> residuals = new ArrayList<>();

        int horizonDays = horizon.getDays();

        // Walk-forward iteration across historical cutoff points m
        for (int m = minObs - 1; m < observations.size(); m++) {
            List<MarketPriceSummaryResponse> trainingSlice = new ArrayList<>(observations.subList(0, m + 1));
            MarketPriceSummaryResponse cutoffObs = observations.get(m);
            String cutoffDateStr = cutoffObs.getPriceDate();

            LocalDate cutoffDate;
            try {
                cutoffDate = LocalDate.parse(cutoffDateStr.trim());
            } catch (DateTimeParseException e) {
                continue;
            }

            LocalDate targetCalendarDate = cutoffDate.plusDays(horizonDays);
            String targetDateStr = targetCalendarDate.toString();

            // Look up actual observation at target calendar date
            MarketPriceSummaryResponse actualObs = dateToObs.get(targetDateStr);
            if (actualObs == null || actualObs.getModalPrice() <= 0) {
                continue; // No actual observation exists for target calendar date
            }

            // Run forecast on training slice strictly <= cutoffDate
            ForecastRequest req = new ForecastRequest(request.getCropId(), request.getMarketId(), horizon.getCode(), 30, request.getUnit());
            Double forecastVal = model.forecastPrice(trainingSlice, req);

            if (forecastVal == null || forecastVal <= 0) {
                continue;
            }

            double actualVal = actualObs.getModalPrice();
            double residual = actualVal - forecastVal;
            double absError = Math.abs(residual);

            residuals.add(residual);
            absoluteErrors.add(absError);

            if (actualVal > 0) {
                double pctErr = (absError / actualVal) * 100.0;
                percentageErrors.add(pctErr);
            }
        }

        int sampleCount = absoluteErrors.size();
        if (sampleCount == 0) {
            return new DetailedBacktestResult(new ForecastBacktestDto(0, null, null, horizon.getCode()), Collections.emptyList());
        }

        // Calculate MAE
        double sumAbs = 0.0;
        for (double err : absoluteErrors) {
            sumAbs += err;
        }
        double mae = BigDecimal.valueOf(sumAbs / sampleCount).setScale(2, RoundingMode.HALF_UP).doubleValue();

        // Calculate MAPE
        Double mape = null;
        if (!percentageErrors.isEmpty()) {
            double sumPct = 0.0;
            for (double pct : percentageErrors) {
                sumPct += pct;
            }
            mape = BigDecimal.valueOf(sumPct / percentageErrors.size()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        ForecastBacktestDto dto = new ForecastBacktestDto(sampleCount, mae, mape, horizon.getCode());
        return new DetailedBacktestResult(dto, residuals);
    }
}
