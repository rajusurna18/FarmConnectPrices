package com.farmlink.api.provider;

import java.math.BigDecimal;

public class PaymentStatusProviderResponse {
    private String providerPaymentId;
    private String status;
    private BigDecimal amount;
    private String currency;

    public PaymentStatusProviderResponse() {}

    public PaymentStatusProviderResponse(String providerPaymentId, String status, BigDecimal amount, String currency) {
        this.providerPaymentId = providerPaymentId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
    }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
