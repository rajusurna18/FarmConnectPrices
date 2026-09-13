package com.farmlink.api.provider;

import java.math.BigDecimal;

public class PaymentVerificationProviderResponse {
    private boolean verified;
    private String status; // SUCCESS or FAILED
    private String providerPaymentId;
    private String transactionReference;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String failureCode;
    private String failureMessage;

    public PaymentVerificationProviderResponse() {}

    public PaymentVerificationProviderResponse(boolean verified, String status, String providerPaymentId,
                                               String transactionReference, String paymentMethod,
                                               BigDecimal amount, String currency, String failureCode, String failureMessage) {
        this.verified = verified;
        this.status = status;
        this.providerPaymentId = providerPaymentId;
        this.transactionReference = transactionReference;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
}
