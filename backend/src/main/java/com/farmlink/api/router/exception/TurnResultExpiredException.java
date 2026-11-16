package com.farmlink.api.router.exception;

public class TurnResultExpiredException extends RuntimeException {
    public TurnResultExpiredException(String message) {
        super(message);
    }
}

