package com.farmlink.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateDiagnosticRequest {

    @NotBlank(message = "diagnosticId is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$", message = "diagnosticId must be 1-64 characters alphanumeric, hyphen, or underscore")
    private String diagnosticId;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$", message = "farmId must be 1-64 characters alphanumeric, hyphen, or underscore")
    private String farmId;

    @NotBlank(message = "cropId is required")
    private String cropId;

    @NotBlank(message = "symptoms description is required")
    @Size(min = 5, max = 2000, message = "symptoms description must be between 5 and 2000 characters")
    private String symptoms;

    private String affectedPart = "LEAF";

    @NotNull(message = "images metadata is required")
    @Valid
    private DiagnosticImagesDto images;

    public CreateDiagnosticRequest() {
    }

    public CreateDiagnosticRequest(String diagnosticId, String farmId, String cropId, String symptoms, String affectedPart, DiagnosticImagesDto images) {
        this.diagnosticId = diagnosticId;
        this.farmId = farmId;
        this.cropId = cropId;
        this.symptoms = symptoms;
        this.affectedPart = (affectedPart != null && !affectedPart.trim().isEmpty()) ? affectedPart.trim().toUpperCase() : "LEAF";
        this.images = images;
    }

    public String getDiagnosticId() {
        return diagnosticId;
    }

    public void setDiagnosticId(String diagnosticId) {
        this.diagnosticId = diagnosticId;
    }

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
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
        this.affectedPart = (affectedPart != null && !affectedPart.trim().isEmpty()) ? affectedPart.trim().toUpperCase() : "LEAF";
    }

    public DiagnosticImagesDto getImages() {
        return images;
    }

    public void setImages(DiagnosticImagesDto images) {
        this.images = images;
    }
}

