package com.farmlink.api.dto;

public class FarmCropResponse {
    private String id;
    private String farmId;
    private String ownerUid;
    private String cropId;
    private CropResponse crop;
    private String season;
    private String status;
    private String createdAt;
    private String updatedAt;

    public FarmCropResponse() {
    }

    public FarmCropResponse(String id, String farmId, String ownerUid, String cropId, CropResponse crop, String season, String status, String createdAt, String updatedAt) {
        this.id = id;
        this.farmId = farmId;
        this.ownerUid = ownerUid;
        this.cropId = cropId;
        this.crop = crop;
        this.season = season;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public CropResponse getCrop() {
        return crop;
    }

    public void setCrop(CropResponse crop) {
        this.crop = crop;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
