package com.farmlink.api.dto;

import java.math.BigDecimal;

public class MarketEvaluationResponse {
    private String status; // SUCCESS, NO_VERIFIED_PRICE, UNIT_MISMATCH, INVALID_CROP, INVALID_MARKET, ERROR
    private String message;
    private CropResponse crop;
    private MarketSummaryResponse market;
    private BigDecimal selectedPrice;
    private String priceBasis;
    private String priceUnit;
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal grossRevenue;
    private BigDecimal transportationCost;
    private BigDecimal otherSellingCosts;
    private BigDecimal totalSellingCosts;
    private BigDecimal estimatedNetRealization;
    private BigDecimal netRealizationPerUnit;
    private BigDecimal sellingCostBreakEvenPrice;
    private String currency;
    private String priceDate;
    private String observedAt;
    private MarketPriceSourceDto source;
    private String qualityStatus;
    private boolean isStalePrice;
    private String staleMessage;
    private boolean isLoss;

    public MarketEvaluationResponse() {}

    public static MarketEvaluationResponse error(String status, String message) {
        MarketEvaluationResponse res = new MarketEvaluationResponse();
        res.setStatus(status);
        res.setMessage(message);
        return res;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public CropResponse getCrop() { return crop; }
    public void setCrop(CropResponse crop) { this.crop = crop; }

    public MarketSummaryResponse getMarket() { return market; }
    public void setMarket(MarketSummaryResponse market) { this.market = market; }

    public BigDecimal getSelectedPrice() { return selectedPrice; }
    public void setSelectedPrice(BigDecimal selectedPrice) { this.selectedPrice = selectedPrice; }

    public String getPriceBasis() { return priceBasis; }
    public void setPriceBasis(String priceBasis) { this.priceBasis = priceBasis; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(BigDecimal grossRevenue) { this.grossRevenue = grossRevenue; }

    public BigDecimal getTransportationCost() { return transportationCost; }
    public void setTransportationCost(BigDecimal transportationCost) { this.transportationCost = transportationCost; }

    public BigDecimal getOtherSellingCosts() { return otherSellingCosts; }
    public void setOtherSellingCosts(BigDecimal otherSellingCosts) { this.otherSellingCosts = otherSellingCosts; }

    public BigDecimal getTotalSellingCosts() { return totalSellingCosts; }
    public void setTotalSellingCosts(BigDecimal totalSellingCosts) { this.totalSellingCosts = totalSellingCosts; }

    public BigDecimal getEstimatedNetRealization() { return estimatedNetRealization; }
    public void setEstimatedNetRealization(BigDecimal estimatedNetRealization) { this.estimatedNetRealization = estimatedNetRealization; }

    public BigDecimal getNetRealizationPerUnit() { return netRealizationPerUnit; }
    public void setNetRealizationPerUnit(BigDecimal netRealizationPerUnit) { this.netRealizationPerUnit = netRealizationPerUnit; }

    public BigDecimal getSellingCostBreakEvenPrice() { return sellingCostBreakEvenPrice; }
    public void setSellingCostBreakEvenPrice(BigDecimal sellingCostBreakEvenPrice) { this.sellingCostBreakEvenPrice = sellingCostBreakEvenPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPriceDate() { return priceDate; }
    public void setPriceDate(String priceDate) { this.priceDate = priceDate; }

    public String getObservedAt() { return observedAt; }
    public void setObservedAt(String observedAt) { this.observedAt = observedAt; }

    public MarketPriceSourceDto getSource() { return source; }
    public void setSource(MarketPriceSourceDto source) { this.source = source; }

    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

    public boolean isStalePrice() { return isStalePrice; }
    public void setStalePrice(boolean stalePrice) { isStalePrice = stalePrice; }

    public String getStaleMessage() { return staleMessage; }
    public void setStaleMessage(String staleMessage) { this.staleMessage = staleMessage; }

    public boolean isLoss() { return isLoss; }
    public void setLoss(boolean loss) { isLoss = loss; }
}
