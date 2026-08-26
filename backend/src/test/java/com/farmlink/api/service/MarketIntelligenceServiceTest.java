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
    private MarketIntelligenceService intelligenceService;

    @BeforeEach
    void setUp() {
        marketPriceService = mock(MarketPriceService.class);
        marketService = mock(MarketService.class);
        cropMasterService = mock(CropMasterService.class);

        when(cropMasterService.getCropById("crop-chilli"))
                .thenReturn(new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"));

        intelligenceService = new MarketIntelligenceService(marketPriceService, marketService, cropMasterService);
    }

    @Test
    void compareMarkets_calculatesHighestLowestDifferenceAndPercentageCorrectly() {
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
    void compareMarkets_telanganaSiddipet_returnsEmptyResultsSafely_whenNoDataExists() {
        when(marketPriceService.getMarketPrices(
                eq(null), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("KG"), eq("Telangana"), eq("Siddipet"), anyInt()
        )).thenReturn(Collections.emptyList());

        MarketComparisonResponse result = intelligenceService.compareMarkets(
                "crop-chilli", null, null, null, "Telangana", "Siddipet", "KG"
        );

        assertNotNull(result);
        assertTrue(result.getMarkets().isEmpty());
        assertNull(result.getHighestMarket());
        assertNull(result.getLowestMarket());
        assertEquals(0.0, result.getPriceDifference());
        assertNull(result.getPercentageDifference());
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

    @Test
    void compareMarkets_missingCrop_handlesGracefullyWithoutHttp500() {
        when(cropMasterService.getCropById("nonexistent-crop"))
                .thenThrow(new NoSuchElementException("Crop not found"));

        when(marketPriceService.getMarketPrices(
                eq(null), eq("nonexistent-crop"), any(), any(), any(), eq("VERIFIED"), eq("QUINTAL"), any(), any(), anyInt()
        )).thenReturn(Collections.emptyList());

        MarketComparisonResponse result = intelligenceService.compareMarkets(
                "nonexistent-crop", null, null, null, null, null, "QUINTAL"
        );

        assertNotNull(result);
        assertTrue(result.getMarkets().isEmpty());
        assertEquals("nonexistent-crop", result.getCrop().getId());
    }

    @Test
    void compareMarkets_deterministicOrdering_sortsModalPriceDescending() {
        MarketPriceSummaryResponse m1 = new MarketPriceSummaryResponse(
                "p1", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-26",
                10000, 14000, 12000, "INR", "QUINTAL", "API", "Source", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse m2 = new MarketPriceSummaryResponse(
                "p2", "mkt-2", "Market 2", "crop-chilli", "Red Chilli", "2026-08-26",
                20000, 24000, 22000, "INR", "QUINTAL", "API", "Source", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse m3 = new MarketPriceSummaryResponse(
                "p3", "mkt-3", "Market 3", "crop-chilli", "Red Chilli", "2026-08-26",
                15000, 19000, 17000, "INR", "QUINTAL", "API", "Source", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(any(), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("QUINTAL"), any(), any(), anyInt()))
                .thenReturn(List.of(m1, m2, m3));

        MarketComparisonResponse result = intelligenceService.compareMarkets("crop-chilli", null, null, null, null, null, "QUINTAL");

        assertNotNull(result);
        assertEquals(3, result.getMarkets().size());
        // Rank 1: mkt-2 (22000)
        assertEquals("mkt-2", result.getMarkets().get(0).getMarketId());
        // Rank 2: mkt-3 (17000)
        assertEquals("mkt-3", result.getMarkets().get(1).getMarketId());
        // Rank 3: mkt-1 (12000)
        assertEquals("mkt-1", result.getMarkets().get(2).getMarketId());
    }

    @Test
    void getSummary_calculatesMetricsAndTrendDirectionCorrectly() {
        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-01",
                15000.0, 19000.0, 17000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-26",
                18000.0, 22000.0, 20000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(
                eq("mkt-1"), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), eq("QUINTAL"), any(), any(), anyInt()
        )).thenReturn(List.of(obs1, obs2));

        MarketIntelligenceSummaryResponse summary = intelligenceService.getSummary("crop-chilli", "mkt-1", "2026-08-01", "2026-08-26", null, null, "QUINTAL");

        assertNotNull(summary);
        assertEquals(2, summary.getObservationCount());
        assertEquals(20000.0, summary.getLatestModalPrice());
        assertEquals(18500.0, summary.getAverageModalPrice());
        assertEquals("INCREASING", summary.getTrendDirection());
        assertEquals(3000.0, summary.getAbsoluteChange());
        assertNotNull(summary.getPercentageChange());
        assertEquals(17.64, summary.getPercentageChange(), 0.1);
    }
}
