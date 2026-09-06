package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.ai.*;
import com.farmlink.api.dto.forecast.ForecastConfidence;
import com.farmlink.api.dto.forecast.ForecastDirection;
import com.farmlink.api.dto.forecast.ForecastResponse;
import com.farmlink.api.service.ai.RuleBasedAiDecisionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiDecisionForecastTest {

    private RuleBasedAiDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleBasedAiDecisionEngine();
    }

    @Test
    void testGenerateDecision_PriceForecastExplanation_GeneratesGuardrailedResponse() {
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.PRICE_FORECAST_EXPLANATION);
        request.setCropId("crop-1");
        request.setMarketIds(List.of("mkt-1"));

        ForecastResponse fc = new ForecastResponse();
        fc.setCropId("crop-1");
        fc.setCropName("Tomato");
        fc.setMarketId("mkt-1");
        fc.setMarketName("Kolar Mandi");
        fc.setModel("WEIGHTED_MOVING_AVERAGE_V1");
        fc.setHorizon("7_DAYS");
        fc.setCurrentVerifiedPrice(2400.0);
        fc.setForecastPrice(2475.0);
        fc.setForecastLowerBound(2280.0);
        fc.setForecastUpperBound(2670.0);
        fc.setConfidence(ForecastConfidence.MEDIUM);
        fc.setDirection(ForecastDirection.UP);
        fc.setDataQuality("GOOD");
        fc.setObservationsUsed(28);
        fc.setLatestObservationDate("2026-09-05");
        fc.setUnit("QUINTAL");

        AiDecisionContext context = new AiDecisionContext();
        context.setPriceForecast(fc);

        AiDecisionResponse response = engine.generateDecision(request, context);

        assertNotNull(response);
        assertEquals(AiDecisionType.PRICE_FORECAST_EXPLANATION, response.getDecisionType());
        assertNotNull(response.getSummary());
        assertTrue(response.getSummary().contains("₹2,475.00"), "Summary should contain formatted forecast price");
        assertTrue(response.getSummary().contains("estimates"), "Summary must use guardrailed estimation terms");

        // Verify guardrails: summary must not contain certainty words
        String fullText = (response.getSummary() + " " + response.getRecommendation()).toLowerCase();
        assertFalse(fullText.contains("guaranteed"), "AI output must never claim guaranteed prices");
        assertFalse(fullText.contains("definitely"), "AI output must never claim definite prices");
    }
}
