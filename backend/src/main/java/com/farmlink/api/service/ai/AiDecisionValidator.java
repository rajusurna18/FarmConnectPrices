package com.farmlink.api.service.ai;

import com.farmlink.api.dto.ai.AiConfidenceLevel;
import com.farmlink.api.dto.ai.AiDecisionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class AiDecisionValidator {
    private static final Logger log = LoggerFactory.getLogger(AiDecisionValidator.class);

    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("(?i)definitely\\s+sell"),
            Pattern.compile("(?i)guaranteed\\s+profit"),
            Pattern.compile("(?i)price\\s+will\\s+rise"),
            Pattern.compile("(?i)price\\s+will\\s+drop"),
            Pattern.compile("(?i)price\\s+will\\s+fall"),
            Pattern.compile("(?i)price\\s+will\\s+reach"),
            Pattern.compile("(?i)guaranteed\\s+price"),
            Pattern.compile("(?i)will\\s+increase\\s+tomorrow"),
            Pattern.compile("(?i)guaranteed\\s+to\\s+give"),
            Pattern.compile("(?i)100%\\s+certain")
    );

    private static final String MANDATORY_DISCLAIMER = "Estimates are based on latest verified market data and farmer-entered costs. Actual market prices and logistics costs may vary at the time of transaction.";

    public AiDecisionResponse validateAndSanitize(AiDecisionResponse response) {
        if (response == null) {
            response = new AiDecisionResponse();
            response.setSummary("Unable to process decision support request.");
            response.setConfidence(AiConfidenceLevel.LOW);
            return response;
        }

        // 1. Sanitize text fields against forbidden speculative claims
        response.setSummary(sanitizeString(response.getSummary()));
        response.setRecommendation(sanitizeString(response.getRecommendation()));

        List<String> sanitizedFacts = sanitizeList(response.getVerifiedFacts());
        response.setVerifiedFacts(sanitizedFacts);

        List<String> sanitizedReasoning = sanitizeList(response.getReasoning());
        response.setReasoning(sanitizedReasoning);

        List<String> sanitizedRisks = sanitizeList(response.getRisks());
        // Ensure standard risk statement is included
        if (sanitizedRisks.stream().noneMatch(r -> r.toLowerCase().contains("price can change"))) {
            sanitizedRisks.add("Market prices fluctuate throughout the day and can change before sale execution.");
        }
        response.setRisks(sanitizedRisks);

        List<String> sanitizedNextSteps = sanitizeList(response.getNextSteps());
        if (sanitizedNextSteps.stream().noneMatch(s -> s.toLowerCase().contains("verify"))) {
            sanitizedNextSteps.add("Verify current price with local mandi traders before finalizing transport.");
        }
        response.setNextSteps(sanitizedNextSteps);

        // 2. Ensure limitations list includes standard disclaimers
        List<String> limitations = response.getLimitations() != null ? new ArrayList<>(response.getLimitations()) : new ArrayList<>();
        if (limitations.stream().noneMatch(l -> l.contains("Estimates are based on"))) {
            limitations.add(MANDATORY_DISCLAIMER);
        }
        response.setLimitations(limitations);

        return response;
    }

    public boolean isValidSchema(AiDecisionResponse response) {
        if (response == null) return false;
        if (response.getDecisionType() == null) return false;
        if (response.getSummary() == null || response.getSummary().isBlank()) return false;
        if (response.getVerifiedFacts() == null) return false;
        if (response.getReasoning() == null) return false;
        if (response.getRisks() == null) return false;
        if (response.getNextSteps() == null) return false;
        return true;
    }

    private String sanitizeString(String text) {
        if (text == null) return "";
        String result = text;
        for (Pattern p : FORBIDDEN_PATTERNS) {
            if (p.matcher(result).find()) {
                log.warn("Guardrail triggered: removing forbidden speculative phrase from text: '{}'", text);
                result = p.matcher(result).replaceAll("currently appears to");
            }
        }
        return result;
    }

    private List<String> sanitizeList(List<String> items) {
        if (items == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                result.add(sanitizeString(item));
            }
        }
        return result;
    }
}
