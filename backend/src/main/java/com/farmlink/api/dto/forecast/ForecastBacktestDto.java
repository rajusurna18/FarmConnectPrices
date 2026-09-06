package com.farmlink.api.dto.forecast;

public class ForecastBacktestDto {

    private int sampleCount;
    private Double mae; // Mean Absolute Error
    private Double mape; // Mean Absolute Percentage Error (null if invalid/zero actuals)
    private String horizonEvaluated;

    public ForecastBacktestDto() {}

    public ForecastBacktestDto(int sampleCount, Double mae, Double mape, String horizonEvaluated) {
        this.sampleCount = sampleCount;
        this.mae = mae;
        this.mape = mape;
        this.horizonEvaluated = horizonEvaluated;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Double getMae() {
        return mae;
    }

    public void setMae(Double mae) {
        this.mae = mae;
    }

    public Double getMape() {
        return mape;
    }

    public void setMape(Double mape) {
        this.mape = mape;
    }

    public String getHorizonEvaluated() {
        return horizonEvaluated;
    }

    public void setHorizonEvaluated(String horizonEvaluated) {
        this.horizonEvaluated = horizonEvaluated;
    }
}
