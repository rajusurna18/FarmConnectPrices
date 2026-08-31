package com.farmlink.api.dto;

public class MarketCostInputDto {
    private String marketId;
    private Double transportationCost = 0.0;
    private Double otherSellingCosts = 0.0;

    public MarketCostInputDto() {}

    public MarketCostInputDto(String marketId, Double transportationCost, Double otherSellingCosts) {
        this.marketId = marketId;
        this.transportationCost = transportationCost != null ? transportationCost : 0.0;
        this.otherSellingCosts = otherSellingCosts != null ? otherSellingCosts : 0.0;
    }

    public String getMarketId() { return marketId; }
    public void setMarketId(String marketId) { this.marketId = marketId; }

    public Double getTransportationCost() { return transportationCost; }
    public void setTransportationCost(Double transportationCost) { this.transportationCost = transportationCost; }

    public Double getOtherSellingCosts() { return otherSellingCosts; }
    public void setOtherSellingCosts(Double otherSellingCosts) { this.otherSellingCosts = otherSellingCosts; }
}
