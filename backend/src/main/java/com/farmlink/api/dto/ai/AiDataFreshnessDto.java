package com.farmlink.api.dto.ai;

public class AiDataFreshnessDto {
    private String marketPriceDate;
    private String status; // CURRENT, STALE, MISSING
    private String lastUpdatedIso;
    private boolean stalePrice;
    private String staleMessage;

    public AiDataFreshnessDto() {}

    public AiDataFreshnessDto(String marketPriceDate, String status, String lastUpdatedIso, boolean stalePrice, String staleMessage) {
        this.marketPriceDate = marketPriceDate;
        this.status = status;
        this.lastUpdatedIso = lastUpdatedIso;
        this.stalePrice = stalePrice;
        this.staleMessage = staleMessage;
    }

    public String getMarketPriceDate() {
        return marketPriceDate;
    }

    public void setMarketPriceDate(String marketPriceDate) {
        this.marketPriceDate = marketPriceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdatedIso() {
        return lastUpdatedIso;
    }

    public void setLastUpdatedIso(String lastUpdatedIso) {
        this.lastUpdatedIso = lastUpdatedIso;
    }

    public boolean isStalePrice() {
        return stalePrice;
    }

    public void setStalePrice(boolean stalePrice) {
        this.stalePrice = stalePrice;
    }

    public String getStaleMessage() {
        return staleMessage;
    }

    public void setStaleMessage(String staleMessage) {
        this.staleMessage = staleMessage;
    }
}
