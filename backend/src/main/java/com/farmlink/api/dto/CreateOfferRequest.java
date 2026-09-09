package com.farmlink.api.dto;

public class CreateOfferRequest {
    private String listingId;
    private Double offeredQuantity;
    private String quantityUnit;
    private Double offeredPrice;
    private String priceUnit;
    private String message;

    public CreateOfferRequest() {}

    public CreateOfferRequest(String listingId, Double offeredQuantity, String quantityUnit,
                              Double offeredPrice, String priceUnit, String message) {
        this.listingId = listingId;
        this.offeredQuantity = offeredQuantity;
        this.quantityUnit = quantityUnit;
        this.offeredPrice = offeredPrice;
        this.priceUnit = priceUnit;
        this.message = message;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public Double getOfferedQuantity() {
        return offeredQuantity;
    }

    public void setOfferedQuantity(Double offeredQuantity) {
        this.offeredQuantity = offeredQuantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public Double getOfferedPrice() {
        return offeredPrice;
    }

    public void setOfferedPrice(Double offeredPrice) {
        this.offeredPrice = offeredPrice;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
