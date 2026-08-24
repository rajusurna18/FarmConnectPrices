package com.farmlink.api.dto;

public class RoleSelectionRequest {
    private String role;

    public RoleSelectionRequest() {
    }

    public RoleSelectionRequest(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
