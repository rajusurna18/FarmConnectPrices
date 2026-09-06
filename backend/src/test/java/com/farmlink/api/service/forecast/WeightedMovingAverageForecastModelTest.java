package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.ForecastHorizon;
import com.farmlink.api.dto.forecast.ForecastRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeightedMovingAverageForecastModelTest {

    private WeightedMovingAverageForecastModel model;

    @BeforeEach
    void setUp() {
        model = new WeightedMovingAverageForecastModel();
    }

    @Test
    void testSingleStepWma_KnownSeries() {
        // Series: [10, 20, 30]
        // Weights: 1, 2, 3 -> Sum = 6
        // Weighted Sum = (10*1 + 20*2 + 30*3) = 10 + 40 + 90 = 140
        // Expected WMA = 140 / 6 = 23.3333...
        List<Double> series = List.of(10.0, 20.0, 30.0);
        double result = model.calculateSingleStepWma(series);
        assertEquals(23.3333, result, 0.001);
    }

    @Test
    void testForecastPrice_FlatSeries() {
        // Flat series of 2400.0 across 10 days
        List<MarketPriceSummaryResponse> obs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setPriceDate(String.format("2026-09-%02d", i));
            p.setModalPrice(2400.0);
            p.setMinPrice(2350.0);
            p.setMaxPrice(2450.0);
            p.setQualityStatus("VERIFIED");
            obs.add(p);
        }

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "7_DAYS", 30, "QUINTAL");
        Double forecast = model.forecastPrice(obs, req);

        assertNotNull(forecast);
        assertEquals(2400.0, forecast, 0.01);
    }

    @Test
    void testForecastPrice_RisingSeries() {
        // Rising series: 2000, 2100, 2200, 2300, 2400, 2500, 2600
        List<MarketPriceSummaryResponse> obs = new ArrayList<>();
        double[] prices = {2000, 2100, 2200, 2300, 2400, 2500, 2600};
        for (int i = 0; i < prices.length; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setPriceDate(String.format("2026-09-%02d", i + 1));
            p.setModalPrice(prices[i]);
            p.setMinPrice(prices[i] - 50);
            p.setMaxPrice(prices[i] + 50);
            p.setQualityStatus("VERIFIED");
            obs.add(p);
        }

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "1_DAY", 30, "QUINTAL");
        Double forecast = model.forecastPrice(obs, req);

        assertNotNull(forecast);
        // Weighted average gives more weight to recent higher values (2600, 2500, etc.)
        assertTrue(forecast > 2300.0, "WMA forecast should be weighted towards recent higher prices");
    }

    @Test
    void testForecastPrice_InsufficientObservations_ReturnsNull() {
        // Only 3 observations, minimum for 7_DAYS is 7
        List<MarketPriceSummaryResponse> obs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setPriceDate(String.format("2026-09-%02d", i));
            p.setModalPrice(2400.0);
            p.setMinPrice(2350.0);
            p.setMaxPrice(2450.0);
            p.setQualityStatus("VERIFIED");
            obs.add(p);
        }

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "7_DAYS", 30, "QUINTAL");
        Double forecast = model.forecastPrice(obs, req);

        assertNull(forecast, "Forecast should be null when observations are below minimum threshold");
    }

    @Test
    void testProductionAndBacktestIdentity() {
        // Verify that running forecast on identical series yields exact same value
        List<MarketPriceSummaryResponse> obs = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setPriceDate(String.format("2026-09-%02d", i));
            p.setModalPrice(2000.0 + (i * 50));
            p.setMinPrice(1900.0 + (i * 50));
            p.setMaxPrice(2100.0 + (i * 50));
            p.setQualityStatus("VERIFIED");
            obs.add(p);
        }

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "3_DAYS", 30, "QUINTAL");
        Double run1 = model.forecastPrice(obs, req);
        Double run2 = model.forecastPrice(obs, req);

        assertNotNull(run1);
        assertEquals(run1, run2, "Repeated execution on identical series must yield identical result");
    }
}
