package com.farmlink.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FarmEconomicRecordResponse {
    private String id;
    private String ownerUid;
    private String farmId;
    private String cropId;
    private String farmName;
    private String cropName;
    private String season;
    private BigDecimal cultivatedArea;
    private String cultivatedAreaUnit;
    private BigDecimal expectedYield;
    private String yieldUnit;
    private List<ProductionCostItemDto> productionCosts = new ArrayList<>();
    private SellingCostsDto sellingCosts = new SellingCostsDto();

    private BigDecimal totalProductionCost;
    private BigDecimal totalSellingCost;
    private BigDecimal totalCost;
    private BigDecimal productionCostPerUnit;
    private BigDecimal totalCostPerUnit;
    private BigDecimal breakEvenSellingPrice;

    private String createdAt;
    private String updatedAt;

    public FarmEconomicRecordResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

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

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
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

    public BigDecimal getTotalProductionCost() {
        return totalProductionCost;
    }

    public void setTotalProductionCost(BigDecimal totalProductionCost) {
        this.totalProductionCost = totalProductionCost;
    }

    public BigDecimal getTotalSellingCost() {
        return totalSellingCost;
    }

    public void setTotalSellingCost(BigDecimal totalSellingCost) {
        this.totalSellingCost = totalSellingCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getProductionCostPerUnit() {
        return productionCostPerUnit;
    }

    public void setProductionCostPerUnit(BigDecimal productionCostPerUnit) {
        this.productionCostPerUnit = productionCostPerUnit;
    }

    public BigDecimal getTotalCostPerUnit() {
        return totalCostPerUnit;
    }

    public void setTotalCostPerUnit(BigDecimal totalCostPerUnit) {
        this.totalCostPerUnit = totalCostPerUnit;
    }

    public BigDecimal getBreakEvenSellingPrice() {
        return breakEvenSellingPrice;
    }

    public void setBreakEvenSellingPrice(BigDecimal breakEvenSellingPrice) {
        this.breakEvenSellingPrice = breakEvenSellingPrice;
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
