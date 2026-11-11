package com.farmlink.api.model;

/**
 * Controlled enum representing dispute resolution outcomes.
 * Note: Adjudication endpoints are not exposed to ordinary participants; this enum serves as a domain model foundation.
 */
public enum DisputeResolutionOutcome {
    ISSUE_CONFIRMED,
    ISSUE_NOT_CONFIRMED,
    PARTIAL_RESOLUTION,
    NO_ACTION_REQUIRED,
    OTHER;

    /**
     * Checks if a string matches a valid DisputeResolutionOutcome.
     */
    public static boolean isValid(String outcomeStr) {
        if (outcomeStr == null || outcomeStr.trim().isEmpty()) {
            return false;
        }
        for (DisputeResolutionOutcome o : values()) {
            if (o.name().equalsIgnoreCase(outcomeStr.trim())) {
                return true;
            }
        }
        return false;
    }
}

