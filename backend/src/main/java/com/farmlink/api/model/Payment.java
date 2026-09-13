package com.farmlink.api.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Single aggregate domain entity representing a Payment stored in Firestore collection 'payments'.
 * Encapsulates intent details, status lifecycle, provider transaction references, and attempt history.
 */
public class Payment {
    private String paymentId;          // Keyed deterministically as pay_{orderId} or pay_UUID
    private String orderId;
    private String buyerUid;
    private String farmerId;
    private String buyerRole;          // MEDIATOR_BUYER or CUSTOMER

    private BigDecimal amount;
    private String currency;           // "INR"

    private String status;             // CREATED, PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, EXPIRED
    private String provider;           // MOCK, RAZORPAY, STRIPE, etc.

    private String providerPaymentId;
    private String providerOrderId;
    private String transactionReference;
    private String paymentMethod;      // MOCK_CARD, UPI, NET_BANKING, etc.

    private String failureCode;
    private String failureMessage;

    private String createdAt;
    private String updatedAt;
    private String initiatedAt;
    private String verifiedAt;
    private String failedAt;
    private String expiresAt;

    private List<PaymentAttempt> attempts = new ArrayList<>();

    public Payment() {}

    public Payment(String paymentId, String orderId, String buyerUid, String farmerId, String buyerRole,
                   BigDecimal amount, String currency, String status, String provider, String providerPaymentId,
                   String providerOrderId, String transactionReference, String paymentMethod, String failureCode,
                   String failureMessage, String createdAt, String updatedAt, String initiatedAt, String verifiedAt,
                   String failedAt, String expiresAt, List<PaymentAttempt> attempts) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.buyerUid = buyerUid;
        this.farmerId = farmerId;
        this.buyerRole = buyerRole;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.provider = provider;
        this.providerPaymentId = providerPaymentId;
        this.providerOrderId = providerOrderId;
        this.transactionReference = transactionReference;
        this.paymentMethod = paymentMethod;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.initiatedAt = initiatedAt;
        this.verifiedAt = verifiedAt;
        this.failedAt = failedAt;
        this.expiresAt = expiresAt;
        if (attempts != null) {
            this.attempts = attempts;
        }
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getBuyerUid() { return buyerUid; }
    public void setBuyerUid(String buyerUid) { this.buyerUid = buyerUid; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getBuyerRole() { return buyerRole; }
    public void setBuyerRole(String buyerRole) { this.buyerRole = buyerRole; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(String initiatedAt) { this.initiatedAt = initiatedAt; }

    public String getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(String verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getFailedAt() { return failedAt; }
    public void setFailedAt(String failedAt) { this.failedAt = failedAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public List<PaymentAttempt> getAttempts() { return attempts; }
    public void setAttempts(List<PaymentAttempt> attempts) { this.attempts = attempts != null ? attempts : new ArrayList<>(); }
}
