package com.farmlink.api.service.ai;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketTrendResponse;

import com.farmlink.api.dto.ai.*;
import com.farmlink.api.service.MarketTrendService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiDecisionTrendTest {

    private AiDecisionContextBuilder contextBuilder;
    private RuleBasedAiDecisionEngine ruleEngine;
    private AiDecisionValidator validator;
    private AiDecisionService aiDecisionService;

    @BeforeEach
    void setUp() {
        contextBuilder = mock(AiDecisionContextBuilder.class);
        ruleEngine = new RuleBasedAiDecisionEngine();
        validator = new AiDecisionValidator();
        aiDecisionService = new AiDecisionService(contextBuilder, ruleEngine, validator);
    }

    @Test
    void processDecisionRequest_marketTrendExplanation_returnsFactualResponseAndHighConfidence() {
        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(true);
        context.setCrop(new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"));

        MarketTrendResponse trend = new MarketTrendResponse();
        trend.setCropId("crop-chilli");
        trend.setCropName("Red Chilli");
        trend.setMarketId("mkt-guntur");
        trend.setMarketName("Guntur Mandi");
        trend.setObservationCount(10);
        trend.setEarliestPrice(18000.0);
        trend.setLatestPrice(21000.0);
        trend.setMinPrice(17500.0);
        trend.setMaxPrice(22000.0);
        trend.setAvgPrice(19500.0);
        trend.setAbsoluteChange(3000.0);
        trend.setPercentageChange(16.67);
        trend.setTrendDirection("RISING");
        trend.setVolatility("LOW");
        trend.setDataQuality("GOOD");
        trend.setFreshnessStatus("FRESH");
        trend.setLatestObservationDate("2026-08-30");
        trend.setCurrentVsAverageStatement("The latest verified price (₹21,000.00 / QUINTAL) is approximately 7.69% above the selected period average (₹19,500.00 / QUINTAL).");

        context.setMarketTrend(trend);

        when(contextBuilder.buildContext(eq("farmer-1"), any())).thenReturn(context);

        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_TREND_EXPLANATION);
        request.setCropId("crop-chilli");

        AiDecisionResponse response = aiDecisionService.processDecisionRequest("farmer-1", request);

        assertNotNull(response);
        assertEquals(AiDecisionType.MARKET_TREND_EXPLANATION, response.getDecisionType());
        assertEquals(AiConfidenceLevel.HIGH, response.getConfidence());
        assertTrue(response.getSummary().contains("RISING"));
        assertTrue(response.getVerifiedFacts().stream().anyMatch(f -> f.contains("Latest verified modal price")));

        // Verify guardrail: No future speculative terms in risks or summary
        assertTrue(response.getRisks().stream().noneMatch(r -> r.toLowerCase().contains("will rise") || r.toLowerCase().contains("guaranteed")));
    }

    @Test
    void processDecisionRequest_staleOrLimitedData_returnsMediumConfidence() {
        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(true);
        context.setCrop(new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"));

        MarketTrendResponse trend = new MarketTrendResponse();
        trend.setObservationCount(3);
        trend.setDataQuality("LIMITED");
        trend.setFreshnessStatus("STALE");
        context.setMarketTrend(trend);

        when(contextBuilder.buildContext(eq("farmer-1"), any())).thenReturn(context);

        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_TREND_EXPLANATION);
        request.setCropId("crop-chilli");

        AiDecisionResponse response = aiDecisionService.processDecisionRequest("farmer-1", request);

        assertNotNull(response);
        assertEquals(AiConfidenceLevel.MEDIUM, response.getConfidence());
    }

    @Test
    void validator_sanitizesForbiddenSpeculativePhrasesInTrendResponse() {
        AiDecisionResponse unsafe = new AiDecisionResponse();
        unsafe.setDecisionType(AiDecisionType.MARKET_TREND_EXPLANATION);
        unsafe.setSummary("The price will rise tomorrow definitely sell now.");
        unsafe.setRecommendation("guaranteed profit if held.");
        unsafe.setVerifiedFacts(List.of("The price will drop soon."));
        unsafe.setReasoning(List.of("100% certain outcome."));

        AiDecisionResponse sanitized = validator.validateAndSanitize(unsafe);

        assertNotNull(sanitized);
        assertFalse(sanitized.getSummary().toLowerCase().contains("price will rise"));
        assertFalse(sanitized.getRecommendation().toLowerCase().contains("guaranteed profit"));
        assertFalse(sanitized.getVerifiedFacts().get(0).toLowerCase().contains("price will drop"));
    }
}
