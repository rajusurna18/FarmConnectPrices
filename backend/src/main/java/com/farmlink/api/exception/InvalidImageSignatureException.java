package com.farmlink.api.exception;

/**
 * Thrown when bounded server-side image signature verification fails.
 * Signals that the initial file header bytes do not match the expected magic signature
 * for the declared container format (JPEG, PNG, WebP).
 * Does not perform full image decoding or pixel validation.
 */
public class InvalidImageSignatureException extends IllegalArgumentException {
    public InvalidImageSignatureException(String message) {
        super(message);
    }
}

