package com.farmlink.api.model;

/**
 * Controlled enum representing supported in-app notification preference categories.
 */
public enum NotificationCategory {
    OFFERS,
    ORDERS,
    PAYMENTS,
    DELIVERY,
    REVIEWS;

    /**
     * Maps a NotificationType to its corresponding NotificationCategory.
     */
    public static NotificationCategory fromNotificationType(NotificationType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case NEW_OFFER, COUNTER_OFFER_RECEIVED, OFFER_ACCEPTED, OFFER_REJECTED, OFFER_CANCELLED -> OFFERS;
            case ORDER_CREATED, ORDER_CONFIRMED, ORDER_PROCESSING, ORDER_CANCELLED, ORDER_COMPLETED -> ORDERS;
            case PAYMENT_CREATED, PAYMENT_SUCCESS, PAYMENT_FAILED -> PAYMENTS;
            case DELIVERY_ASSIGNED, DELIVERY_READY_FOR_PICKUP, DELIVERY_PICKED_UP, DELIVERY_IN_TRANSIT,
                 DELIVERY_OUT_FOR_DELIVERY, DELIVERY_DELIVERED, DELIVERY_CANCELLED -> DELIVERY;
            case NEW_REVIEW -> REVIEWS;
        };
    }
}
