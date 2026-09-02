package com.farmlink.api.service.ai;

import com.farmlink.api.dto.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AiDecisionService {
    private static final Logger log = LoggerFactory.getLogger(AiDecisionService.class);

    private final AiDecisionContextBuilder contextBuilder;
    private final AiDecisionEngine decisionEngine;
    private final AiDecisionValidator validator;

    public AiDecisionService(AiDecisionContextBuilder contextBuilder,
                             AiDecisionEngine decisionEngine,
                             AiDecisionValidator validator) {
        this.contextBuilder = contextBuilder;
        this.decisionEngine = decisionEngine;
        this.validator = validator;
    }

    public AiDecisionResponse processDecisionRequest(String farmerUid, AiDecisionRequest request) {
        log.info("Processing AI Decision Request for farmerUid={}, decisionType={}", farmerUid, request.getDecisionType());

        if (request == null || request.getDecisionType() == null) {
            throw new IllegalArgumentException("Decision type is required.");
        }

        // 1. Build trusted context from authoritative backend services
        AiDecisionContext context = contextBuilder.buildContext(farmerUid, request);

        // 2. Generate decision via decision engine (RuleBased or configured AI provider)
        AiDecisionResponse response;
        try {
            response = decisionEngine.generateDecision(request, context);
            if (response == null || !validator.isValidSchema(response)) {
                log.warn("AI Decision Engine returned invalid schema. Triggering RuleBased fallback engine.");
                RuleBasedAiDecisionEngine fallbackEngine = new RuleBasedAiDecisionEngine();
                response = fallbackEngine.generateDecision(request, context);
                response.getLimitations().add("AI provider response failed schema validation; fallback rule engine was utilized.");
            }
        } catch (Exception e) {
            log.error("AI Decision Engine error: {}. Falling back to rule-based evaluation.", e.getMessage());
            RuleBasedAiDecisionEngine fallbackEngine = new RuleBasedAiDecisionEngine();
            response = fallbackEngine.generateDecision(request, context);
            response.getLimitations().add("AI provider execution failed; fallback rule engine was utilized.");
        }

        // 3. Compute deterministic confidence level
        AiConfidenceLevel confidence = calculateConfidence(context, request);
        response.setConfidence(confidence);

        // 4. Validate output and enforce safety guardrails
        return validator.validateAndSanitize(response);
    }

    private AiConfidenceLevel calculateConfidence(AiDecisionContext context, AiDecisionRequest request) {
        // Deterministic confidence logic based on data freshness & completeness
        if (!context.isHasVerifiedPrice()) {
            return AiConfidenceLevel.LOW;
        }

        if (request.getDecisionType() == AiDecisionType.PROFITABILITY_EXPLANATION && context.getProfitabilityEvaluation() == null) {
            return AiConfidenceLevel.LOW;
        }

        if (request.getDecisionType() == AiDecisionType.MARKET_COMPARISON_EXPLANATION 
                && (context.getCandidateMarkets() == null || context.getCandidateMarkets().size() < 2)) {
            return AiConfidenceLevel.LOW;
        }

        if (context.isHasStalePrice()) {
            return AiConfidenceLevel.MEDIUM;
        }

        boolean hasQuantity = context.getQuantity() != null && context.getQuantity().compareTo(BigDecimal.ZERO) > 0;
        boolean hasCrop = context.getCrop() != null;
        boolean hasMarkets = context.getCandidateMarkets() != null && !context.getCandidateMarkets().isEmpty();

        if (hasCrop && hasMarkets && hasQuantity) {
            return AiConfidenceLevel.HIGH;
        }

        return AiConfidenceLevel.MEDIUM;
    }
}
