package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class MarketComparisonRequest {
    private String cropId;
    private double quantity;
    private String quantityUnit;
    private String priceBasis = "MODAL"; // MODAL, MIN, MAX
    private String priceMode = "LATEST_AVAILABLE"; // LATEST_AVAILABLE, EXACT_DATE
    private String date; // YYYY-MM-DD
    private List<MarketCostInputDto> markets = new ArrayList<>();

    public MarketComparisonRequest() {}

    public MarketComparisonRequest(String cropId, double quantity, String quantityUnit,
                                   String priceBasis, String priceMode, String date,
                                   List<MarketCostInputDto> markets) {
        this.cropId = cropId;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.priceBasis = priceBasis != null ? priceBasis : "MODAL";
        this.priceMode = priceMode != null ? priceMode : "LATEST_AVAILABLE";
        this.date = date;
        this.markets = markets != null ? markets : new ArrayList<>();
    }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

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

    public List<MarketCostInputDto> getMarkets() { return markets; }
    public void setMarkets(List<MarketCostInputDto> markets) { this.markets = markets != null ? markets : new ArrayList<>(); }
}
