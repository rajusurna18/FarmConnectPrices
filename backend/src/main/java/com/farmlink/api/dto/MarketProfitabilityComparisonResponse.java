package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class MarketProfitabilityComparisonResponse {
    private CropResponse crop;
    private double quantity;
    private String quantityUnit;
    private String priceBasis;
    private String priceMode;
    private String date;
    private List<MarketEvaluationResponse> evaluations = new ArrayList<>();
    private MarketEvaluationResponse topRealizationMarket;
    private String rankingSummary;
    private String disclaimer = "Results are transparent estimates based on verified market prices and your entered selling costs. Unentered costs (production, labor, risk, delays, quality deductions) are not included.";

    public MarketProfitabilityComparisonResponse() {}

    public MarketProfitabilityComparisonResponse(CropResponse crop, double quantity, String quantityUnit,
                                                 String priceBasis, String priceMode, String date,
                                                 List<MarketEvaluationResponse> evaluations,
                                                 MarketEvaluationResponse topRealizationMarket,
                                                 String rankingSummary) {
        this.crop = crop;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.priceBasis = priceBasis;
        this.priceMode = priceMode;
        this.date = date;
        this.evaluations = evaluations != null ? evaluations : new ArrayList<>();
        this.topRealizationMarket = topRealizationMarket;
        this.rankingSummary = rankingSummary;
    }

    public CropResponse getCrop() { return crop; }
    public void setCrop(CropResponse crop) { this.crop = crop; }

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

    public List<MarketEvaluationResponse> getEvaluations() { return evaluations; }
    public void setEvaluations(List<MarketEvaluationResponse> evaluations) { this.evaluations = evaluations != null ? evaluations : new ArrayList<>(); }

    public MarketEvaluationResponse getTopRealizationMarket() { return topRealizationMarket; }
    public void setTopRealizationMarket(MarketEvaluationResponse topRealizationMarket) { this.topRealizationMarket = topRealizationMarket; }

    public String getRankingSummary() { return rankingSummary; }
    public void setRankingSummary(String rankingSummary) { this.rankingSummary = rankingSummary; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
