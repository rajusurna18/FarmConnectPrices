package com.farmlink.api.dto.forecast;

public enum ForecastModelType {
    WEIGHTED_MOVING_AVERAGE_V1("WEIGHTED_MOVING_AVERAGE_V1", "Weighted Moving Average Baseline (V1)"),
    SIMPLE_MOVING_AVERAGE_V1("SIMPLE_MOVING_AVERAGE_V1", "Simple Moving Average Baseline (V1)");

    private final String code;
    private final String description;

    ForecastModelType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ForecastModelType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return WEIGHTED_MOVING_AVERAGE_V1;
        }
        String clean = code.trim().toUpperCase();
        for (ForecastModelType t : values()) {
            if (t.code.equalsIgnoreCase(clean) || t.name().equalsIgnoreCase(clean)) {
                return t;
            }
        }
        return WEIGHTED_MOVING_AVERAGE_V1;
    }
}
