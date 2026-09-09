package com.farmlink.api.dto;

public class CounterOfferRequest {
    private Double counterQuantity;
    private String quantityUnit;
    private Double counterPrice;
    private String priceUnit;
    private String message;

    public CounterOfferRequest() {}

    public CounterOfferRequest(Double counterQuantity, String quantityUnit, Double counterPrice, String priceUnit, String message) {
        this.counterQuantity = counterQuantity;
        this.quantityUnit = quantityUnit;
        this.counterPrice = counterPrice;
        this.priceUnit = priceUnit;
        this.message = message;
    }

    public Double getCounterQuantity() {
        return counterQuantity;
    }

    public void setCounterQuantity(Double counterQuantity) {
        this.counterQuantity = counterQuantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public Double getCounterPrice() {
        return counterPrice;
    }

    public void setCounterPrice(Double counterPrice) {
        this.counterPrice = counterPrice;
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
