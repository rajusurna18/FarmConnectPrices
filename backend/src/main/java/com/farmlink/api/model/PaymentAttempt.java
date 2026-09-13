package com.farmlink.api.model;

/**
 * Domain entity representing an individual payment verification/gateway attempt.
 */
public class PaymentAttempt {
    private String attemptId;
    private String providerPaymentId;
    private String transactionReference;
    private String paymentMethod; // UPI, MOCK_CARD, NET_BANKING, etc.
    private String status;        // SUCCESS, FAILED, PROCESSING
    private String failureCode;
    private String failureMessage;
    private String attemptedAt;
    private String verifiedAt;

    public PaymentAttempt() {}

    public PaymentAttempt(String attemptId, String providerPaymentId, String transactionReference,
                          String paymentMethod, String status, String failureCode,
                          String failureMessage, String attemptedAt, String verifiedAt) {
        this.attemptId = attemptId;
        this.providerPaymentId = providerPaymentId;
        this.transactionReference = transactionReference;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.attemptedAt = attemptedAt;
        this.verifiedAt = verifiedAt;
    }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public String getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(String attemptedAt) { this.attemptedAt = attemptedAt; }

    public String getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(String verifiedAt) { this.verifiedAt = verifiedAt; }
}
