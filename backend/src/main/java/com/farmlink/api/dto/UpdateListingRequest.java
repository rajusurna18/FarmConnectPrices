package com.farmlink.api.dto;

public class UpdateListingRequest {
    private Double quantity;
    private Double availableQuantity;
    private String unit;
    private Double askingPrice;
    private String priceUnit;
    private LocationDto location;
    private String description;
    private String qualityGrade;
    private String harvestDate;
    private String availableFrom;

    public UpdateListingRequest() {
    }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Double availableQuantity) { this.availableQuantity = availableQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getAskingPrice() { return askingPrice; }
    public void setAskingPrice(Double askingPrice) { this.askingPrice = askingPrice; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public LocationDto getLocation() { return location; }
    public void setLocation(LocationDto location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getHarvestDate() { return harvestDate; }
    public void setHarvestDate(String harvestDate) { this.harvestDate = harvestDate; }

    public String getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(String availableFrom) { this.availableFrom = availableFrom; }
}
