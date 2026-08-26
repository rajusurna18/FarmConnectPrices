package com.farmlink.api.dto;

public class MarketIntelligenceSummaryResponse {

    private int observationCount;
    private double latestModalPrice;
    private double minObservedModalPrice;
    private double maxObservedModalPrice;
    private double averageModalPrice;
    private String firstObservationDate;
    private String latestObservationDate;
    private double absoluteChange;
    private Double percentageChange; // Nullable if first price is 0
    private String trendDirection; // INCREASING, DECREASING, STABLE, INSUFFICIENT_DATA
    private String currency;
    private String unit;

    public MarketIntelligenceSummaryResponse() {
    }

    public MarketIntelligenceSummaryResponse(int observationCount, double latestModalPrice,
                                            double minObservedModalPrice, double maxObservedModalPrice,
                                            double averageModalPrice, String firstObservationDate,
                                            String latestObservationDate, double absoluteChange,
                                            Double percentageChange, String trendDirection,
                                            String currency, String unit) {
        this.observationCount = observationCount;
        this.latestModalPrice = latestModalPrice;
        this.minObservedModalPrice = minObservedModalPrice;
        this.maxObservedModalPrice = maxObservedModalPrice;
        this.averageModalPrice = averageModalPrice;
        this.firstObservationDate = firstObservationDate;
        this.latestObservationDate = latestObservationDate;
        this.absoluteChange = absoluteChange;
        this.percentageChange = percentageChange;
        this.trendDirection = trendDirection;
        this.currency = currency;
        this.unit = unit;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(int observationCount) {
        this.observationCount = observationCount;
    }

    public double getLatestModalPrice() {
        return latestModalPrice;
    }

    public void setLatestModalPrice(double latestModalPrice) {
        this.latestModalPrice = latestModalPrice;
    }

    public double getMinObservedModalPrice() {
        return minObservedModalPrice;
    }

    public void setMinObservedModalPrice(double minObservedModalPrice) {
        this.minObservedModalPrice = minObservedModalPrice;
    }

    public double getMaxObservedModalPrice() {
        return maxObservedModalPrice;
    }

    public void setMaxObservedModalPrice(double maxObservedModalPrice) {
        this.maxObservedModalPrice = maxObservedModalPrice;
    }

    public double getAverageModalPrice() {
        return averageModalPrice;
    }

    public void setAverageModalPrice(double averageModalPrice) {
        this.averageModalPrice = averageModalPrice;
    }

    public String getFirstObservationDate() {
        return firstObservationDate;
    }

    public void setFirstObservationDate(String firstObservationDate) {
        this.firstObservationDate = firstObservationDate;
    }

    public String getLatestObservationDate() {
        return latestObservationDate;
    }

    public void setLatestObservationDate(String latestObservationDate) {
        this.latestObservationDate = latestObservationDate;
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

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
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
}
