package com.farmlink.api.service.ai;

import com.farmlink.api.dto.ai.AiDecisionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiDecisionValidatorTest {

    private AiDecisionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AiDecisionValidator();
    }

    @Test
    void testValidateAndSanitize_ReplacesForbiddenSpeculativeClaims() {
        AiDecisionResponse response = new AiDecisionResponse();
        response.setSummary("You should definitely sell today because the price will rise tomorrow!");
        response.setRecommendation("Definitely sell at Warangal Mandi with guaranteed profit!");
        response.setVerifiedFacts(List.of("100% certain return expected"));

        AiDecisionResponse sanitized = validator.validateAndSanitize(response);

        assertNotNull(sanitized);
        assertFalse(sanitized.getSummary().toLowerCase().contains("definitely sell"));
        assertFalse(sanitized.getSummary().toLowerCase().contains("price will rise"));
        assertFalse(sanitized.getRecommendation().toLowerCase().contains("guaranteed profit"));
        assertFalse(sanitized.getRisks().isEmpty());
        assertFalse(sanitized.getLimitations().isEmpty());
    }
}
