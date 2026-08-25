package com.farmlink.api.dto;

import java.util.Objects;

public class MarketCropResponse {
    private String id;
    private String marketId;
    private String cropId;
    private String cropName;
    private String cropCategory;
    private String cropScientificName;
    private String status;
    private String createdAt;
    private String updatedAt;

    public MarketCropResponse() {
    }

    public MarketCropResponse(String id, String marketId, String cropId, String cropName,
                              String cropCategory, String cropScientificName, String status,
                              String createdAt, String updatedAt) {
        this.id = id;
        this.marketId = marketId;
        this.cropId = cropId;
        this.cropName = cropName;
        this.cropCategory = cropCategory;
        this.cropScientificName = cropScientificName;
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

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getCropCategory() {
        return cropCategory;
    }

    public void setCropCategory(String cropCategory) {
        this.cropCategory = cropCategory;
    }

    public String getCropScientificName() {
        return cropScientificName;
    }

    public void setCropScientificName(String cropScientificName) {
        this.cropScientificName = cropScientificName;
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
        MarketCropResponse that = (MarketCropResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
