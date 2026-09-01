package com.farmlink.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FarmProfitabilityEvaluationRequest {
    private String economicRecordId;

    private String farmId;
    private String cropId;
    private String season;
    private BigDecimal cultivatedArea;
    private String cultivatedAreaUnit;
    private BigDecimal expectedYield;
    private String yieldUnit;

    private List<ProductionCostItemDto> productionCosts = new ArrayList<>();

    private String marketId;
    private String priceBasis = "MODAL"; // MIN, MODAL, MAX
    private String priceMode = "LATEST_AVAILABLE"; // EXACT_DATE, LATEST_AVAILABLE
    private String date;

    private BigDecimal transportationCost = BigDecimal.ZERO;
    private BigDecimal otherSellingCosts = BigDecimal.ZERO;

    public FarmProfitabilityEvaluationRequest() {}

    public String getEconomicRecordId() {
        return economicRecordId;
    }

    public void setEconomicRecordId(String economicRecordId) {
        this.economicRecordId = economicRecordId;
    }

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public BigDecimal getCultivatedArea() {
        return cultivatedArea;
    }

    public void setCultivatedArea(BigDecimal cultivatedArea) {
        this.cultivatedArea = cultivatedArea;
    }

    public String getCultivatedAreaUnit() {
        return cultivatedAreaUnit;
    }

    public void setCultivatedAreaUnit(String cultivatedAreaUnit) {
        this.cultivatedAreaUnit = cultivatedAreaUnit;
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

    public List<ProductionCostItemDto> getProductionCosts() {
        return productionCosts;
    }

    public void setProductionCosts(List<ProductionCostItemDto> productionCosts) {
        this.productionCosts = productionCosts != null ? productionCosts : new ArrayList<>();
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
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

    public BigDecimal getTransportationCost() {
        return transportationCost;
    }

    public void setTransportationCost(BigDecimal transportationCost) {
        this.transportationCost = transportationCost != null ? transportationCost : BigDecimal.ZERO;
    }

    public BigDecimal getOtherSellingCosts() {
        return otherSellingCosts;
    }

    public void setOtherSellingCosts(BigDecimal otherSellingCosts) {
        this.otherSellingCosts = otherSellingCosts != null ? otherSellingCosts : BigDecimal.ZERO;
    }
}
