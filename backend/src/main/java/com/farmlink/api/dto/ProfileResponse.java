package com.farmlink.api.dto;

public class ProfileResponse {
    private String uid;
    private String displayName;
    private String email;
    private boolean emailVerified;
    private String role;
    private String status;
    private String phoneNumber;
    private LocationDto location;
    private boolean profileCompleted;

    public ProfileResponse() {
    }

    public ProfileResponse(String uid, String displayName, String email, boolean emailVerified,
                           String role, String status, String phoneNumber, LocationDto location,
                           boolean profileCompleted) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.emailVerified = emailVerified;
        this.role = role;
        this.status = status;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.profileCompleted = profileCompleted;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
