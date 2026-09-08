package com.farmlink.api.dto;

public class UpdateListingStatusRequest {
    private String status;

    public UpdateListingStatusRequest() {
    }

    public UpdateListingStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
