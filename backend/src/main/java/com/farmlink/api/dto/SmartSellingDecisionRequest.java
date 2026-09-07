package com.farmlink.api.dto;

import com.farmlink.api.dto.SellingCostsDto;

import java.util.List;

public class SmartSellingDecisionRequest {

    private String cropId;
    private double quantity;
    private String quantityUnit; // e.g. "QUINTAL"
    private List<String> candidateMarketIds; // Bounded list, max 10

    private String farmId; // Optional
    private String economicRecordId; // Optional saved economic record
    private SellingCostsDto customSellingCosts; // Optional farmer entered custom costs (USER_ESTIMATE)

    private String priceBasis = "MODAL"; // MODAL, MIN, MAX
    private String forecastHorizon = "7_DAYS"; // 1_DAY, 3_DAYS, 7_DAYS, 14_DAYS

    public SmartSellingDecisionRequest() {
    }

    public SmartSellingDecisionRequest(String cropId, double quantity, String quantityUnit, List<String> candidateMarketIds) {
        this.cropId = cropId;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.candidateMarketIds = candidateMarketIds;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public List<String> getCandidateMarketIds() {
        return candidateMarketIds;
    }

    public void setCandidateMarketIds(List<String> candidateMarketIds) {
        this.candidateMarketIds = candidateMarketIds;
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

    public SellingCostsDto getCustomSellingCosts() {
        return customSellingCosts;
    }

    public void setCustomSellingCosts(SellingCostsDto customSellingCosts) {
        this.customSellingCosts = customSellingCosts;
    }

    public String getPriceBasis() {
        return priceBasis;
    }

    public void setPriceBasis(String priceBasis) {
        this.priceBasis = priceBasis;
    }

    public String getForecastHorizon() {
        return forecastHorizon;
    }

    public void setForecastHorizon(String forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
    }
}
