package com.farmlink.api.dto;

public class ProductListingResponse {
    private String listingId;
    private String ownerUid; // Provided for ownership checking by front-end if authenticated as owner
    private String cropId;
    private String cropName;
    private Double quantity;
    private Double availableQuantity;
    private String unit;
    private Double askingPrice;
    private String priceUnit;
    private LocationDto location; // Regional location (State, District, Mandal, Village) - no private address/coords
    private String description;
    private String qualityGrade;
    private String harvestDate;
    private String availableFrom;
    private String status;
    private String createdAt;
    private String updatedAt;
    private MarketPriceReferenceDto referenceMarketPrice;

    public ProductListingResponse() {
    }

    public ProductListingResponse(String listingId, String ownerUid, String cropId, String cropName,
                                  Double quantity, Double availableQuantity, String unit, Double askingPrice,
                                  String priceUnit, LocationDto location, String description, String qualityGrade,
                                  String harvestDate, String availableFrom, String status, String createdAt,
                                  String updatedAt, MarketPriceReferenceDto referenceMarketPrice) {
        this.listingId = listingId;
        this.ownerUid = ownerUid;
        this.cropId = cropId;
        this.cropName = cropName;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
        this.unit = unit;
        this.askingPrice = askingPrice;
        this.priceUnit = priceUnit;
        this.location = location;
        this.description = description;
        this.qualityGrade = qualityGrade;
        this.harvestDate = harvestDate;
        this.availableFrom = availableFrom;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.referenceMarketPrice = referenceMarketPrice;
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public MarketPriceReferenceDto getReferenceMarketPrice() { return referenceMarketPrice; }
    public void setReferenceMarketPrice(MarketPriceReferenceDto referenceMarketPrice) { this.referenceMarketPrice = referenceMarketPrice; }
}
