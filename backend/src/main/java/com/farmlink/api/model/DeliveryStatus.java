package com.farmlink.api.model;

/**
 * Controlled state machine statuses for Module 19 Delivery & Logistics Foundation.
 */
public enum DeliveryStatus {
    CREATED,
    ASSIGNED,
    READY_FOR_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
