package com.farmlink.api.dto;

public class UpdateNotificationPreferenceRequest {

    private Boolean offers;
    private Boolean orders;
    private Boolean payments;
    private Boolean delivery;
    private Boolean reviews;

    public UpdateNotificationPreferenceRequest() {
    }

    public UpdateNotificationPreferenceRequest(Boolean offers, Boolean orders, Boolean payments, Boolean delivery, Boolean reviews) {
        this.offers = offers;
        this.orders = orders;
        this.payments = payments;
        this.delivery = delivery;
        this.reviews = reviews;
    }

    public Boolean getOffers() {
        return offers;
    }

    public void setOffers(Boolean offers) {
        this.offers = offers;
    }

    public Boolean getOrders() {
        return orders;
    }

    public void setOrders(Boolean orders) {
        this.orders = orders;
    }

    public Boolean getPayments() {
        return payments;
    }

    public void setPayments(Boolean payments) {
        this.payments = payments;
    }

    public Boolean getDelivery() {
        return delivery;
    }

    public void setDelivery(Boolean delivery) {
        this.delivery = delivery;
    }

    public Boolean getReviews() {
        return reviews;
    }

    public void setReviews(Boolean reviews) {
        this.reviews = reviews;
    }
}
