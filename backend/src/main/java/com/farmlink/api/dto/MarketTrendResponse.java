package com.farmlink.api.dto;

import com.farmlink.api.dto.ai.AiDecisionResponse;
import com.farmlink.api.service.MarketPriceService;

import java.util.ArrayList;
import java.util.List;

public class MarketTrendResponse {

    private String cropId;
    private String cropName;
    private String marketId;
    private String marketName;
    private String currency = MarketPriceService.CURRENCY_INR;
    private String unit = MarketPriceService.UNIT_QUINTAL;
    private String period;
    private String startDate;
    private String endDate;

    private int observationCount;
    private Double earliestPrice;
    private Double latestPrice;
    private Double minPrice;
    private Double maxPrice;
    private Double avgPrice;
    private Double priceRange;
    private Double absoluteChange;
    private Double percentageChange;

    private String trendDirection; // RISING, FALLING, STABLE, INSUFFICIENT_DATA
    private String volatility;     // LOW, MEDIUM, HIGH, INSUFFICIENT_DATA
    private Double volatilityCvPercent;
    private String dataQuality;   // GOOD, LIMITED, INSUFFICIENT
    private String freshnessStatus;// FRESH, STALE, UNAVAILABLE
    private String latestObservationDate;

    private String currentVsAverageStatement;
    private Double currentVsAveragePctDiff;

    private List<PriceTrendPointDto> points = new ArrayList<>();
    private AiDecisionResponse aiExplanation;

    public MarketTrendResponse() {
    }

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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(int observationCount) {
        this.observationCount = observationCount;
    }

    public Double getEarliestPrice() {
        return earliestPrice;
    }

    public void setEarliestPrice(Double earliestPrice) {
        this.earliestPrice = earliestPrice;
    }

    public Double getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(Double latestPrice) {
        this.latestPrice = latestPrice;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Double getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(Double avgPrice) {
        this.avgPrice = avgPrice;
    }

    public Double getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(Double priceRange) {
        this.priceRange = priceRange;
    }

    public Double getAbsoluteChange() {
        return absoluteChange;
    }

    public void setAbsoluteChange(Double absoluteChange) {
        this.absoluteChange = absoluteChange;
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(Double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public String getVolatility() {
        return volatility;
    }

    public void setVolatility(String volatility) {
        this.volatility = volatility;
    }

    public Double getVolatilityCvPercent() {
        return volatilityCvPercent;
    }

    public void setVolatilityCvPercent(Double volatilityCvPercent) {
        this.volatilityCvPercent = volatilityCvPercent;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getLatestObservationDate() {
        return latestObservationDate;
    }

    public void setLatestObservationDate(String latestObservationDate) {
        this.latestObservationDate = latestObservationDate;
    }

    public String getCurrentVsAverageStatement() {
        return currentVsAverageStatement;
    }

    public void setCurrentVsAverageStatement(String currentVsAverageStatement) {
        this.currentVsAverageStatement = currentVsAverageStatement;
    }

    public Double getCurrentVsAveragePctDiff() {
        return currentVsAveragePctDiff;
    }

    public void setCurrentVsAveragePctDiff(Double currentVsAveragePctDiff) {
        this.currentVsAveragePctDiff = currentVsAveragePctDiff;
    }

    public List<PriceTrendPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<PriceTrendPointDto> points) {
        this.points = points != null ? points : new ArrayList<>();
    }

    public AiDecisionResponse getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(AiDecisionResponse aiExplanation) {
        this.aiExplanation = aiExplanation;
    }
}
