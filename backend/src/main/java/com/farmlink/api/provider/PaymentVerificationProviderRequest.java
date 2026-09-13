package com.farmlink.api.provider;

public class PaymentVerificationProviderRequest {
    private String providerPaymentId;
    private String providerSignature;
    private String simulatedStatus; // "SUCCESS" or "FAILED" for mock mode
    private String paymentMethod;
    private String failureReason;

    public PaymentVerificationProviderRequest() {}

    public PaymentVerificationProviderRequest(String providerPaymentId, String providerSignature, String simulatedStatus, String paymentMethod, String failureReason) {
        this.providerPaymentId = providerPaymentId;
        this.providerSignature = providerSignature;
        this.simulatedStatus = simulatedStatus;
        this.paymentMethod = paymentMethod;
        this.failureReason = failureReason;
    }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public String getProviderSignature() { return providerSignature; }
    public void setProviderSignature(String providerSignature) { this.providerSignature = providerSignature; }

    public String getSimulatedStatus() { return simulatedStatus; }
    public void setSimulatedStatus(String simulatedStatus) { this.simulatedStatus = simulatedStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
