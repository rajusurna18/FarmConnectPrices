package com.farmlink.api.dto;

public class CropResponse {
    private String id;
    private String name;
    private String category;
    private String scientificName;
    private String status;

    public CropResponse() {
    }

    public CropResponse(String id, String name, String category, String scientificName, String status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.scientificName = scientificName;
        this.status = status;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
