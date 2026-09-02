package com.farmlink.api.dto.ai;

import java.math.BigDecimal;

public class AiCalculatedMetricsDto {
    private BigDecimal selectedPrice;
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
    private BigDecimal roi;
    private String profitabilityStatus; // PROFITABLE, BREAK_EVEN, LOSS, INSUFFICIENT_DATA

    public AiCalculatedMetricsDto() {}

    public BigDecimal getSelectedPrice() {
        return selectedPrice;
    }

    public void setSelectedPrice(BigDecimal selectedPrice) {
        this.selectedPrice = selectedPrice;
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
}
