package com.farmlink.api.dto;

import com.farmlink.api.model.NotificationPreference;

public class NotificationPreferenceResponse {

    private boolean offers;
    private boolean orders;
    private boolean payments;
    private boolean delivery;
    private boolean reviews;

    public NotificationPreferenceResponse() {
    }

    public NotificationPreferenceResponse(boolean offers, boolean orders, boolean payments, boolean delivery, boolean reviews) {
        this.offers = offers;
        this.orders = orders;
        this.payments = payments;
        this.delivery = delivery;
        this.reviews = reviews;
    }

    public NotificationPreferenceResponse(NotificationPreference preference) {
        if (preference != null) {
            this.offers = preference.isOffers();
            this.orders = preference.isOrders();
            this.payments = preference.isPayments();
            this.delivery = preference.isDelivery();
            this.reviews = preference.isReviews();
        }
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
}
