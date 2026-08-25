package com.farmlink.api.dto;

public class FarmCropRequest {
    private String cropId;
    private String season; // KHARIF, RABI, ZAID
    private String status; // ACTIVE, INACTIVE

    public FarmCropRequest() {
    }

    public FarmCropRequest(String cropId, String season, String status) {
        this.cropId = cropId;
        this.season = season;
        this.status = status;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
