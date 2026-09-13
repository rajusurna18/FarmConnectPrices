package com.farmlink.api.dto;

/**
 * Request DTO for creating a payment intent.
 * Note: Only orderId is submitted by client. Commercial total and currency are derived server-side.
 */
public class CreatePaymentIntentRequest {
    private String orderId;

    public CreatePaymentIntentRequest() {}

    public CreatePaymentIntentRequest(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
