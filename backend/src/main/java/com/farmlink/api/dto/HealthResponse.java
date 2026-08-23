package com.farmlink.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthResponse {
    private String status;
    private String service;
    private String firebase;

    public HealthResponse() {
    }

    public HealthResponse(String status, String service) {
        this.status = status;
        this.service = service;
    }

    public HealthResponse(String status, String service, String firebase) {
        this.status = status;
        this.service = service;
        this.firebase = firebase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getFirebase() {
        return firebase;
    }

    public void setFirebase(String firebase) {
        this.firebase = firebase;
    }
}
