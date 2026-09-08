package com.farmlink.api.dto;

/**
 * Read-only reference metadata providing verified market price context for comparison.
 * The seller's asking price remains completely separate and farmer-entered.
 */
public class MarketPriceReferenceDto {
    private Double verifiedModalPrice;
    private Double verifiedMinPrice;
    private Double verifiedMaxPrice;
    private String priceUnit;
    private String marketName;
    private String date;
    private String note;

    public MarketPriceReferenceDto() {
        this.note = "Reference market price is provided for informational comparison only. Asking price is farmer-specified.";
    }

    public MarketPriceReferenceDto(Double verifiedModalPrice, Double verifiedMinPrice, Double verifiedMaxPrice,
                                   String priceUnit, String marketName, String date) {
        this.verifiedModalPrice = verifiedModalPrice;
        this.verifiedMinPrice = verifiedMinPrice;
        this.verifiedMaxPrice = verifiedMaxPrice;
        this.priceUnit = priceUnit;
        this.marketName = marketName;
        this.date = date;
        this.note = "Reference market price is provided for informational comparison only. Asking price is farmer-specified.";
    }

    public Double getVerifiedModalPrice() { return verifiedModalPrice; }
    public void setVerifiedModalPrice(Double verifiedModalPrice) { this.verifiedModalPrice = verifiedModalPrice; }

    public Double getVerifiedMinPrice() { return verifiedMinPrice; }
    public void setVerifiedMinPrice(Double verifiedMinPrice) { this.verifiedMinPrice = verifiedMinPrice; }

    public Double getVerifiedMaxPrice() { return verifiedMaxPrice; }
    public void setVerifiedMaxPrice(Double verifiedMaxPrice) { this.verifiedMaxPrice = verifiedMaxPrice; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public String getMarketName() { return marketName; }
    public void setMarketName(String marketName) { this.marketName = marketName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
