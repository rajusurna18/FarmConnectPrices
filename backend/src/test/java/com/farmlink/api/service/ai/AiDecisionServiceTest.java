package com.farmlink.api.service.ai;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketResponse;
import com.farmlink.api.dto.ai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AiDecisionServiceTest {

    @Mock
    private AiDecisionContextBuilder contextBuilder;

    @Mock
    private AiDecisionEngine decisionEngine;

    private AiDecisionValidator validator;
    private AiDecisionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new AiDecisionValidator();
        service = new AiDecisionService(contextBuilder, decisionEngine, validator);
    }

    @Test
    void testProcessDecisionRequest_CalculatesConfidenceAndSanitizes() {
        String farmerUid = "farmer-1";
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(true);
        context.setHasStalePrice(false);
        context.setQuantity(new BigDecimal("10.0"));

        CropResponse crop = new CropResponse();
        crop.setId("crop-1");
        context.setCrop(crop);

        MarketResponse mkt = new MarketResponse();
        mkt.setId("m1");
        context.setCandidateMarkets(List.of(mkt));

        when(contextBuilder.buildContext(eq(farmerUid), any())).thenReturn(context);

        AiDecisionResponse rawResponse = new AiDecisionResponse();
        rawResponse.setDecisionType(AiDecisionType.MARKET_SELECTION);
        rawResponse.setSummary("Market 1 is recommended.");
        when(decisionEngine.generateDecision(any(), any())).thenReturn(rawResponse);

        AiDecisionResponse result = service.processDecisionRequest(farmerUid, request);

        assertNotNull(result);
        assertEquals(AiConfidenceLevel.HIGH, result.getConfidence());
        assertNotNull(result.getSummary());
        assertFalse(result.getLimitations().isEmpty());
    }

    @Test
    void testConfidenceLevel_LowWhenMissingVerifiedPrice() {
        String farmerUid = "farmer-1";
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(false); // missing price

        when(contextBuilder.buildContext(eq(farmerUid), any())).thenReturn(context);

        AiDecisionResponse rawResponse = new AiDecisionResponse();
        rawResponse.setDecisionType(AiDecisionType.MARKET_SELECTION);
        rawResponse.setSummary("No price available");
        when(decisionEngine.generateDecision(any(), any())).thenReturn(rawResponse);

        AiDecisionResponse result = service.processDecisionRequest(farmerUid, request);
        assertEquals(AiConfidenceLevel.LOW, result.getConfidence());
    }

    @Test
    void testConfidenceLevel_MediumWhenStalePrice() {
        String farmerUid = "farmer-1";
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(true);
        context.setHasStalePrice(true); // stale price

        when(contextBuilder.buildContext(eq(farmerUid), any())).thenReturn(context);

        AiDecisionResponse rawResponse = new AiDecisionResponse();
        rawResponse.setDecisionType(AiDecisionType.MARKET_SELECTION);
        rawResponse.setSummary("Stale price recommendation");
        when(decisionEngine.generateDecision(any(), any())).thenReturn(rawResponse);

        AiDecisionResponse result = service.processDecisionRequest(farmerUid, request);
        assertEquals(AiConfidenceLevel.MEDIUM, result.getConfidence());
    }

    @Test
    void testConfidenceLevel_LowWhenMissingRequiredEconomicsRecordForProfitability() {
        String farmerUid = "farmer-1";
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.PROFITABILITY_EXPLANATION);

        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(true);
        context.setProfitabilityEvaluation(null); // missing profitability evaluation

        when(contextBuilder.buildContext(eq(farmerUid), any())).thenReturn(context);

        AiDecisionResponse rawResponse = new AiDecisionResponse();
        rawResponse.setDecisionType(AiDecisionType.PROFITABILITY_EXPLANATION);
        rawResponse.setSummary("Profitability requested without record");
        when(decisionEngine.generateDecision(any(), any())).thenReturn(rawResponse);

        AiDecisionResponse result = service.processDecisionRequest(farmerUid, request);
        assertEquals(AiConfidenceLevel.LOW, result.getConfidence());
    }

    @Test
    void testInvalidSchemaTriggersFallbackToRuleBasedEngine() {
        String farmerUid = "farmer-1";
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        AiDecisionContext context = new AiDecisionContext();
        context.setHasVerifiedPrice(true);
        when(contextBuilder.buildContext(eq(farmerUid), any())).thenReturn(context);

        // Invalid response missing summary and required arrays
        AiDecisionResponse invalidResponse = new AiDecisionResponse();
        invalidResponse.setSummary(null);
        when(decisionEngine.generateDecision(any(), any())).thenReturn(invalidResponse);

        AiDecisionResponse result = service.processDecisionRequest(farmerUid, request);
        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertTrue(result.getLimitations().stream().anyMatch(l -> l.contains("fallback rule engine")));
    }
}
