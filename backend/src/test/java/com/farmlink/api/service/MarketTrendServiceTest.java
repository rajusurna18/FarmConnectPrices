package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.MarketResponse;
import com.farmlink.api.dto.MarketTrendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketTrendServiceTest {

    private MarketPriceService marketPriceService;
    private MarketService marketService;
    private CropMasterService cropMasterService;
    private PriceUnitConversionService conversionService;
    private MarketTrendService trendService;

    @BeforeEach
    void setUp() {
        marketPriceService = mock(MarketPriceService.class);
        marketService = mock(MarketService.class);
        cropMasterService = mock(CropMasterService.class);
        conversionService = new PriceUnitConversionService();

        when(cropMasterService.getCropById("crop-tomato"))
                .thenReturn(new CropResponse("crop-tomato", "Tomato", "VEGETABLE", "Solanum lycopersicum", "ACTIVE"));

        when(marketService.getMarketById("mkt-rythu"))
                .thenReturn(new MarketResponse("mkt-rythu", "Rythu Bazar", "MKT001", "MANDI", null, 16.5, 80.5, "ACTIVE", null, null));

        trendService = new MarketTrendService(marketPriceService, marketService, cropMasterService, conversionService);
    }

    @Test
    void calculateTrend_zeroObservations_returnsInsufficientDataAndUnavailableFreshness() {
        when(marketPriceService.getMarketPrices(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        MarketTrendResponse res = trendService.calculateTrend("crop-tomato", "mkt-rythu", "30D", null, null, "QUINTAL");

        assertNotNull(res);
        assertEquals(0, res.getObservationCount());
        assertEquals("INSUFFICIENT_DATA", res.getTrendDirection());
        assertEquals("INSUFFICIENT_DATA", res.getVolatility());
        assertEquals("INSUFFICIENT", res.getDataQuality());
        assertEquals("UNAVAILABLE", res.getFreshnessStatus());
        assertNull(res.getEarliestPrice());
        assertNull(res.getLatestPrice());
    }

    @Test
    void calculateTrend_oneObservation_returnsInsufficientTrendAndInsufficientVolatility() {
        MarketPriceSummaryResponse obs = new MarketPriceSummaryResponse(
                "p1", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", LocalDate.now().toString(),
                1500.0, 2500.0, 2000.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(obs));

        MarketTrendResponse res = trendService.calculateTrend("crop-tomato", "mkt-rythu", "30D", null, null, "QUINTAL");

        assertNotNull(res);
        assertEquals(1, res.getObservationCount());
        assertEquals("INSUFFICIENT_DATA", res.getTrendDirection());
        assertEquals("INSUFFICIENT_DATA", res.getVolatility());
        assertEquals("INSUFFICIENT", res.getDataQuality());
        assertEquals("FRESH", res.getFreshnessStatus());
        assertEquals(2000.0, res.getLatestPrice());
        assertEquals(2000.0, res.getAvgPrice());
    }

    @Test
    void calculateTrend_risingTrend_calculatesCorrectPercentagesAndQuality() {
        String today = LocalDate.now().toString();
        String fiveDaysAgo = LocalDate.now().minusDays(5).toString();

        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", fiveDaysAgo,
                1800.0, 2200.0, 2000.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", today,
                2200.0, 2800.0, 2500.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(obs1, obs2));

        MarketTrendResponse res = trendService.calculateTrend("crop-tomato", "mkt-rythu", "30D", null, null, "QUINTAL");

        assertNotNull(res);
        assertEquals(2, res.getObservationCount());
        assertEquals("RISING", res.getTrendDirection());
        assertEquals(500.0, res.getAbsoluteChange());
        assertEquals(25.0, res.getPercentageChange());
        assertEquals(500.0, res.getPriceRange());
        assertEquals(2250.0, res.getAvgPrice());
        assertEquals("LIMITED", res.getDataQuality()); // 2-4 obs -> LIMITED
        assertEquals("INSUFFICIENT_DATA", res.getVolatility()); // <3 obs -> INSUFFICIENT_DATA
    }

    @Test
    void calculateTrend_fallingTrend_calculatesNegativePercentage() {
        String tenDaysAgo = LocalDate.now().minusDays(10).toString();
        String twentyDaysAgo = LocalDate.now().minusDays(20).toString();

        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", twentyDaysAgo,
                2800.0, 3200.0, 3000.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", tenDaysAgo,
                2000.0, 2400.0, 2200.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(obs1, obs2));

        MarketTrendResponse res = trendService.calculateTrend("crop-tomato", "mkt-rythu", "30D", null, null, "QUINTAL");

        assertNotNull(res);
        assertEquals("FALLING", res.getTrendDirection());
        assertEquals(-800.0, res.getAbsoluteChange());
        assertEquals(-26.67, res.getPercentageChange());
        assertEquals("STALE", res.getFreshnessStatus()); // Latest observation date is 10 days ago ( > 7 days )
    }

    @Test
    void calculateTrend_stableTrend_withinThresholds() {
        String today = LocalDate.now().toString();
        String threeDaysAgo = LocalDate.now().minusDays(3).toString();

        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", threeDaysAgo,
                1950.0, 2050.0, 2000.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-rythu", "Rythu Bazar", "crop-tomato", "Tomato", today,
                1960.0, 2060.0, 2020.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(obs1, obs2));

        MarketTrendResponse res = trendService.calculateTrend("crop-tomato", "mkt-rythu", "30D", null, null, "QUINTAL");

        assertNotNull(res);
        // Change is +1.0%, which is below the +2.0% rising threshold -> STABLE
        assertEquals("STABLE", res.getTrendDirection());
        assertEquals(1.0, res.getPercentageChange());
    }

    @Test
    void calculateVolatility_lowMediumHighClassifications() {
        // Low Volatility: prices [2000, 2010, 2020, 2000, 2010] (CV < 5%)
        List<Double> lowPrices = List.of(2000.0, 2010.0, 2020.0, 2000.0, 2010.0);
        MarketTrendService.VolatilityResult lowRes = trendService.calculateVolatility(lowPrices);
        assertEquals("LOW", lowRes.getCategory());
        assertTrue(lowRes.getCvPercent() < 5.0);

        // Medium Volatility: prices [2000, 2200, 1800, 2100, 1900] (5% <= CV <= 15%)
        List<Double> medPrices = List.of(2000.0, 2200.0, 1800.0, 2100.0, 1900.0);
        MarketTrendService.VolatilityResult medRes = trendService.calculateVolatility(medPrices);
        assertEquals("MEDIUM", medRes.getCategory());

        // High Volatility: prices [1000, 3000, 1500, 4000, 2000] (CV > 15%)
        List<Double> highPrices = List.of(1000.0, 3000.0, 1500.0, 4000.0, 2000.0);
        MarketTrendService.VolatilityResult highRes = trendService.calculateVolatility(highPrices);
        assertEquals("HIGH", highRes.getCategory());
        assertTrue(highRes.getCvPercent() > 15.0);
    }

    @Test
    void calculateVolatility_zeroMeanAndInsufficientPrices_handledSafely() {
        MarketTrendService.VolatilityResult twoObs = trendService.calculateVolatility(List.of(10.0, 20.0));
        assertEquals("INSUFFICIENT_DATA", twoObs.getCategory());
        assertNull(twoObs.getCvPercent());

        MarketTrendService.VolatilityResult zeroMean = trendService.calculateVolatility(List.of(0.0, 0.0, 0.0));
        assertEquals("LOW", zeroMean.getCategory());
        assertEquals(0.0, zeroMean.getCvPercent());
    }

    @Test
    void resolveAndValidateDateRange_presetPeriods_calculatesBounds() {
        MarketTrendService.ResolvedDateRange r7d = trendService.resolveAndValidateDateRange("7D", null, null);
        assertEquals("7D", r7d.getPeriodCode());
        assertEquals(LocalDate.now().minusDays(7).toString(), r7d.getStartDate());

        MarketTrendService.ResolvedDateRange r1y = trendService.resolveAndValidateDateRange("1Y", null, null);
        assertEquals("1Y", r1y.getPeriodCode());
        assertEquals(LocalDate.now().minusDays(365).toString(), r1y.getStartDate());
    }

    @Test
    void resolveAndValidateDateRange_customValid_succeeds() {
        String start = LocalDate.now().minusDays(60).toString();
        String end = LocalDate.now().minusDays(10).toString();
        MarketTrendService.ResolvedDateRange res = trendService.resolveAndValidateDateRange("CUSTOM", start, end);
        assertEquals("CUSTOM", res.getPeriodCode());
        assertEquals(start, res.getStartDate());
        assertEquals(end, res.getEndDate());
    }

    @Test
    void resolveAndValidateDateRange_customInvalidReversedOrFuture_throwsException() {
        String today = LocalDate.now().toString();
        String future = LocalDate.now().plusDays(5).toString();
        String past = LocalDate.now().minusDays(10).toString();

        // 1. Missing dates
        assertThrows(IllegalArgumentException.class, () -> trendService.resolveAndValidateDateRange("CUSTOM", null, today));

        // 2. Start > End
        assertThrows(IllegalArgumentException.class, () -> trendService.resolveAndValidateDateRange("CUSTOM", today, past));

        // 3. End in future
        assertThrows(IllegalArgumentException.class, () -> trendService.resolveAndValidateDateRange("CUSTOM", past, future));

        // 4. Span > 365 days
        String oldStart = LocalDate.now().minusDays(400).toString();
        assertThrows(IllegalArgumentException.class, () -> trendService.resolveAndValidateDateRange("CUSTOM", oldStart, today));
    }
}
