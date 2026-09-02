package com.farmlink.api.service.ai;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.ai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedAiDecisionEngineTest {

    private RuleBasedAiDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleBasedAiDecisionEngine();
    }

    @Test
    void testMarketSelection_GeneratesRecommendation() {
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        AiDecisionContext context = new AiDecisionContext();
        CropResponse crop = new CropResponse();
        crop.setId("crop-1");
        crop.setName("Chilli");
        context.setCrop(crop);

        MarketSummaryResponse mkt1 = new MarketSummaryResponse();
        mkt1.setId("m1");
        mkt1.setName("Warangal Mandi");

        MarketSummaryResponse mkt2 = new MarketSummaryResponse();
        mkt2.setId("m2");
        mkt2.setName("Khammam Mandi");

        MarketEvaluationResponse eval1 = new MarketEvaluationResponse();
        eval1.setStatus("SUCCESS");
        eval1.setMarket(mkt1);
        eval1.setSelectedPrice(new BigDecimal("15000.00"));
        eval1.setQuantity(new BigDecimal("10.0"));
        eval1.setQuantityUnit("QUINTAL");
        eval1.setPriceUnit("QUINTAL");
        eval1.setTotalSellingCosts(new BigDecimal("500.00"));
        eval1.setEstimatedNetRealization(new BigDecimal("149500.00"));

        MarketEvaluationResponse eval2 = new MarketEvaluationResponse();
        eval2.setStatus("SUCCESS");
        eval2.setMarket(mkt2);
        eval2.setSelectedPrice(new BigDecimal("14200.00"));
        eval2.setQuantity(new BigDecimal("10.0"));
        eval2.setQuantityUnit("QUINTAL");
        eval2.setPriceUnit("QUINTAL");
        eval2.setTotalSellingCosts(new BigDecimal("400.00"));
        eval2.setEstimatedNetRealization(new BigDecimal("141600.00"));

        context.setMarketEvaluations(List.of(eval1, eval2));
        context.setHasVerifiedPrice(true);
        context.setPriceDate("2026-09-02");

        AiDecisionResponse response = engine.generateDecision(request, context);

        assertNotNull(response);
        assertEquals(AiDecisionType.MARKET_SELECTION, response.getDecisionType());
        assertEquals("Warangal Mandi", response.getRecommendation());
        assertNotNull(response.getCalculatedMetrics());
        assertEquals(new BigDecimal("149500.00"), response.getCalculatedMetrics().getEstimatedNetRealization());
        assertFalse(response.getVerifiedFacts().isEmpty());
        assertFalse(response.getReasoning().isEmpty());
    }

    @Test
    void testProfitabilityExplanation_ExplainsMetrics() {
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.PROFITABILITY_EXPLANATION);

        AiDecisionContext context = new AiDecisionContext();
        FarmProfitabilityEvaluationResponse prof = new FarmProfitabilityEvaluationResponse();
        prof.setStatus("SUCCESS");
        CropResponse crop = new CropResponse();
        crop.setName("Cotton");
        prof.setCrop(crop);
        prof.setExpectedYield(new BigDecimal("25.0"));
        prof.setYieldUnit("QUINTAL");
        prof.setTotalProductionCost(new BigDecimal("60000.00"));
        prof.setTotalSellingCost(new BigDecimal("2500.00"));
        prof.setTotalCost(new BigDecimal("62500.00"));
        prof.setGrossRevenue(new BigDecimal("175000.00"));
        prof.setEstimatedNetRealization(new BigDecimal("172500.00"));
        prof.setEstimatedProfit(new BigDecimal("112500.00"));
        prof.setProfitPerUnit(new BigDecimal("4500.00"));
        prof.setBreakEvenSellingPrice(new BigDecimal("2500.00"));
        prof.setRoi(new BigDecimal("180.00"));
        prof.setProfitabilityStatus("PROFITABLE");

        context.setProfitabilityEvaluation(prof);

        AiDecisionResponse response = engine.generateDecision(request, context);

        assertNotNull(response);
        assertEquals(AiDecisionType.PROFITABILITY_EXPLANATION, response.getDecisionType());
        assertTrue(response.getSummary().contains("PROFITABLE"));
        assertNotNull(response.getCalculatedMetrics());
        assertEquals(new BigDecimal("112500.00"), response.getCalculatedMetrics().getEstimatedProfit());
        assertEquals(new BigDecimal("2500.00"), response.getCalculatedMetrics().getBreakEvenSellingPrice());
    }
}
