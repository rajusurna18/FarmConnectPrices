package com.farmlink.api.dto.ai;

import com.farmlink.api.dto.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiDecisionContext {
    private String farmerUid;
    private CropResponse crop;
    private FarmResponse farm;
    private FarmEconomicRecordResponse economicRecord;

    private List<MarketResponse> candidateMarkets = new ArrayList<>();
    private List<MarketPriceResponse> marketPrices = new ArrayList<>();

    private List<MarketEvaluationResponse> marketEvaluations = new ArrayList<>();
    private FarmProfitabilityEvaluationResponse profitabilityEvaluation;
    private FarmMarketComparisonResponse marketComparison;

    private BigDecimal quantity;
    private String quantityUnit = "QUINTAL";
    private BigDecimal transportationCost = BigDecimal.ZERO;
    private BigDecimal otherSellingCosts = BigDecimal.ZERO;

    private boolean hasVerifiedPrice = false;
    private boolean hasStalePrice = false;
    private String priceDate;
    private List<String> dataGaps = new ArrayList<>();

    public AiDecisionContext() {}

    public String getFarmerUid() {
        return farmerUid;
    }

    public void setFarmerUid(String farmerUid) {
        this.farmerUid = farmerUid;
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

    public FarmEconomicRecordResponse getEconomicRecord() {
        return economicRecord;
    }

    public void setEconomicRecord(FarmEconomicRecordResponse economicRecord) {
        this.economicRecord = economicRecord;
    }

    public List<MarketResponse> getCandidateMarkets() {
        return candidateMarkets;
    }

    public void setCandidateMarkets(List<MarketResponse> candidateMarkets) {
        this.candidateMarkets = candidateMarkets != null ? candidateMarkets : new ArrayList<>();
    }

    public List<MarketPriceResponse> getMarketPrices() {
        return marketPrices;
    }

    public void setMarketPrices(List<MarketPriceResponse> marketPrices) {
        this.marketPrices = marketPrices != null ? marketPrices : new ArrayList<>();
    }

    public List<MarketEvaluationResponse> getMarketEvaluations() {
        return marketEvaluations;
    }

    public void setMarketEvaluations(List<MarketEvaluationResponse> marketEvaluations) {
        this.marketEvaluations = marketEvaluations != null ? marketEvaluations : new ArrayList<>();
    }

    public FarmProfitabilityEvaluationResponse getProfitabilityEvaluation() {
        return profitabilityEvaluation;
    }

    public void setProfitabilityEvaluation(FarmProfitabilityEvaluationResponse profitabilityEvaluation) {
        this.profitabilityEvaluation = profitabilityEvaluation;
    }

    public FarmMarketComparisonResponse getMarketComparison() {
        return marketComparison;
    }

    public void setMarketComparison(FarmMarketComparisonResponse marketComparison) {
        this.marketComparison = marketComparison;
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

    public boolean isHasVerifiedPrice() {
        return hasVerifiedPrice;
    }

    public void setHasVerifiedPrice(boolean hasVerifiedPrice) {
        this.hasVerifiedPrice = hasVerifiedPrice;
    }

    public boolean isHasStalePrice() {
        return hasStalePrice;
    }

    public void setHasStalePrice(boolean hasStalePrice) {
        this.hasStalePrice = hasStalePrice;
    }

    public String getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(String priceDate) {
        this.priceDate = priceDate;
    }

    public List<String> getDataGaps() {
        return dataGaps;
    }

    public void setDataGaps(List<String> dataGaps) {
        this.dataGaps = dataGaps != null ? dataGaps : new ArrayList<>();
    }
}
