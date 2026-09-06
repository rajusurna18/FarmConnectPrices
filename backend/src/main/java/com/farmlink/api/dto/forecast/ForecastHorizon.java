package com.farmlink.api.dto.forecast;

public enum ForecastHorizon {
    ONE_DAY("1_DAY", 1),
    THREE_DAYS("3_DAYS", 3),
    SEVEN_DAYS("7_DAYS", 7),
    FOURTEEN_DAYS("14_DAYS", 14);

    private final String code;
    private final int days;

    ForecastHorizon(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String getCode() {
        return code;
    }

    public int getDays() {
        return days;
    }

    public static ForecastHorizon fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return SEVEN_DAYS; // Default horizon
        }
        String clean = code.trim().toUpperCase();
        for (ForecastHorizon h : values()) {
            if (h.code.equalsIgnoreCase(clean) || h.name().equalsIgnoreCase(clean)) {
                return h;
            }
        }
        if ("1".equals(clean) || "1DAY".equals(clean)) return ONE_DAY;
        if ("3".equals(clean) || "3DAYS".equals(clean)) return THREE_DAYS;
        if ("7".equals(clean) || "7DAYS".equals(clean)) return SEVEN_DAYS;
        if ("14".equals(clean) || "14DAYS".equals(clean)) return FOURTEEN_DAYS;

        throw new IllegalArgumentException("Unsupported forecast horizon: " + code + ". Supported horizons: 1_DAY, 3_DAYS, 7_DAYS, 14_DAYS.");
    }
}
