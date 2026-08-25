package com.farmlink.api.dto;

import java.util.Objects;

public class MarketResponse {
    private String id;
    private String name;
    private String code;
    private String type; // MANDI, RYTHU_BAZAAR, WHOLESALE_MARKET, LOCAL_MARKET, OTHER
    private LocationDto location;
    private Double latitude;
    private Double longitude;
    private String status; // ACTIVE, INACTIVE
    private String createdAt;
    private String updatedAt;

    public MarketResponse() {
    }

    public MarketResponse(String id, String name, String code, String type, LocationDto location,
                          Double latitude, Double longitude, String status, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.type = type;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketResponse that = (MarketResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
