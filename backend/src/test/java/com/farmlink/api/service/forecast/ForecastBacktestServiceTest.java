package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.ForecastHorizon;
import com.farmlink.api.dto.forecast.ForecastRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForecastBacktestServiceTest {

    private ForecastBacktestService backtestService;
    private WeightedMovingAverageForecastModel model;

    @BeforeEach
    void setUp() {
        backtestService = new ForecastBacktestService();
        model = new WeightedMovingAverageForecastModel();
    }

    @Test
    void testExecuteBacktest_ValidSeries_CalculatesMaeAndMape() {
        // Daily observations for 15 consecutive days
        List<MarketPriceSummaryResponse> obs = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setCropId("crop-1");
            p.setMarketId("mkt-1");
            p.setPriceDate(String.format("2026-09-%02d", i));
            p.setModalPrice(2000.0 + (i * 20.0)); // 2020, 2040, ..., 2300
            p.setMinPrice(1900.0);
            p.setMaxPrice(2400.0);
            p.setQualityStatus("VERIFIED");
            obs.add(p);
        }

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "1_DAY", 30, "QUINTAL");
        ForecastBacktestService.DetailedBacktestResult result = backtestService.executeBacktest(
                model, obs, req, ForecastHorizon.ONE_DAY
        );

        assertNotNull(result);
        assertNotNull(result.getDto());
        assertTrue(result.getDto().getSampleCount() > 0, "Should have valid backtest samples");
        assertNotNull(result.getDto().getMae(), "MAE should be calculated");
        assertNotNull(result.getDto().getMape(), "MAPE should be calculated");
        assertTrue(result.getDto().getMae() >= 0, "MAE must be non-negative");
        assertTrue(result.getDto().getMape() >= 0, "MAPE must be non-negative");
        assertEquals(result.getDto().getSampleCount(), result.getResiduals().size());
    }

    @Test
    void testExecuteBacktest_InsufficientObservations_ReturnsZeroSamples() {
        // Only 3 observations
        List<MarketPriceSummaryResponse> obs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setPriceDate(String.format("2026-09-%02d", i));
            p.setModalPrice(2000.0);
            p.setQualityStatus("VERIFIED");
            obs.add(p);
        }

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "7_DAYS", 30, "QUINTAL");
        ForecastBacktestService.DetailedBacktestResult result = backtestService.executeBacktest(
                model, obs, req, ForecastHorizon.SEVEN_DAYS
        );

        assertEquals(0, result.getDto().getSampleCount());
        assertNull(result.getDto().getMae());
        assertNull(result.getDto().getMape());
        assertTrue(result.getResiduals().isEmpty());
    }
}
