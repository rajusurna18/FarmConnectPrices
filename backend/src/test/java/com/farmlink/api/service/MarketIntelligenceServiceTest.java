package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketIntelligenceServiceTest {

    private MarketPriceService marketPriceService;
    private MarketService marketService;
    private CropMasterService cropMasterService;
    private PriceUnitConversionService conversionService;
    private MarketIntelligenceService intelligenceService;

    @BeforeEach
    void setUp() {
        marketPriceService = mock(MarketPriceService.class);
        marketService = mock(MarketService.class);
        cropMasterService = mock(CropMasterService.class);
        conversionService = new PriceUnitConversionService();

        when(cropMasterService.getCropById("crop-chilli"))
                .thenReturn(new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"));

        intelligenceService = new MarketIntelligenceService(marketPriceService, marketService, cropMasterService, conversionService);
    }

    @Test
    void compareMarkets_quintalToQuintal_calculatesCorrectly() {
        MarketPriceSummaryResponse m1 = new MarketPriceSummaryResponse(
                "prc-1", "mkt-guntur", "Guntur Mandi", "crop-chilli", "Red Chilli", "2026-08-26",
                18000.0, 22000.0, 20000.0, "INR", "QUINTAL", "GOVERNMENT_API", "data.gov.in / AGMARKNET", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse m2 = new MarketPriceSummaryResponse(
                "prc-2", "mkt-warangal", "Warangal Mandi", "crop-chilli", "Red Chilli", "2026-08-26",
                20000.0, 26000.0, 24000.0, "INR", "QUINTAL", "GOVERNMENT_API", "data.gov.in / AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("QUINTAL"), any(), any(), anyInt()
        )).thenReturn(List.of(m1, m2));

        MarketComparisonResponse result = intelligenceService.compareMarkets("crop-chilli", "2026-08-26", null, null, null, null, "QUINTAL");

        assertNotNull(result);
        assertEquals(2, result.getMarkets().size());
        assertEquals("mkt-warangal", result.getHighestMarket().getMarketId());
        assertEquals(24000.0, result.getHighestMarket().getModalPrice());
        assertEquals("mkt-guntur", result.getLowestMarket().getMarketId());
        assertEquals(20000.0, result.getLowestMarket().getModalPrice());

        assertEquals(4000.0, result.getPriceDifference());
        assertNotNull(result.getPercentageDifference());
        assertEquals(20.0, result.getPercentageDifference(), 0.01);
    }

    @Test
    void compareMarkets_quintalToKg_convertsValuesAndPreservesRankingAndPercentage() {
        MarketPriceSummaryResponse m1 = new MarketPriceSummaryResponse(
                "prc-1", "mkt-guntur", "Guntur Mandi", "crop-chilli", "Red Chilli", "2026-08-26",
                18500.0, 22500.0, 20500.0, "INR", "QUINTAL", "GOVERNMENT_API", "data.gov.in / AGMARKNET", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse m2 = new MarketPriceSummaryResponse(
                "prc-2", "mkt-warangal", "Warangal Mandi", "crop-chilli", "Red Chilli", "2026-08-26",
                20000.0, 26000.0, 24600.0, "INR", "QUINTAL", "GOVERNMENT_API", "data.gov.in / AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("KG"), any(), any(), anyInt()
        )).thenReturn(List.of(m1, m2));

        MarketComparisonResponse result = intelligenceService.compareMarkets("crop-chilli", "2026-08-26", null, null, null, null, "KG");

        assertNotNull(result);
        assertEquals("KG", result.getUnit());
        assertEquals(2, result.getMarkets().size());
        // Rank 1: Warangal (246.0 / KG)
        assertEquals("mkt-warangal", result.getHighestMarket().getMarketId());
        assertEquals(246.0, result.getHighestMarket().getModalPrice());
        assertTrue(result.getHighestMarket().isConversionApplied());
        assertEquals("QUINTAL", result.getHighestMarket().getSourceUnit());

        // Rank 2: Guntur (205.0 / KG)
        assertEquals("mkt-guntur", result.getLowestMarket().getMarketId());
        assertEquals(205.0, result.getLowestMarket().getModalPrice());

        // Price difference: 246 - 205 = 41 / KG
        assertEquals(41.0, result.getPriceDifference(), 0.01);
        // Percentage difference: (41 / 205) * 100 = 20.0%
        assertEquals(20.0, result.getPercentageDifference(), 0.01);
    }

    @Test
    void compareMarkets_quintalToTonne_convertsValuesCorrectly() {
        MarketPriceSummaryResponse m1 = new MarketPriceSummaryResponse(
                "prc-1", "mkt-guntur", "Guntur Mandi", "crop-chilli", "Red Chilli", "2026-08-26",
                18500.0, 22500.0, 20500.0, "INR", "QUINTAL", "GOVERNMENT_API", "data.gov.in / AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("TONNE"), any(), any(), anyInt()
        )).thenReturn(List.of(m1));

        MarketComparisonResponse result = intelligenceService.compareMarkets("crop-chilli", "2026-08-26", null, null, null, null, "TONNE");

        assertNotNull(result);
        assertEquals("TONNE", result.getUnit());
        assertEquals(205000.0, result.getHighestMarket().getModalPrice());
        assertTrue(result.getHighestMarket().isConversionApplied());
    }

    @Test
    void compareMarkets_mixedSourceUnits_normalizesAllToRequestedUnit() {
        MarketPriceSummaryResponse mQuintal = new MarketPriceSummaryResponse(
                "p1", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-26",
                18500.0, 22500.0, 20500.0, "INR", "QUINTAL", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse mKg = new MarketPriceSummaryResponse(
                "p2", "mkt-2", "Market 2", "crop-chilli", "Red Chilli", "2026-08-26",
                180.0, 240.0, 215.0, "INR", "KG", "GOVERNMENT_API", "AGMARKNET", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("KG"), any(), any(), anyInt()
        )).thenReturn(List.of(mQuintal, mKg));

        MarketComparisonResponse result = intelligenceService.compareMarkets("crop-chilli", "2026-08-26", null, null, null, null, "KG");

        assertNotNull(result);
        assertEquals(2, result.getMarkets().size());
        // Rank 1: Market 2 (215 / KG)
        assertEquals("mkt-2", result.getMarkets().get(0).getMarketId());
        assertEquals(215.0, result.getMarkets().get(0).getModalPrice());
        // Rank 2: Market 1 (205 / KG)
        assertEquals("mkt-1", result.getMarkets().get(1).getMarketId());
        assertEquals(205.0, result.getMarkets().get(1).getModalPrice());
    }

    @Test
    void getSummary_calculatesAverageAfterUnitConversion() {
        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-01",
                15000.0, 19000.0, 17000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-26",
                18000.0, 22000.0, 20000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq("mkt-1"), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("KG"), any(), any(), anyInt()
        )).thenReturn(List.of(obs1, obs2));

        MarketIntelligenceSummaryResponse summary = intelligenceService.getSummary("crop-chilli", "mkt-1", "2026-08-01", "2026-08-26", null, null, "KG");

        assertNotNull(summary);
        assertEquals("KG", summary.getUnit());
        assertEquals(2, summary.getObservationCount());
        assertEquals(200.0, summary.getLatestModalPrice());
        assertEquals(185.0, summary.getAverageModalPrice()); // (170 + 200) / 2 = 185
        assertEquals("INCREASING", summary.getTrendDirection());
        assertEquals(30.0, summary.getAbsoluteChange());
        assertEquals(17.64, summary.getPercentageChange(), 0.1);
    }

    @Test
    void getTrends_convertsPointsToRequestedUnit() {
        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-01",
                15000.0, 19000.0, 17000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-26",
                18000.0, 22000.0, 20000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq("mkt-1"), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("KG"), any(), any(), anyInt()
        )).thenReturn(List.of(obs1, obs2));

        PriceTrendResponse trend = intelligenceService.getTrends("crop-chilli", "mkt-1", "2026-08-01", "2026-08-26", "KG");

        assertNotNull(trend);
        assertEquals("KG", trend.getUnit());
        assertEquals(2, trend.getPoints().size());
        assertEquals(170.0, trend.getPoints().get(0).getModalPrice());
        assertEquals(200.0, trend.getPoints().get(1).getModalPrice());
    }

    @Test
    void compareMarkets_emptyCropObservation_returnsEmptyResultsSafely() {
        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-tomato"), any(), any(), any(), eq("VERIFIED"), eq("KG"), any(), any(), anyInt()
        )).thenReturn(Collections.emptyList());

        when(cropMasterService.getCropById("crop-tomato"))
                .thenReturn(new CropResponse("crop-tomato", "Tomato", "VEGETABLE", "Solanum lycopersicum", "ACTIVE"));

        MarketComparisonResponse result = intelligenceService.compareMarkets("crop-tomato", null, null, null, null, null, "KG");

        assertNotNull(result);
        assertTrue(result.getMarkets().isEmpty());
        assertEquals("Tomato", result.getCrop().getName());
    }

    @Test
    void compareMarkets_unsupportedUnit_returnsEmptyResultsSafely() {
        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("UNSUPPORTED_UNIT"), any(), any(), anyInt()
        )).thenReturn(Collections.emptyList());

        MarketComparisonResponse result = intelligenceService.compareMarkets(
                "crop-chilli", null, null, null, null, null, "UNSUPPORTED_UNIT"
        );

        assertNotNull(result);
        assertTrue(result.getMarkets().isEmpty());
        assertNull(result.getHighestMarket());
    }
}
