package com.farmlink.api.dto;

public class CancelDeliveryRequest {
    private String reason;

    public CancelDeliveryRequest() {}

    public CancelDeliveryRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
