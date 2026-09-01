package com.farmlink.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FarmMarketComparisonResponse {
    private CropResponse crop;
    private BigDecimal expectedYield;
    private String yieldUnit;
    private String priceBasis;
    private String priceMode;
    private String date;

    private List<FarmProfitabilityEvaluationResponse> evaluations = new ArrayList<>();
    private FarmProfitabilityEvaluationResponse topMarket;
    private String rankingSummary;

    public FarmMarketComparisonResponse() {}

    public FarmMarketComparisonResponse(
            CropResponse crop,
            BigDecimal expectedYield,
            String yieldUnit,
            String priceBasis,
            String priceMode,
            String date,
            List<FarmProfitabilityEvaluationResponse> evaluations,
            FarmProfitabilityEvaluationResponse topMarket,
            String rankingSummary
    ) {
        this.crop = crop;
        this.expectedYield = expectedYield;
        this.yieldUnit = yieldUnit;
        this.priceBasis = priceBasis;
        this.priceMode = priceMode;
        this.date = date;
        this.evaluations = evaluations != null ? evaluations : new ArrayList<>();
        this.topMarket = topMarket;
        this.rankingSummary = rankingSummary;
    }

    public CropResponse getCrop() {
        return crop;
    }

    public void setCrop(CropResponse crop) {
        this.crop = crop;
    }

    public BigDecimal getExpectedYield() {
        return expectedYield;
    }

    public void setExpectedYield(BigDecimal expectedYield) {
        this.expectedYield = expectedYield;
    }

    public String getYieldUnit() {
        return yieldUnit;
    }

    public void setYieldUnit(String yieldUnit) {
        this.yieldUnit = yieldUnit;
    }

    public String getPriceBasis() {
        return priceBasis;
    }

    public void setPriceBasis(String priceBasis) {
        this.priceBasis = priceBasis;
    }

    public String getPriceMode() {
        return priceMode;
    }

    public void setPriceMode(String priceMode) {
        this.priceMode = priceMode;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<FarmProfitabilityEvaluationResponse> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<FarmProfitabilityEvaluationResponse> evaluations) {
        this.evaluations = evaluations != null ? evaluations : new ArrayList<>();
    }

    public FarmProfitabilityEvaluationResponse getTopMarket() {
        return topMarket;
    }

    public void setTopMarket(FarmProfitabilityEvaluationResponse topMarket) {
        this.topMarket = topMarket;
    }

    public String getRankingSummary() {
        return rankingSummary;
    }

    public void setRankingSummary(String rankingSummary) {
        this.rankingSummary = rankingSummary;
    }
}
