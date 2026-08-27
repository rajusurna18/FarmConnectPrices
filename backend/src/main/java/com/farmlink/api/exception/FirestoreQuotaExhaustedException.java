package com.farmlink.api.exception;

public class FirestoreQuotaExhaustedException extends RuntimeException {

    public FirestoreQuotaExhaustedException(String message) {
        super(message);
    }

    public FirestoreQuotaExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
