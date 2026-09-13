package com.farmlink.api.dto;

/**
 * Request DTO for verifying a payment.
 * Allows passing provider verification tokens/signatures.
 * For MOCK provider: allows passing simulatedStatus ("SUCCESS" or "FAILED") during development/test mode.
 */
public class VerifyPaymentRequest {
    private String providerPaymentId;
    private String providerSignature;
    private String paymentMethod;
    private String simulatedStatus; // "SUCCESS" or "FAILED" for MOCK mode
    private String failureReason;

    public VerifyPaymentRequest() {}

    public VerifyPaymentRequest(String providerPaymentId, String providerSignature, String paymentMethod, String simulatedStatus, String failureReason) {
        this.providerPaymentId = providerPaymentId;
        this.providerSignature = providerSignature;
        this.paymentMethod = paymentMethod;
        this.simulatedStatus = simulatedStatus;
        this.failureReason = failureReason;
    }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getProviderSignature() { return providerSignature; }
    public void setProviderSignature(String providerSignature) { this.providerSignature = providerSignature; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSimulatedStatus() { return simulatedStatus; }
    public void setSimulatedStatus(String simulatedStatus) { this.simulatedStatus = simulatedStatus; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
