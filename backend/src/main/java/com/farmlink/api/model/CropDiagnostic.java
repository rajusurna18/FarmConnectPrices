package com.farmlink.api.model;

import com.farmlink.api.dto.DiagnosticImagesDto;
import com.google.cloud.Timestamp;

public class CropDiagnostic {

    private String id;
    private String farmerUid;
    private String farmId;
    private String farmCropId;
    private String cropId;
    private String cropName;
    private String symptoms;
    private String affectedPart;
    private DiagnosticImagesDto images;
    private String status;
    private Object aiDiagnosis;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CropDiagnostic() {
    }

    public CropDiagnostic(
            String id,
            String farmerUid,
            String farmId,
            String farmCropId,
            String cropId,
            String cropName,
            String symptoms,
            String affectedPart,
            DiagnosticImagesDto images,
            String status,
            Object aiDiagnosis,
            Timestamp createdAt,
            Timestamp updatedAt
    ) {
        this.id = id;
        this.farmerUid = farmerUid;
        this.farmId = farmId;
        this.farmCropId = farmCropId;
        this.cropId = cropId;
        this.cropName = cropName;
        this.symptoms = symptoms;
        this.affectedPart = affectedPart;
        this.images = images;
        this.status = status;
        this.aiDiagnosis = aiDiagnosis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerUid() {
        return farmerUid;
    }

    public void setFarmerUid(String farmerUid) {
        this.farmerUid = farmerUid;
    }

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getFarmCropId() {
        return farmCropId;
    }

    public void setFarmCropId(String farmCropId) {
        this.farmCropId = farmCropId;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getAffectedPart() {
        return affectedPart;
    }

    public void setAffectedPart(String affectedPart) {
        this.affectedPart = affectedPart;
    }

    public DiagnosticImagesDto getImages() {
        return images;
    }

    public void setImages(DiagnosticImagesDto images) {
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getAiDiagnosis() {
        return aiDiagnosis;
    }

    public void setAiDiagnosis(Object aiDiagnosis) {
        this.aiDiagnosis = aiDiagnosis;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

