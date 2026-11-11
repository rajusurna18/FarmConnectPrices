package com.farmlink.api.model;

import java.util.Set;

/**
 * Controlled enum representing the status lifecycle of a dispute.
 * Active states: OPEN, UNDER_REVIEW, WAITING_FOR_INFORMATION.
 * Resolution outcomes: RESOLVED, REJECTED (can transition to final CLOSED state).
 * Terminal state: CLOSED.
 */
public enum DisputeStatus {
    OPEN,
    UNDER_REVIEW,
    WAITING_FOR_INFORMATION,
    RESOLVED,
    REJECTED,
    CLOSED;

    private static final Set<DisputeStatus> TERMINAL_STATUSES = Set.of(CLOSED);
    private static final Set<DisputeStatus> ACTIVE_STATUSES = Set.of(OPEN, UNDER_REVIEW, WAITING_FOR_INFORMATION);

    /**
     * Checks if this status is terminal (cannot transition further).
     */
    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }

    /**
     * Checks if this status is active (open for responses and information exchange).
     */
    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }

    /**
     * Enforces valid status transitions according to the Module 24 lifecycle.
     */
    public boolean canTransitionTo(DisputeStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case OPEN -> target == UNDER_REVIEW
                    || target == WAITING_FOR_INFORMATION
                    || target == RESOLVED
                    || target == REJECTED
                    || target == CLOSED;

            case UNDER_REVIEW -> target == WAITING_FOR_INFORMATION
                    || target == RESOLVED
                    || target == REJECTED
                    || target == CLOSED;

            case WAITING_FOR_INFORMATION -> target == UNDER_REVIEW
                    || target == RESOLVED
                    || target == REJECTED
                    || target == CLOSED;

            case RESOLVED, REJECTED -> target == CLOSED;

            case CLOSED -> false;
        };
    }

    /**
     * Checks if a string matches a valid DisputeStatus.
     */
    public static boolean isValid(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return false;
        }
        for (DisputeStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely parse a DisputeStatus from string, returning null if invalid.
     */
    public static DisputeStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }
        for (DisputeStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        return null;
    }
}
