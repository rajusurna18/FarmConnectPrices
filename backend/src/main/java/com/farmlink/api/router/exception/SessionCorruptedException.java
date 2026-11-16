package com.farmlink.api.router.exception;

public class SessionCorruptedException extends RuntimeException {
    public SessionCorruptedException(String message) {
        super(message);
    }
}

