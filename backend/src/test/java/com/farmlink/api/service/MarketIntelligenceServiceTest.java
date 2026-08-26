package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        when(marketPriceService.getMarketPrices(any(), any(), any(), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), any(), anyInt()))
                .thenReturn(List.of(m1, m2));

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
    void getSummary_calculatesMetricsAndTrendDirectionCorrectly() {
        MarketPriceSummaryResponse obs1 = new MarketPriceSummaryResponse(
                "p1", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-01",
                15000.0, 19000.0, 17000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );
        MarketPriceSummaryResponse obs2 = new MarketPriceSummaryResponse(
                "p2", "mkt-1", "Market 1", "crop-chilli", "Red Chilli", "2026-08-26",
                18000.0, 22000.0, 20000.0, "INR", "QUINTAL", "GOVERNMENT_API", "Source", "VERIFIED", "ACTIVE"
        );

        when(marketPriceService.getMarketPrices(any(), any(), eq("mkt-1"), eq("crop-chilli"), any(), any(), any(), eq("VERIFIED"), any(), anyInt()))
                .thenReturn(List.of(obs1, obs2));

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
