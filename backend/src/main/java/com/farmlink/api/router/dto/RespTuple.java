package com.farmlink.api.router.dto;

import java.util.List;

/**
 * Three-element RESP response tuple: [status, payloadOrNONE, errorOrNONE]
 */
public record RespTuple(String status, String payload, String error) {

    public static final String NONE = "NONE";

    public static RespTuple fromList(List<?> list) {
        if (list == null || list.size() < 3) {
            throw new IllegalArgumentException("RESP tuple must contain at least 3 elements");
        }
        String status = list.get(0) != null ? list.get(0).toString() : NONE;
        String payload = list.get(1) != null ? list.get(1).toString() : NONE;
        String error = list.get(2) != null ? list.get(2).toString() : NONE;
        return new RespTuple(status, payload, error);
    }

    public boolean isCommitted() {
        return "COMMITTED".equals(status);
    }

    public boolean isLockAcquired() {
        return "LOCK_ACQUIRED".equals(status);
    }

    public boolean isResultCached() {
        return "RESULT_CACHED".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    public boolean isConflict() {
        return "CONFLICT".equals(status);
    }

    public boolean isError() {
        return "ERROR".equals(status);
    }

    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    public boolean isCompletedWarning() {
        return "COMPLETED_WARNING".equals(status);
    }
}

