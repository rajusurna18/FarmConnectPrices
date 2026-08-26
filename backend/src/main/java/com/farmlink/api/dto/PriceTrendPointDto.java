package com.farmlink.api.dto;

public class PriceTrendPointDto {

    private String priceDate;
    private double modalPrice;
    private double minPrice;
    private double maxPrice;

    public PriceTrendPointDto() {
    }

    public PriceTrendPointDto(String priceDate, double modalPrice, double minPrice, double maxPrice) {
        this.priceDate = priceDate;
        this.modalPrice = modalPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public String getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(String priceDate) {
        this.priceDate = priceDate;
    }

    public double getModalPrice() {
        return modalPrice;
    }

    public void setModalPrice(double modalPrice) {
        this.modalPrice = modalPrice;
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
}
