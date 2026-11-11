package com.farmlink.api.dto;

public class AddDisputeResponseRequest {
    private String message;

    public AddDisputeResponseRequest() {}

    public AddDisputeResponseRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

