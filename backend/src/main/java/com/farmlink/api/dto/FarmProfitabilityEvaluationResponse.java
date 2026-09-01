package com.farmlink.api.dto;

import java.math.BigDecimal;

public class FarmProfitabilityEvaluationResponse {
    private String status;
    private String message;

    private CropResponse crop;
    private FarmResponse farm;
    private MarketSummaryResponse market;

    private BigDecimal selectedPrice;
    private String priceBasis;
    private String priceUnit;

    private BigDecimal expectedYield;
    private String yieldUnit;

    private BigDecimal grossRevenue;
    private BigDecimal totalProductionCost;
    private BigDecimal totalSellingCost;
    private BigDecimal totalCost;
    private BigDecimal estimatedNetRealization;
    private BigDecimal estimatedProfit;

    private BigDecimal profitPerUnit;
    private BigDecimal productionCostPerUnit;
    private BigDecimal totalCostPerUnit;
    private BigDecimal breakEvenSellingPrice;
    private BigDecimal roi; // percentage (or null if totalCost == 0)

    private String profitabilityStatus; // PROFITABLE, BREAK_EVEN, LOSS, INSUFFICIENT_DATA
    private String currency = "INR";

    private String priceDate;
    private String observedAt;
    private MarketPriceSourceDto source;
    private String qualityStatus;
    private boolean stalePrice;
    private String staleMessage;
    private String costCompleteness; // COMPLETE, PARTIAL

    public FarmProfitabilityEvaluationResponse() {}

    public static FarmProfitabilityEvaluationResponse error(String status, String message) {
        FarmProfitabilityEvaluationResponse resp = new FarmProfitabilityEvaluationResponse();
        resp.setStatus(status);
        resp.setMessage(message);
        resp.setProfitabilityStatus("INSUFFICIENT_DATA");
        return resp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CropResponse getCrop() {
        return crop;
    }

    public void setCrop(CropResponse crop) {
        this.crop = crop;
    }

    public FarmResponse getFarm() {
        return farm;
    }

    public void setFarm(FarmResponse farm) {
        this.farm = farm;
    }

    public MarketSummaryResponse getMarket() {
        return market;
    }

    public void setMarket(MarketSummaryResponse market) {
        this.market = market;
    }

    public BigDecimal getSelectedPrice() {
        return selectedPrice;
    }

    public void setSelectedPrice(BigDecimal selectedPrice) {
        this.selectedPrice = selectedPrice;
    }

    public String getPriceBasis() {
        return priceBasis;
    }

    public void setPriceBasis(String priceBasis) {
        this.priceBasis = priceBasis;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
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

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }

    public void setGrossRevenue(BigDecimal grossRevenue) {
        this.grossRevenue = grossRevenue;
    }

    public BigDecimal getTotalProductionCost() {
        return totalProductionCost;
    }

    public void setTotalProductionCost(BigDecimal totalProductionCost) {
        this.totalProductionCost = totalProductionCost;
    }

    public BigDecimal getTotalSellingCost() {
        return totalSellingCost;
    }

    public void setTotalSellingCost(BigDecimal totalSellingCost) {
        this.totalSellingCost = totalSellingCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getEstimatedNetRealization() {
        return estimatedNetRealization;
    }

    public void setEstimatedNetRealization(BigDecimal estimatedNetRealization) {
        this.estimatedNetRealization = estimatedNetRealization;
    }

    public BigDecimal getEstimatedProfit() {
        return estimatedProfit;
    }

    public void setEstimatedProfit(BigDecimal estimatedProfit) {
        this.estimatedProfit = estimatedProfit;
    }

    public BigDecimal getProfitPerUnit() {
        return profitPerUnit;
    }

    public void setProfitPerUnit(BigDecimal profitPerUnit) {
        this.profitPerUnit = profitPerUnit;
    }

    public BigDecimal getProductionCostPerUnit() {
        return productionCostPerUnit;
    }

    public void setProductionCostPerUnit(BigDecimal productionCostPerUnit) {
        this.productionCostPerUnit = productionCostPerUnit;
    }

    public BigDecimal getTotalCostPerUnit() {
        return totalCostPerUnit;
    }

    public void setTotalCostPerUnit(BigDecimal totalCostPerUnit) {
        this.totalCostPerUnit = totalCostPerUnit;
    }

    public BigDecimal getBreakEvenSellingPrice() {
        return breakEvenSellingPrice;
    }

    public void setBreakEvenSellingPrice(BigDecimal breakEvenSellingPrice) {
        this.breakEvenSellingPrice = breakEvenSellingPrice;
    }

    public BigDecimal getRoi() {
        return roi;
    }

    public void setRoi(BigDecimal roi) {
        this.roi = roi;
    }

    public String getProfitabilityStatus() {
        return profitabilityStatus;
    }

    public void setProfitabilityStatus(String profitabilityStatus) {
        this.profitabilityStatus = profitabilityStatus;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(String priceDate) {
        this.priceDate = priceDate;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(String observedAt) {
        this.observedAt = observedAt;
    }

    public MarketPriceSourceDto getSource() {
        return source;
    }

    public void setSource(MarketPriceSourceDto source) {
        this.source = source;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public boolean isStalePrice() {
        return stalePrice;
    }

    public void setStalePrice(boolean stalePrice) {
        this.stalePrice = stalePrice;
    }

    public String getStaleMessage() {
        return staleMessage;
    }

    public void setStaleMessage(String staleMessage) {
        this.staleMessage = staleMessage;
    }

    public String getCostCompleteness() {
        return costCompleteness;
    }

    public void setCostCompleteness(String costCompleteness) {
        this.costCompleteness = costCompleteness;
    }
}
