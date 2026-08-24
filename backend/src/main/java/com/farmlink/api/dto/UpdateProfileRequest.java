package com.farmlink.api.dto;

public class UpdateProfileRequest {
    private String displayName;
    private String phoneNumber;
    private LocationDto location;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String displayName, String phoneNumber, LocationDto location) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.location = location;
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
}
