package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DecisionSupportServiceTest {

    private MarketPriceService marketPriceService;
    private MarketService marketService;
    private CropMasterService cropMasterService;
    private ProfitabilityCalculationService calculationService;
    private DecisionSupportService decisionSupportService;

    @BeforeEach
    void setUp() {
        marketPriceService = Mockito.mock(MarketPriceService.class);
        marketService = Mockito.mock(MarketService.class);
        cropMasterService = Mockito.mock(CropMasterService.class);
        calculationService = new ProfitabilityCalculationService();

        decisionSupportService = new DecisionSupportService(
                marketPriceService, marketService, cropMasterService, calculationService
        );

        // Default mock setups
        CropResponse mockCrop = new CropResponse("CROP1", "Rice", "CEREAL", "RICE", "ACTIVE");
        when(cropMasterService.getCropById("CROP1")).thenReturn(mockCrop);

        LocationDto loc = new LocationDto();
        loc.setState("Telangana");
        loc.setDistrict("Warangal");
        MarketResponse mockMarket1 = new MarketResponse("MKT1", "Warangal Mandi", "WGL", "MANDI", loc, 17.97, 79.59, "ACTIVE", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        MarketResponse mockMarket2 = new MarketResponse("MKT2", "Khammam Mandi", "KHM", "MANDI", loc, 17.24, 80.15, "ACTIVE", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        when(marketService.getMarketById("MKT1")).thenReturn(mockMarket1);
        when(marketService.getMarketById("MKT2")).thenReturn(mockMarket2);
    }

    @Test
    @DisplayName("Evaluate single market successfully with verified price and MODAL price basis")
    void testEvaluateMarketSuccess() {
        MarketPriceResponse mockPrice = createMockPrice("MKT1", "CROP1", "2800", "QUINTAL", "2026-08-31", "VERIFIED");
        when(marketPriceService.getMarketPrices(any(), any(), eq("MKT1"), eq("CROP1"), any(), eq("VERIFIED"), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockPrice));

        MarketEvaluationRequest req = new MarketEvaluationRequest(
                "CROP1", "MKT1", 10.0, "QUINTAL", "MODAL", "LATEST_AVAILABLE", null, 300.0, 100.0
        );

        MarketEvaluationResponse res = decisionSupportService.evaluateMarket(req);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals(new BigDecimal("2800.00"), res.getSelectedPrice());
        assertEquals(new BigDecimal("28000.00"), res.getGrossRevenue());
        assertEquals(new BigDecimal("400.00"), res.getTotalSellingCosts());
        assertEquals(new BigDecimal("27600.00"), res.getEstimatedNetRealization());
        assertEquals(new BigDecimal("2760.00"), res.getNetRealizationPerUnit());
        assertEquals(new BigDecimal("40.00"), res.getSellingCostBreakEvenPrice());
        assertFalse(res.isLoss());
    }

    @Test
    @DisplayName("Unit mismatch handling: Quantity unit KG vs Price unit QUINTAL")
    void testUnitMismatch() {
        MarketPriceResponse mockPrice = createMockPrice("MKT1", "CROP1", "2800", "QUINTAL", "2026-08-31", "VERIFIED");
        when(marketPriceService.getMarketPrices(any(), any(), eq("MKT1"), eq("CROP1"), any(), eq("VERIFIED"), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockPrice));

        MarketEvaluationRequest req = new MarketEvaluationRequest(
                "CROP1", "MKT1", 100.0, "KG", "MODAL", "LATEST_AVAILABLE", null, 50.0, 10.0
        );

        MarketEvaluationResponse res = decisionSupportService.evaluateMarket(req);

        assertEquals("UNIT_MISMATCH", res.getStatus());
        assertEquals("Price and quantity units are incompatible.", res.getMessage());
    }

    @Test
    @DisplayName("No verified price available returns structured NO_VERIFIED_PRICE error")
    void testNoVerifiedPrice() {
        when(marketPriceService.getMarketPrices(any(), any(), eq("MKT1"), eq("CROP1"), any(), eq("VERIFIED"), any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        MarketEvaluationRequest req = new MarketEvaluationRequest(
                "CROP1", "MKT1", 10.0, "QUINTAL", "MODAL", "LATEST_AVAILABLE", null, 300.0, 100.0
        );

        MarketEvaluationResponse res = decisionSupportService.evaluateMarket(req);

        assertEquals("NO_VERIFIED_PRICE", res.getStatus());
        assertTrue(res.getMessage().contains("No verified price is available"));
    }

    @Test
    @DisplayName("Exact date mode looks up price strictly for requested date")
    void testExactDateMode() {
        MarketPriceResponse mockPrice = createMockPrice("MKT1", "CROP1", "2900", "QUINTAL", "2026-08-30", "VERIFIED");
        when(marketPriceService.getMarketPrices(any(), any(), eq("MKT1"), eq("CROP1"), eq("2026-08-30"), eq("VERIFIED"), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockPrice));

        MarketEvaluationRequest req = new MarketEvaluationRequest(
                "CROP1", "MKT1", 10.0, "QUINTAL", "MODAL", "EXACT_DATE", "2026-08-30", 300.0, 100.0
        );

        MarketEvaluationResponse res = decisionSupportService.evaluateMarket(req);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals("2026-08-30", res.getPriceDate());
        assertEquals(new BigDecimal("2900.00"), res.getSelectedPrice());
    }

    @Test
    @DisplayName("Multi-market comparison ranks markets by estimatedNetRealization DESC")
    void testCompareMarketsRanking() {
        // Market 1: Price 2800, Transport 300, Other 100 => Net 27600
        MarketPriceResponse mockPrice1 = createMockPrice("MKT1", "CROP1", "2800", "QUINTAL", "2026-08-31", "VERIFIED");
        when(marketPriceService.getMarketPrices(any(), any(), eq("MKT1"), eq("CROP1"), any(), eq("VERIFIED"), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockPrice1));

        // Market 2: Price 2750, Transport 100, Other 50 => Net 27350
        MarketPriceResponse mockPrice2 = createMockPrice("MKT2", "CROP1", "2750", "QUINTAL", "2026-08-31", "VERIFIED");
        when(marketPriceService.getMarketPrices(any(), any(), eq("MKT2"), eq("CROP1"), any(), eq("VERIFIED"), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockPrice2));

        MarketComparisonRequest req = new MarketComparisonRequest(
                "CROP1", 10.0, "QUINTAL", "MODAL", "LATEST_AVAILABLE", null,
                List.of(
                        new MarketCostInputDto("MKT1", 300.0, 100.0),
                        new MarketCostInputDto("MKT2", 100.0, 50.0)
                )
        );

        MarketProfitabilityComparisonResponse res = decisionSupportService.compareMarkets(req);

        assertNotNull(res.getTopRealizationMarket());
        assertEquals("MKT1", res.getTopRealizationMarket().getMarket().getId());
        assertEquals(2, res.getEvaluations().size());
        assertEquals("MKT1", res.getEvaluations().get(0).getMarket().getId());
        assertEquals("MKT2", res.getEvaluations().get(1).getMarket().getId());
        assertTrue(res.getRankingSummary().contains("Warangal Mandi"));
    }

    private MarketPriceResponse createMockPrice(String marketId, String cropId, String priceVal, String unit, String priceDate, String quality) {
        double p = Double.parseDouble(priceVal);
        MarketSummaryResponse mSummary = new MarketSummaryResponse(marketId, "Mandi " + marketId, "CODE", "MANDI", "Telangana", "Warangal", null, "ACTIVE", 0);
        CropResponse crop = new CropResponse(cropId, "Rice", "CEREAL", "RICE", "ACTIVE");
        MarketPriceSourceDto src = new MarketPriceSourceDto("AGMARKNET", "Agmarknet Gov", "REF123");

        return new MarketPriceResponse(
                "PRC1", mSummary, crop, priceDate, "2026-08-31T10:00:00Z",
                p - 100, p + 100, p, "INR", unit, src, quality, "ACTIVE", "2026-08-31T10:00:00Z", "2026-08-31T10:00:00Z"
        );
    }
}
