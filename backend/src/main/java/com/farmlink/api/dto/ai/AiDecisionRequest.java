package com.farmlink.api.dto.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiDecisionRequest {
    private AiDecisionType decisionType;

    private String cropId;
    private String farmId;
    private String economicRecordId;

    private List<String> marketIds = new ArrayList<>();
    private BigDecimal quantity;
    private String quantityUnit = "QUINTAL";

    private String priceBasis = "MODAL"; // MIN, MODAL, MAX
    private String priceMode = "LATEST_AVAILABLE"; // EXACT_DATE, LATEST_AVAILABLE
    private String date;

    private BigDecimal transportationCost = BigDecimal.ZERO;
    private BigDecimal otherSellingCosts = BigDecimal.ZERO;

    public AiDecisionRequest() {}

    public AiDecisionType getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(AiDecisionType decisionType) {
        this.decisionType = decisionType;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getEconomicRecordId() {
        return economicRecordId;
    }

    public void setEconomicRecordId(String economicRecordId) {
        this.economicRecordId = economicRecordId;
    }

    public List<String> getMarketIds() {
        return marketIds;
    }

    public void setMarketIds(List<String> marketIds) {
        this.marketIds = marketIds != null ? marketIds : new ArrayList<>();
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
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
