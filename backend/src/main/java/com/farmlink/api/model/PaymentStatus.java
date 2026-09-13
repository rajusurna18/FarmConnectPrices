package com.farmlink.api.model;

/**
 * Controlled state machine statuses for Module 18 Payments Foundation.
 */
public enum PaymentStatus {
    CREATED,
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED
}
