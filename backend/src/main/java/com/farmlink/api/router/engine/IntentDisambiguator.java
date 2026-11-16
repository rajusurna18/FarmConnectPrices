package com.farmlink.api.router.engine;

import com.farmlink.api.router.dto.ClarificationDetails;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.entity.AgriIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IntentDisambiguator {

    public static final double TIE_THRESHOLD = 0.05;
    public static final double MIN_CONFIDENCE_THRESHOLD = 0.35;

    public record DisambiguationResult(
            AgriIntent selectedIntent,
            double confidence,
            ClarificationDetails clarification
    ) {}

    public DisambiguationResult disambiguate(List<RuleBasedClassifier.CandidateScore> scores, ExtractedEntitiesDto entities) {
        if (scores == null || scores.isEmpty()) {
            return new DisambiguationResult(
                    AgriIntent.UNSUPPORTED_OR_UNCLEAR,
                    0.0,
                    ClarificationDetails.notRequired()
            );
        }

        RuleBasedClassifier.CandidateScore top = scores.get(0);

        // Low confidence fallback
        if (top.score() < MIN_CONFIDENCE_THRESHOLD) {
            return new DisambiguationResult(
                    AgriIntent.UNSUPPORTED_OR_UNCLEAR,
                    top.score(),
                    ClarificationDetails.of(
                            "Could you please rephrase or provide more details about your agricultural question?",
                            List.of(AgriIntent.UNSUPPORTED_OR_UNCLEAR),
                            List.of()
                    )
            );
        }

        // Check tie / ambiguity with 2nd candidate
        if (scores.size() > 1) {
            RuleBasedClassifier.CandidateScore second = scores.get(1);
            if (second.score() >= 0.40 && Math.abs(top.score() - second.score()) <= TIE_THRESHOLD) {
                // Ambiguity between top two intents
                String prompt = String.format(
                        "Your question could be related to %s or %s. Please specify which aspect you need assistance with.",
                        formatIntentName(top.intent()),
                        formatIntentName(second.intent())
                );
                return new DisambiguationResult(
                        top.intent(),
                        top.score(),
                        ClarificationDetails.of(prompt, List.of(top.intent(), second.intent()), List.of())
                );
            }
        }

        // Check required entities for specific intents
        List<String> missingEntities = new ArrayList<>();
        if ((top.intent() == AgriIntent.MARKET_PRICE_INQUIRY || top.intent() == AgriIntent.MARKET_TREND_INQUIRY)
                && entities.crops().isEmpty()) {
            missingEntities.add("crop");
        }
        if (top.intent() == AgriIntent.WEATHER_INQUIRY && entities.locations().isEmpty()) {
            missingEntities.add("location");
        }

        if (!missingEntities.isEmpty()) {
            String prompt = String.format("Please specify the %s for your query.", String.join(" and ", missingEntities));
            return new DisambiguationResult(
                    top.intent(),
                    top.score(),
                    ClarificationDetails.of(prompt, List.of(top.intent()), missingEntities)
            );
        }

        return new DisambiguationResult(
                top.intent(),
                top.score(),
                ClarificationDetails.notRequired()
        );
    }

    private String formatIntentName(AgriIntent intent) {
        return intent.name().toLowerCase().replace('_', ' ');
    }
}

