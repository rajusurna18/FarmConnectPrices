package com.farmlink.api.dto;

public class MarketPriceResponse {
    private String id;
    private MarketSummaryResponse market;
    private CropResponse crop;
    private String priceDate;
    private String observedAt;
    private double minPrice;
    private double maxPrice;
    private double modalPrice;
    private String currency;
    private String unit;
    private MarketPriceSourceDto source;
    private String qualityStatus;
    private String status;
    private String createdAt;
    private String updatedAt;

    public MarketPriceResponse() {}

    public MarketPriceResponse(
            String id,
            MarketSummaryResponse market,
            CropResponse crop,
            String priceDate,
            String observedAt,
            double minPrice,
            double maxPrice,
            double modalPrice,
            String currency,
            String unit,
            MarketPriceSourceDto source,
            String qualityStatus,
            String status,
            String createdAt,
            String updatedAt
    ) {
        this.id = id;
        this.market = market;
        this.crop = crop;
        this.priceDate = priceDate;
        this.observedAt = observedAt;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
        this.currency = currency;
        this.unit = unit;
        this.source = source;
        this.qualityStatus = qualityStatus;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MarketSummaryResponse getMarket() {
        return market;
    }

    public void setMarket(MarketSummaryResponse market) {
        this.market = market;
    }

    public CropResponse getCrop() {
        return crop;
    }

    public void setCrop(CropResponse crop) {
        this.crop = crop;
    }

    public String getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(String priceDate) {
        this.priceDate = priceDate;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(String observedAt) {
        this.observedAt = observedAt;
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

    public MarketPriceSourceDto getSource() {
        return source;
    }

    public void setSource(MarketPriceSourceDto source) {
        this.source = source;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
