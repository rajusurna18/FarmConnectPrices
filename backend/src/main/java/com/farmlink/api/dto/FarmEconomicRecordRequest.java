package com.farmlink.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FarmEconomicRecordRequest {
    private String farmId;
    private String cropId;
    private String season;
    private BigDecimal cultivatedArea;
    private String cultivatedAreaUnit; // ACRE, HECTARE
    private BigDecimal expectedYield;
    private String yieldUnit; // KG, QUINTAL, TON
    private List<ProductionCostItemDto> productionCosts = new ArrayList<>();
    private SellingCostsDto sellingCosts = new SellingCostsDto();

    public FarmEconomicRecordRequest() {}

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public BigDecimal getCultivatedArea() {
        return cultivatedArea;
    }

    public void setCultivatedArea(BigDecimal cultivatedArea) {
        this.cultivatedArea = cultivatedArea;
    }

    public String getCultivatedAreaUnit() {
        return cultivatedAreaUnit;
    }

    public void setCultivatedAreaUnit(String cultivatedAreaUnit) {
        this.cultivatedAreaUnit = cultivatedAreaUnit;
    }

    public BigDecimal getExpectedYield() {
        return expectedYield;
    }

    public void setExpectedYield(BigDecimal expectedYield) {
        this.expectedYield = expectedYield;
    }

    public String getYieldUnit() {
        return yieldUnit;
    }

    public void setYieldUnit(String yieldUnit) {
        this.yieldUnit = yieldUnit;
    }

    public List<ProductionCostItemDto> getProductionCosts() {
        return productionCosts;
    }

    public void setProductionCosts(List<ProductionCostItemDto> productionCosts) {
        this.productionCosts = productionCosts != null ? productionCosts : new ArrayList<>();
    }

    public SellingCostsDto getSellingCosts() {
        return sellingCosts;
    }

    public void setSellingCosts(SellingCostsDto sellingCosts) {
        this.sellingCosts = sellingCosts != null ? sellingCosts : new SellingCostsDto();
    }
}
