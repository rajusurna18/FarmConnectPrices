package com.farmlink.api.router.entity;

/**
 * Confidence band for router classification.
 */
public enum ConfidenceBand {
    HIGH,
    MEDIUM,
    LOW;

    public static ConfidenceBand fromScore(double score) {
        if (score >= 0.75) {
            return HIGH;
        } else if (score >= 0.50) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}

