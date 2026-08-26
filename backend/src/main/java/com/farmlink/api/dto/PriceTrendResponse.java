package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class PriceTrendResponse {

    private String cropId;
    private String cropName;
    private String marketId;
    private String marketName;
    private String currency;
    private String unit;
    private List<PriceTrendPointDto> points = new ArrayList<>();
    private String trendDirection; // INCREASING, DECREASING, STABLE, INSUFFICIENT_DATA
    private double absoluteChange;
    private Double percentageChange; // Nullable if starting price is 0

    public PriceTrendResponse() {
    }

    public PriceTrendResponse(String cropId, String cropName, String marketId, String marketName,
                              String currency, String unit, List<PriceTrendPointDto> points,
                              String trendDirection, double absoluteChange, Double percentageChange) {
        this.cropId = cropId;
        this.cropName = cropName;
        this.marketId = marketId;
        this.marketName = marketName;
        this.currency = currency;
        this.unit = unit;
        this.points = points != null ? points : new ArrayList<>();
        this.trendDirection = trendDirection;
        this.absoluteChange = absoluteChange;
        this.percentageChange = percentageChange;
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

    public List<PriceTrendPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<PriceTrendPointDto> points) {
        this.points = points != null ? points : new ArrayList<>();
    }

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public double getAbsoluteChange() {
        return absoluteChange;
    }

    public void setAbsoluteChange(double absoluteChange) {
        this.absoluteChange = absoluteChange;
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(Double percentageChange) {
        this.percentageChange = percentageChange;
    }
}
