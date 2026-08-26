package com.farmlink.api.dto;

public class MarketComparisonItemDto {

    private String marketId;
    private String marketName;
    private String state;
    private String district;
    private String mandal;
    private double minPrice;
    private double maxPrice;
    private double modalPrice;
    private String currency;
    private String unit;
    private String priceDate;
    private String sourceName;
    private String qualityStatus;

    public MarketComparisonItemDto() {
    }

    public MarketComparisonItemDto(String marketId, String marketName, String state, String district,
                                 String mandal, double minPrice, double maxPrice, double modalPrice,
                                 String currency, String unit, String priceDate, String sourceName,
                                 String qualityStatus) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.state = state;
        this.district = district;
        this.mandal = mandal;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
        this.currency = currency;
        this.unit = unit;
        this.priceDate = priceDate;
        this.sourceName = sourceName;
        this.qualityStatus = qualityStatus;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getMandal() {
        return mandal;
    }

    public void setMandal(String mandal) {
        this.mandal = mandal;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public double getModalPrice() {
        return modalPrice;
    }

    public void setModalPrice(double modalPrice) {
        this.modalPrice = modalPrice;
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

    public String getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(String priceDate) {
        this.priceDate = priceDate;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }
}
