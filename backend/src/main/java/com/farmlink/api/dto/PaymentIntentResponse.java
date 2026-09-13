package com.farmlink.api.dto;

import java.math.BigDecimal;

/**
 * Response DTO returned when a payment intent is created or retrieved.
 */
public class PaymentIntentResponse {
    private String paymentIntentId;
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String providerOrderId;
    private String status;
    private String createdAt;
    private String expiresAt;

    public PaymentIntentResponse() {}

    public PaymentIntentResponse(String paymentIntentId, String paymentId, String orderId, BigDecimal amount,
                                 String currency, String provider, String providerOrderId, String status,
                                 String createdAt, String expiresAt) {
        this.paymentIntentId = paymentIntentId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.providerOrderId = providerOrderId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
