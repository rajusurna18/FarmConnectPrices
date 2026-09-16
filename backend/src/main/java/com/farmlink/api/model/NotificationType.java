package com.farmlink.api.model;

/**
 * Controlled enum representing supported in-app notification types.
 */
public enum NotificationType {
    // Offers (Module 16)
    NEW_OFFER,
    COUNTER_OFFER_RECEIVED,
    OFFER_ACCEPTED,
    OFFER_REJECTED,
    OFFER_CANCELLED,

    // Orders (Module 17)
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_PROCESSING,
    ORDER_CANCELLED,
    ORDER_COMPLETED,

    // Payments (Module 18)
    PAYMENT_CREATED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,

    // Delivery & Logistics (Module 19)
    DELIVERY_ASSIGNED,
    DELIVERY_READY_FOR_PICKUP,
    DELIVERY_PICKED_UP,
    DELIVERY_IN_TRANSIT,
    DELIVERY_OUT_FOR_DELIVERY,
    DELIVERY_DELIVERED,
    DELIVERY_CANCELLED,

    // Reviews (Module 20)
    NEW_REVIEW
}
