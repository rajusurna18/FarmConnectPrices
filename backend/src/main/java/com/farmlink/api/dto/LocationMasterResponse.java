package com.farmlink.api.dto;

public class LocationMasterResponse {
    private String id;
    private String state;
    private String district;
    private String mandal;
    private String village;
    private String pincode;

    public LocationMasterResponse() {
    }

    public LocationMasterResponse(String id, String state, String district, String mandal, String village, String pincode) {
        this.id = id;
        this.state = state;
        this.district = district;
        this.mandal = mandal;
        this.village = village;
        this.pincode = pincode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
}
