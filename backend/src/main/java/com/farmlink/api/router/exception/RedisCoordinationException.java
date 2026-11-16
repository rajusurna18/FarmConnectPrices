package com.farmlink.api.router.exception;

public class RedisCoordinationException extends RuntimeException {
    public RedisCoordinationException(String message) {
        super(message);
    }

    public RedisCoordinationException(String message, Throwable cause) {
        super(message, cause);
    }
}

