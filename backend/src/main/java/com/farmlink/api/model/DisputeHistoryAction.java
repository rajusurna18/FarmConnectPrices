package com.farmlink.api.model;

/**
 * Controlled enum representing audit history action types for disputes.
 */
public enum DisputeHistoryAction {
    DISPUTE_CREATED,
    RESPONSE_ADDED,
    EVIDENCE_ADDED,
    INFORMATION_REQUESTED,
    INFORMATION_SUBMITTED,
    STATUS_CHANGED,
    DISPUTE_RESOLVED,
    DISPUTE_REJECTED,
    DISPUTE_CLOSED;

    /**
     * Checks if a string matches a valid DisputeHistoryAction.
     */
    public static boolean isValid(String actionStr) {
        if (actionStr == null || actionStr.trim().isEmpty()) {
            return false;
        }
        for (DisputeHistoryAction a : values()) {
            if (a.name().equalsIgnoreCase(actionStr.trim())) {
                return true;
            }
        }
        return false;
    }
}

