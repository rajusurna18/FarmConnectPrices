package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.SmartSellingDecisionResponse.MarketCard;
import com.farmlink.api.dto.ai.AiDecisionResponse;
import com.farmlink.api.service.ai.AiDecisionService;
import com.farmlink.api.service.forecast.ForecastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class SmartSellingDecisionServiceTest {

    private FarmService farmService;
    private CropMasterService cropMasterService;
    private MarketService marketService;
    private MarketPriceService marketPriceService;
    private ProfitabilityCalculationService profitabilityCalculationService;
    private FarmEconomicsService farmEconomicsService;
    private FarmEconomicsCalculationService farmEconomicsCalculationService;
    private MarketTrendService marketTrendService;
    private ForecastService forecastService;
    private SmartSellingRankingEngine rankingEngine;
    private SmartSellingTradeOffEngine tradeOffEngine;
    private AiDecisionService aiDecisionService;

    private SmartSellingDecisionService smartSellingDecisionService;

    @BeforeEach
    public void setUp() {
        farmService = Mockito.mock(FarmService.class);
        cropMasterService = Mockito.mock(CropMasterService.class);
        marketService = Mockito.mock(MarketService.class);
        marketPriceService = Mockito.mock(MarketPriceService.class);
        profitabilityCalculationService = new ProfitabilityCalculationService();
        farmEconomicsService = Mockito.mock(FarmEconomicsService.class);
        farmEconomicsCalculationService = new FarmEconomicsCalculationService();
        marketTrendService = Mockito.mock(MarketTrendService.class);
        forecastService = Mockito.mock(ForecastService.class);
        rankingEngine = new SmartSellingRankingEngine();
        tradeOffEngine = new SmartSellingTradeOffEngine();
        aiDecisionService = Mockito.mock(AiDecisionService.class);

        smartSellingDecisionService = new SmartSellingDecisionService(
                farmService,
                cropMasterService,
                marketService,
                marketPriceService,
                profitabilityCalculationService,
                farmEconomicsService,
                farmEconomicsCalculationService,
                marketTrendService,
                forecastService,
                rankingEngine,
                tradeOffEngine,
                aiDecisionService
        );

        // Setup Crop Mock
        CropResponse crop = new CropResponse("crop-1", "Paddy", "GRAIN", "Oryza sativa", "ACTIVE");
        when(cropMasterService.getCropById("crop-1")).thenReturn(crop);

        // Setup Markets Mock
        LocationDto loc = new LocationDto("Telangana", "Warangal", "Warangal Rural", "Village");
        MarketResponse m1 = new MarketResponse("market-1", "Warangal Mandi", "M1", "MANDI", loc, 17.9, 79.5, "ACTIVE", "2026-01-01", "2026-01-01");
        MarketResponse m2 = new MarketResponse("market-2", "Khammam Mandi", "M2", "MANDI", loc, 17.2, 80.1, "ACTIVE", "2026-01-01", "2026-01-01");
        when(marketService.getMarketById("market-1")).thenReturn(m1);
        when(marketService.getMarketById("market-2")).thenReturn(m2);

        // Setup AI Service Mock
        AiDecisionResponse aiResp = new AiDecisionResponse();
        aiResp.setSummary("Recommended market offers the highest net realization.");
        aiResp.setRisks(List.of("Market prices fluctuate throughout the day."));
        aiResp.setNextSteps(List.of("Verify current price with local traders."));
        aiResp.setLimitations(List.of("Estimates are based on latest verified market data."));
        when(aiDecisionService.processDecisionRequest(anyString(), any())).thenReturn(aiResp);
    }

    @Test
    public void testRanking_EstimatedNetRealizationPrimary() {
        String ownerUid = "farmer-123";
        SmartSellingDecisionRequest request = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1", "market-2"));
        request.setCustomSellingCosts(new SellingCostsDto(BigDecimal.valueOf(200), BigDecimal.ZERO)); // 200 transport cost

        // Market 1: Price 2500, Transport 200 => Gross 25000, SellingCost 200 => Net Realization = 24800
        MarketPriceResponse p1 = new MarketPriceResponse();
        p1.setId("p1");
        p1.setModalPrice(2500.0);
        p1.setMinPrice(2400.0);
        p1.setMaxPrice(2600.0);
        p1.setUnit("QUINTAL");
        p1.setPriceDate("2026-09-07");
        p1.setQualityStatus("VERIFIED");

        // Market 2: Price 2200, Transport 200 => Gross 22000, SellingCost 200 => Net Realization = 21800
        MarketPriceResponse p2 = new MarketPriceResponse();
        p2.setId("p2");
        p2.setModalPrice(2200.0);
        p2.setMinPrice(2100.0);
        p2.setMaxPrice(2300.0);
        p2.setUnit("QUINTAL");
        p2.setPriceDate("2026-09-07");
        p2.setQualityStatus("VERIFIED");

        when(marketPriceService.getMarketPrices(null, null, "market-1", "crop-1", null, "VERIFIED", null, "priceDate", "desc", 1, 10, 10))
                .thenReturn(List.of(p1));
        when(marketPriceService.getMarketPrices(null, null, "market-2", "crop-1", null, "VERIFIED", null, "priceDate", "desc", 1, 10, 10))
                .thenReturn(List.of(p2));

        SmartSellingDecisionResponse response = smartSellingDecisionService.evaluateSmartSelling(ownerUid, request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        // Market 1 has higher Net Realization (24800 vs 21800), so Market 1 MUST be recommended over Market 2
        assertEquals("market-1", response.getRecommendedMarketId());
        assertEquals("BEST_ESTIMATED_NET_REALIZATION", response.getRecommendationType());

        List<MarketCard> cards = response.getRankedMarkets();
        assertEquals(2, cards.size());
        assertEquals("market-1", cards.get(0).getMarket().getId());
        assertEquals(1, cards.get(0).getRank());
        assertEquals("market-2", cards.get(1).getMarket().getId());
        assertEquals(2, cards.get(1).getRank());
    }

    @Test
    public void testUnsupportedForecastHorizon_ThrowsException() {
        String ownerUid = "farmer-123";
        SmartSellingDecisionRequest request = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1"));
        request.setForecastHorizon("30_DAYS"); // 30_DAYS is not supported by Module 13!

        assertThrows(IllegalArgumentException.class, () -> {
            smartSellingDecisionService.evaluateSmartSelling(ownerUid, request);
        });
    }

    @Test
    public void testCandidateMarkets_StrictEvaluationScope() {
        String ownerUid = "farmer-123";
        SmartSellingDecisionRequest request = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1"));

        MarketPriceResponse p1 = new MarketPriceResponse();
        p1.setId("p1");
        p1.setModalPrice(2000.0);
        p1.setMinPrice(1900.0);
        p1.setMaxPrice(2100.0);
        p1.setUnit("QUINTAL");
        p1.setPriceDate("2026-09-07");
        p1.setQualityStatus("VERIFIED");

        when(marketPriceService.getMarketPrices(null, null, "market-1", "crop-1", null, "VERIFIED", null, "priceDate", "desc", 1, 10, 10))
                .thenReturn(List.of(p1));

        SmartSellingDecisionResponse response = smartSellingDecisionService.evaluateSmartSelling(ownerUid, request);

        assertEquals(1, response.getRankedMarkets().size());
        assertEquals("market-1", response.getRankedMarkets().get(0).getMarket().getId());
    }

    @Test
    public void testDecoupledConfidence_FreshPriceHighConfidence() {
        String ownerUid = "farmer-123";
        SmartSellingDecisionRequest request = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1"));
        request.setCustomSellingCosts(new SellingCostsDto(BigDecimal.valueOf(100), BigDecimal.ZERO));

        MarketPriceResponse p1 = new MarketPriceResponse();
        p1.setId("p1");
        p1.setModalPrice(2000.0);
        p1.setMinPrice(1900.0);
        p1.setMaxPrice(2100.0);
        p1.setUnit("QUINTAL");
        p1.setPriceDate("2026-09-07"); // Today / Fresh
        p1.setQualityStatus("VERIFIED");

        when(marketPriceService.getMarketPrices(null, null, "market-1", "crop-1", null, "VERIFIED", null, "priceDate", "desc", 1, 10, 10))
                .thenReturn(List.of(p1));

        SmartSellingDecisionResponse response = smartSellingDecisionService.evaluateSmartSelling(ownerUid, request);

        assertEquals("HIGH", response.getOverallDecisionConfidence());
        assertEquals("COMPLETE_SELLING_COSTS", response.getEconomicsCompleteness());
    }
}
