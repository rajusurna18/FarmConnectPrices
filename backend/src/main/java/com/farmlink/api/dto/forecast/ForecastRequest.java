package com.farmlink.api.dto.forecast;

public class ForecastRequest {

    private String cropId;
    private String marketId;
    private String horizon; // e.g. "7_DAYS", "1_DAY", "3_DAYS", "14_DAYS"
    private Integer lookbackDays; // Default 30, min 7, max 90
    private String unit; // e.g. "QUINTAL", "KG"
    private String model; // e.g. "WEIGHTED_MOVING_AVERAGE_V1"

    public ForecastRequest() {}

    public ForecastRequest(String cropId, String marketId, String horizon, Integer lookbackDays, String unit) {
        this.cropId = cropId;
        this.marketId = marketId;
        this.horizon = horizon;
        this.lookbackDays = lookbackDays;
        this.unit = unit;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getHorizon() {
        return horizon;
    }

    public void setHorizon(String horizon) {
        this.horizon = horizon;
    }

    public Integer getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(Integer lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
