package com.farmlink.api.provider;

import java.math.BigDecimal;

public class PaymentIntentProviderRequest {
    private String orderId;
    private String buyerUid;
    private BigDecimal amount;
    private String currency;

    public PaymentIntentProviderRequest() {}

    public PaymentIntentProviderRequest(String orderId, String buyerUid, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.buyerUid = buyerUid;
        this.amount = amount;
        this.currency = currency;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerUid() { return buyerUid; }
    public void setBuyerUid(String buyerUid) { this.buyerUid = buyerUid; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
