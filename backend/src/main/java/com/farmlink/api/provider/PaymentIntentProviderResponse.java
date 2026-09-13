package com.farmlink.api.provider;

import java.math.BigDecimal;

public class PaymentIntentProviderResponse {
    private String providerPaymentId;
    private String providerOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String expiresAt;

    public PaymentIntentProviderResponse() {}

    public PaymentIntentProviderResponse(String providerPaymentId, String providerOrderId, BigDecimal amount, String currency, String status, String expiresAt) {
        this.providerPaymentId = providerPaymentId;
        this.providerOrderId = providerOrderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
