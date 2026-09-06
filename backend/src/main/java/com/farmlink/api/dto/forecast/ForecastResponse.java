package com.farmlink.api.dto.forecast;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ForecastResponse {

    private String cropId;
    private String cropName;
    private String marketId;
    private String marketName;
    private String model; // e.g. WEIGHTED_MOVING_AVERAGE_V1
    private String horizon; // e.g. 7_DAYS
    private Double currentVerifiedPrice;
    private Double forecastPrice;
    private Double forecastLowerBound; // null if M_h < 5
    private Double forecastUpperBound; // null if M_h < 5
    private ForecastDirection direction; // UP, DOWN, STABLE, UNCERTAIN
    private ForecastConfidence confidence; // HIGH, MEDIUM, LOW, INSUFFICIENT_DATA
    private String dataQuality; // GOOD, LIMITED, INSUFFICIENT
    private int observationsUsed;
    private String latestObservationDate;
    private String unit;
    private String currency = "INR";
    private Instant generatedAt = Instant.now();
    private ForecastBacktestDto backtest;
    private List<String> limitations = new ArrayList<>();
    private String disclaimer = "Forecasts are empirical statistical estimates based on historical verified market-price data and may differ from actual future prices. Verify the latest market information before making a selling decision.";

    public ForecastResponse() {}

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getHorizon() {
        return horizon;
    }

    public void setHorizon(String horizon) {
        this.horizon = horizon;
    }

    public Double getCurrentVerifiedPrice() {
        return currentVerifiedPrice;
    }

    public void setCurrentVerifiedPrice(Double currentVerifiedPrice) {
        this.currentVerifiedPrice = currentVerifiedPrice;
    }

    public Double getForecastPrice() {
        return forecastPrice;
    }

    public void setForecastPrice(Double forecastPrice) {
        this.forecastPrice = forecastPrice;
    }

    public Double getForecastLowerBound() {
        return forecastLowerBound;
    }

    public void setForecastLowerBound(Double forecastLowerBound) {
        this.forecastLowerBound = forecastLowerBound;
    }

    public Double getForecastUpperBound() {
        return forecastUpperBound;
    }

    public void setForecastUpperBound(Double forecastUpperBound) {
        this.forecastUpperBound = forecastUpperBound;
    }

    public ForecastDirection getDirection() {
        return direction;
    }

    public void setDirection(ForecastDirection direction) {
        this.direction = direction;
    }

    public ForecastConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(ForecastConfidence confidence) {
        this.confidence = confidence;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    public int getObservationsUsed() {
        return observationsUsed;
    }

    public void setObservationsUsed(int observationsUsed) {
        this.observationsUsed = observationsUsed;
    }

    public String getLatestObservationDate() {
        return latestObservationDate;
    }

    public void setLatestObservationDate(String latestObservationDate) {
        this.latestObservationDate = latestObservationDate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public ForecastBacktestDto getBacktest() {
        return backtest;
    }

    public void setBacktest(ForecastBacktestDto backtest) {
        this.backtest = backtest;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
