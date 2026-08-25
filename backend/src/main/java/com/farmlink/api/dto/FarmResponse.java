package com.farmlink.api.dto;

public class FarmResponse {
    private String id;
    private String ownerUid;
    private String name;
    private LocationDto location;
    private Double landArea;
    private String landAreaUnit;
    private String status;
    private String createdAt;
    private String updatedAt;

    public FarmResponse() {
    }

    public FarmResponse(String id, String ownerUid, String name, LocationDto location, Double landArea, String landAreaUnit, String status, String createdAt, String updatedAt) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.name = name;
        this.location = location;
        this.landArea = landArea;
        this.landAreaUnit = landAreaUnit;
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

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public Double getLandArea() {
        return landArea;
    }

    public void setLandArea(Double landArea) {
        this.landArea = landArea;
    }

    public String getLandAreaUnit() {
        return landAreaUnit;
    }

    public void setLandAreaUnit(String landAreaUnit) {
        this.landAreaUnit = landAreaUnit;
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
