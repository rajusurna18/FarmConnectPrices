package com.farmlink.api.dto;

public class UpdateProfileRequest {
    private String displayName;
    private String phoneNumber;
    private LocationDto location;
    private String businessOrganizationName;
    private String address;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String displayName, String phoneNumber, LocationDto location,
                                String businessOrganizationName, String address) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.businessOrganizationName = businessOrganizationName;
        this.address = address;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public String getBusinessOrganizationName() {
        return businessOrganizationName;
    }

    public void setBusinessOrganizationName(String businessOrganizationName) {
        this.businessOrganizationName = businessOrganizationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
