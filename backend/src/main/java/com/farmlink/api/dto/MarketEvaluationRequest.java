package com.farmlink.api.dto;

public class MarketEvaluationRequest {
    private String cropId;
    private String marketId;
    private double quantity;
    private String quantityUnit;
    private String priceBasis = "MODAL"; // MODAL, MIN, MAX
    private String priceMode = "LATEST_AVAILABLE"; // LATEST_AVAILABLE, EXACT_DATE
    private String date; // YYYY-MM-DD (optional, required if priceMode == EXACT_DATE)
    private Double transportationCost = 0.0;
    private Double otherSellingCosts = 0.0;

    public MarketEvaluationRequest() {}

    public MarketEvaluationRequest(String cropId, String marketId, double quantity, String quantityUnit,
                                   String priceBasis, String priceMode, String date,
                                   Double transportationCost, Double otherSellingCosts) {
        this.cropId = cropId;
        this.marketId = marketId;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.priceBasis = priceBasis != null ? priceBasis : "MODAL";
        this.priceMode = priceMode != null ? priceMode : "LATEST_AVAILABLE";
        this.date = date;
        this.transportationCost = transportationCost != null ? transportationCost : 0.0;
        this.otherSellingCosts = otherSellingCosts != null ? otherSellingCosts : 0.0;
    }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getMarketId() { return marketId; }
    public void setMarketId(String marketId) { this.marketId = marketId; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public String getPriceBasis() { return priceBasis; }
    public void setPriceBasis(String priceBasis) { this.priceBasis = priceBasis; }

    public String getPriceMode() { return priceMode; }
    public void setPriceMode(String priceMode) { this.priceMode = priceMode; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Double getTransportationCost() { return transportationCost; }
    public void setTransportationCost(Double transportationCost) { this.transportationCost = transportationCost; }

    public Double getOtherSellingCosts() { return otherSellingCosts; }
    public void setOtherSellingCosts(Double otherSellingCosts) { this.otherSellingCosts = otherSellingCosts; }
}
