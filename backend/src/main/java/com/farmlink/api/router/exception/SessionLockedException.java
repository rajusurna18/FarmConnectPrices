package com.farmlink.api.router.exception;

public class SessionLockedException extends RuntimeException {
    public SessionLockedException(String message) {
        super(message);
    }
}

