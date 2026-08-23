package com.farmlink.api.dto;

public class UserResponse {

    private String uid;
    private String email;
    private String displayName;
    private String role;
    private String status;

    public UserResponse() {
    }

    public UserResponse(String uid, String email, String displayName, String role, String status) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
