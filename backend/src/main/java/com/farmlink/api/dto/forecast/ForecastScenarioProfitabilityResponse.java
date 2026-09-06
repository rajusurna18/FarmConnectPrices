package com.farmlink.api.dto.forecast;

import com.farmlink.api.dto.FarmProfitabilityEvaluationResponse;

public class ForecastScenarioProfitabilityResponse {

    private String farmEconomicRecordId;
    private Double currentPriceUsed;
    private Double forecastPriceUsed;
    private Double priceDifference;
    private Double priceDifferencePercentage;
    private boolean isScenario = true;
    private String scenarioTag = "SCENARIO";
    private FarmProfitabilityEvaluationResponse currentEvaluation;
    private FarmProfitabilityEvaluationResponse scenarioEvaluation;
    private Double scenarioRevenueDelta;
    private Double scenarioProfitDelta;
    private String disclaimer = "This is a read-only scenario profitability projection using estimated forecast prices. It does not alter your farm's recorded economic figures.";

    public ForecastScenarioProfitabilityResponse() {}

    public String getFarmEconomicRecordId() {
        return farmEconomicRecordId;
    }

    public void setFarmEconomicRecordId(String farmEconomicRecordId) {
        this.farmEconomicRecordId = farmEconomicRecordId;
    }

    public Double getCurrentPriceUsed() {
        return currentPriceUsed;
    }

    public void setCurrentPriceUsed(Double currentPriceUsed) {
        this.currentPriceUsed = currentPriceUsed;
    }

    public Double getForecastPriceUsed() {
        return forecastPriceUsed;
    }

    public void setForecastPriceUsed(Double forecastPriceUsed) {
        this.forecastPriceUsed = forecastPriceUsed;
    }

    public Double getPriceDifference() {
        return priceDifference;
    }

    public void setPriceDifference(Double priceDifference) {
        this.priceDifference = priceDifference;
    }

    public Double getPriceDifferencePercentage() {
        return priceDifferencePercentage;
    }

    public void setPriceDifferencePercentage(Double priceDifferencePercentage) {
        this.priceDifferencePercentage = priceDifferencePercentage;
    }

    public boolean isScenario() {
        return isScenario;
    }

    public void setScenario(boolean scenario) {
        isScenario = scenario;
    }

    public String getScenarioTag() {
        return scenarioTag;
    }

    public void setScenarioTag(String scenarioTag) {
        this.scenarioTag = scenarioTag;
    }

    public FarmProfitabilityEvaluationResponse getCurrentEvaluation() {
        return currentEvaluation;
    }

    public void setCurrentEvaluation(FarmProfitabilityEvaluationResponse currentEvaluation) {
        this.currentEvaluation = currentEvaluation;
    }

    public FarmProfitabilityEvaluationResponse getScenarioEvaluation() {
        return scenarioEvaluation;
    }

    public void setScenarioEvaluation(FarmProfitabilityEvaluationResponse scenarioEvaluation) {
        this.scenarioEvaluation = scenarioEvaluation;
    }

    public Double getScenarioRevenueDelta() {
        return scenarioRevenueDelta;
    }

    public void setScenarioRevenueDelta(Double scenarioRevenueDelta) {
        this.scenarioRevenueDelta = scenarioRevenueDelta;
    }

    public Double getScenarioProfitDelta() {
        return scenarioProfitDelta;
    }

    public void setScenarioProfitDelta(Double scenarioProfitDelta) {
        this.scenarioProfitDelta = scenarioProfitDelta;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
