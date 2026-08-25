package com.farmlink.api.dto;

public class FarmRequest {
    private String name;
    private LocationDto location;
    private Double landArea;
    private String landAreaUnit; // ACRE or HECTARE
    private String status;       // ACTIVE or INACTIVE

    public FarmRequest() {
    }

    public FarmRequest(String name, LocationDto location, Double landArea, String landAreaUnit, String status) {
        this.name = name;
        this.location = location;
        this.landArea = landArea;
        this.landAreaUnit = landAreaUnit;
        this.status = status;
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
}
