package com.farmlink.api.dto;

import java.math.BigDecimal;

public class SellingCostsDto {
    private BigDecimal transportationCost = BigDecimal.ZERO;
    private BigDecimal otherSellingCosts = BigDecimal.ZERO;

    public SellingCostsDto() {}

    public SellingCostsDto(BigDecimal transportationCost, BigDecimal otherSellingCosts) {
        this.transportationCost = transportationCost != null ? transportationCost : BigDecimal.ZERO;
        this.otherSellingCosts = otherSellingCosts != null ? otherSellingCosts : BigDecimal.ZERO;
    }

    public SellingCostsDto(double transportationCost, double otherSellingCosts) {
        this(BigDecimal.valueOf(transportationCost), BigDecimal.valueOf(otherSellingCosts));
    }

    public BigDecimal getTransportationCost() {
        return transportationCost;
    }

    public void setTransportationCost(BigDecimal transportationCost) {
        this.transportationCost = transportationCost != null ? transportationCost : BigDecimal.ZERO;
    }

    public BigDecimal getOtherSellingCosts() {
        return otherSellingCosts;
    }

    public void setOtherSellingCosts(BigDecimal otherSellingCosts) {
        this.otherSellingCosts = otherSellingCosts != null ? otherSellingCosts : BigDecimal.ZERO;
    }
}
