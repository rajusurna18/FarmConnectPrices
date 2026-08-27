package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class MarketComparisonResponse {

    private CropResponse crop;
    private String date;
    private String currency;
    private String unit;
    private List<MarketComparisonItemDto> markets = new ArrayList<>();
    private MarketComparisonItemDto highestMarket;
    private MarketComparisonItemDto lowestMarket;
    private double priceDifference;
    private Double percentageDifference; // Nullable if lowest price is 0
    private String comparisonScope = "COMMODITY_LEVEL";
    private String observationSummary;

    public MarketComparisonResponse() {
    }

    public MarketComparisonResponse(CropResponse crop, String date, String currency, String unit,
                                  List<MarketComparisonItemDto> markets, MarketComparisonItemDto highestMarket,
                                  MarketComparisonItemDto lowestMarket, double priceDifference,
                                  Double percentageDifference) {
        this(crop, date, currency, unit, markets, highestMarket, lowestMarket, priceDifference, percentageDifference, "COMMODITY_LEVEL", null);
    }

    public MarketComparisonResponse(CropResponse crop, String date, String currency, String unit,
                                  List<MarketComparisonItemDto> markets, MarketComparisonItemDto highestMarket,
                                  MarketComparisonItemDto lowestMarket, double priceDifference,
                                  Double percentageDifference, String comparisonScope, String observationSummary) {
        this.crop = crop;
        this.date = date;
        this.currency = currency;
        this.unit = unit;
        this.markets = markets != null ? markets : new ArrayList<>();
        this.highestMarket = highestMarket;
        this.lowestMarket = lowestMarket;
        this.priceDifference = priceDifference;
        this.percentageDifference = percentageDifference;
        this.comparisonScope = comparisonScope != null ? comparisonScope : "COMMODITY_LEVEL";
        this.observationSummary = observationSummary;
    }

    public CropResponse getCrop() {
        return crop;
    }

    public void setCrop(CropResponse crop) {
        this.crop = crop;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public List<MarketComparisonItemDto> getMarkets() {
        return markets;
    }

    public void setMarkets(List<MarketComparisonItemDto> markets) {
        this.markets = markets != null ? markets : new ArrayList<>();
    }

    public MarketComparisonItemDto getHighestMarket() {
        return highestMarket;
    }

    public void setHighestMarket(MarketComparisonItemDto highestMarket) {
        this.highestMarket = highestMarket;
    }

    public MarketComparisonItemDto getLowestMarket() {
        return lowestMarket;
    }

    public void setLowestMarket(MarketComparisonItemDto lowestMarket) {
        this.lowestMarket = lowestMarket;
    }

    public double getPriceDifference() {
        return priceDifference;
    }

    public void setPriceDifference(double priceDifference) {
        this.priceDifference = priceDifference;
    }

    public Double getPercentageDifference() {
        return percentageDifference;
    }

    public void setPercentageDifference(Double percentageDifference) {
        this.percentageDifference = percentageDifference;
    }

    public String getComparisonScope() {
        return comparisonScope;
    }

    public void setComparisonScope(String comparisonScope) {
        this.comparisonScope = comparisonScope;
    }

    public String getObservationSummary() {
        return observationSummary;
    }

    public void setObservationSummary(String observationSummary) {
        this.observationSummary = observationSummary;
    }
}

