package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForecastConfidenceServiceTest {

    private ForecastConfidenceService confidenceService;

    @BeforeEach
    void setUp() {
        confidenceService = new ForecastConfidenceService();
    }

    @Test
    void testEvaluateConfidenceAndBounds_InsufficientResiduals_ReturnsNullBoundsAndLowConfidence() {
        List<MarketPriceSummaryResponse> obs = createObservations(10, "2026-09-05");
        ForecastBacktestDto backtest = new ForecastBacktestDto(3, 50.0, 2.5, "7_DAYS");
        List<Double> residuals = List.of(10.0, -15.0, 5.0); // Only 3 residuals (< 5)

        LocalDate today = LocalDate.parse("2026-09-06");
        ForecastConfidenceService.ConfidenceEvaluationResult result = confidenceService.evaluateConfidenceAndBounds(
                2475.0, 2400.0, obs, ForecastHorizon.SEVEN_DAYS, backtest, residuals, today
        );

        assertNull(result.getLowerBound(), "Lower bound must be null when residuals < 5");
        assertNull(result.getUpperBound(), "Upper bound must be null when residuals < 5");
        assertEquals(ForecastConfidence.LOW, result.getConfidence(), "Confidence must be LOW when residual count < 5");
        assertEquals(ForecastDirection.UNCERTAIN, result.getDirection(), "Direction must be UNCERTAIN when bounds are null");
    }

    @Test
    void testEvaluateConfidenceAndBounds_SufficientResiduals_CalculatesBoundsAndConfidence() {
        List<MarketPriceSummaryResponse> obs = createObservations(12, "2026-09-05");
        ForecastBacktestDto backtest = new ForecastBacktestDto(8, 30.0, 1.2, "7_DAYS");
        List<Double> residuals = List.of(10.0, -12.0, 15.0, -8.0, 5.0, -4.0, 9.0, -2.0); // 8 residuals (>= 5)

        LocalDate today = LocalDate.parse("2026-09-06");
        ForecastConfidenceService.ConfidenceEvaluationResult result = confidenceService.evaluateConfidenceAndBounds(
                2475.0, 2400.0, obs, ForecastHorizon.SEVEN_DAYS, backtest, residuals, today
        );

        assertNotNull(result.getLowerBound(), "Lower bound must be calculated");
        assertNotNull(result.getUpperBound(), "Upper bound must be calculated");
        assertTrue(result.getLowerBound() < 2475.0);
        assertTrue(result.getUpperBound() > 2475.0);
        assertNotEquals(ForecastConfidence.INSUFFICIENT_DATA, result.getConfidence());
    }

    @Test
    void testEvaluateConfidenceAndBounds_StaleData_DegradesConfidence() {
        // Data is 10 days old (latest: 2026-08-27, today: 2026-09-06)
        List<MarketPriceSummaryResponse> obs = createObservations(10, "2026-08-27");
        ForecastBacktestDto backtest = new ForecastBacktestDto(10, 20.0, 1.0, "7_DAYS");
        List<Double> residuals = List.of(5.0, -5.0, 4.0, -4.0, 3.0, -3.0);

        LocalDate today = LocalDate.parse("2026-09-06");
        ForecastConfidenceService.ConfidenceEvaluationResult result = confidenceService.evaluateConfidenceAndBounds(
                2475.0, 2400.0, obs, ForecastHorizon.SEVEN_DAYS, backtest, residuals, today
        );

        assertEquals(ForecastConfidence.INSUFFICIENT_DATA, result.getConfidence(), "Freshness gap > 7 days forces INSUFFICIENT_DATA");
        assertEquals("INSUFFICIENT", result.getDataQuality());
    }

    private List<MarketPriceSummaryResponse> createObservations(int count, String latestDate) {
        LocalDate end = LocalDate.parse(latestDate);
        List<MarketPriceSummaryResponse> list = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            LocalDate d = end.minusDays(i);
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setPriceDate(d.toString());
            p.setModalPrice(2400.0);
            p.setMinPrice(2300.0);
            p.setMaxPrice(2500.0);
            p.setQualityStatus("VERIFIED");
            list.add(p);
        }
        return list;
    }
}
