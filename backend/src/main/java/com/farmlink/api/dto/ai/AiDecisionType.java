package com.farmlink.api.dto.ai;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AiDecisionType {
    MARKET_SELECTION,
    PROFITABILITY_EXPLANATION,
    MARKET_COMPARISON_EXPLANATION,
    SELLING_DECISION_SUPPORT,
    MARKET_TREND_EXPLANATION,
    PRICE_FORECAST_EXPLANATION;

    private static final Set<String> VALID_NAMES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public static boolean isValid(String name) {
        if (name == null) return false;
        return VALID_NAMES.contains(name.trim().toUpperCase());
    }

    public static AiDecisionType fromString(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Decision type cannot be null.");
        }
        try {
            return AiDecisionType.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid decision type: " + name + ". Allowed decision types are: " + VALID_NAMES);
        }
    }
}
