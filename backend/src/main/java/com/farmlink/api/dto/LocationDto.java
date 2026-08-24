package com.farmlink.api.dto;

public class LocationDto {
    private String state;
    private String district;
    private String mandal;
    private String village;

    public LocationDto() {
    }

    public LocationDto(String state, String district, String mandal, String village) {
        this.state = state;
        this.district = district;
        this.mandal = mandal;
        this.village = village;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getMandal() {
        return mandal;
    }

    public void setMandal(String mandal) {
        this.mandal = mandal;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public boolean isComplete() {
        return isNotNullOrEmpty(state) &&
               isNotNullOrEmpty(district) &&
               isNotNullOrEmpty(mandal) &&
               isNotNullOrEmpty(village);
    }

    private boolean isNotNullOrEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
