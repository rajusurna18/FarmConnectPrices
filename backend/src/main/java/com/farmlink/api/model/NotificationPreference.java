package com.farmlink.api.model;

import java.time.Instant;

/**
 * Model representing user-controlled notification preferences persisted in Firestore collection 'notificationPreferences'.
 */
public class NotificationPreference {

    private String uid;
    private boolean offers = true;
    private boolean orders = true;
    private boolean payments = true;
    private boolean delivery = true;
    private boolean reviews = true;
    private String createdAt;
    private String updatedAt;

    public NotificationPreference() {
    }

    public NotificationPreference(String uid, boolean offers, boolean orders, boolean payments, boolean delivery, boolean reviews, String createdAt, String updatedAt) {
        this.uid = uid;
        this.offers = offers;
        this.orders = orders;
        this.payments = payments;
        this.delivery = delivery;
        this.reviews = reviews;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreference createDefault(String uid) {
        String now = Instant.now().toString();
        return new NotificationPreference(uid, true, true, true, true, true, now, now);
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isOffers() {
        return offers;
    }

    public void setOffers(boolean offers) {
        this.offers = offers;
    }

    public boolean isOrders() {
        return orders;
    }

    public void setOrders(boolean orders) {
        this.orders = orders;
    }

    public boolean isPayments() {
        return payments;
    }

    public void setPayments(boolean payments) {
        this.payments = payments;
    }

    public boolean isDelivery() {
        return delivery;
    }

    public void setDelivery(boolean delivery) {
        this.delivery = delivery;
    }

    public boolean isReviews() {
        return reviews;
    }

    public void setReviews(boolean reviews) {
        this.reviews = reviews;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
