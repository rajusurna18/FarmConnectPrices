package com.farmlink.api.dto;

public class MarketPriceSummaryResponse {
    private String id;
    private String marketId;
    private String marketName;
    private String cropId;
    private String cropName;
    private String priceDate;
    private double minPrice;
    private double maxPrice;
    private double modalPrice;
    private String currency;
    private String unit;
    private String sourceUnit;
    private boolean conversionApplied;
    private double conversionFactor = 1.0;
    private String sourceType;
    private String sourceName;
    private String qualityStatus;
    private String status;

    public MarketPriceSummaryResponse() {}

    public MarketPriceSummaryResponse(
            String id,
            String marketId,
            String marketName,
            String cropId,
            String cropName,
            String priceDate,
            double minPrice,
            double maxPrice,
            double modalPrice,
            String currency,
            String unit,
            String sourceType,
            String sourceName,
            String qualityStatus,
            String status
    ) {
        this(id, marketId, marketName, cropId, cropName, priceDate, minPrice, maxPrice, modalPrice, currency, unit, unit, false, 1.0, sourceType, sourceName, qualityStatus, status);
    }

    public MarketPriceSummaryResponse(
            String id,
            String marketId,
            String marketName,
            String cropId,
            String cropName,
            String priceDate,
            double minPrice,
            double maxPrice,
            double modalPrice,
            String currency,
            String unit,
            String sourceUnit,
            boolean conversionApplied,
            double conversionFactor,
            String sourceType,
            String sourceName,
            String qualityStatus,
            String status
    ) {
        this.id = id;
        this.marketId = marketId;
        this.marketName = marketName;
        this.cropId = cropId;
        this.cropName = cropName;
        this.priceDate = priceDate;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
        this.currency = currency;
        this.unit = unit;
        this.sourceUnit = sourceUnit != null ? sourceUnit : unit;
        this.conversionApplied = conversionApplied;
        this.conversionFactor = conversionFactor;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.qualityStatus = qualityStatus;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMarketId() { return marketId; }
    public void setMarketId(String marketId) { this.marketId = marketId; }

    public String getMarketName() { return marketName; }
    public void setMarketName(String marketName) { this.marketName = marketName; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getPriceDate() { return priceDate; }
    public void setPriceDate(String priceDate) { this.priceDate = priceDate; }

    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }

    public double getModalPrice() { return modalPrice; }
    public void setModalPrice(double modalPrice) { this.modalPrice = modalPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSourceUnit() { return sourceUnit; }
    public void setSourceUnit(String sourceUnit) { this.sourceUnit = sourceUnit; }

    public boolean isConversionApplied() { return conversionApplied; }
    public void setConversionApplied(boolean conversionApplied) { this.conversionApplied = conversionApplied; }

    public double getConversionFactor() { return conversionFactor; }
    public void setConversionFactor(double conversionFactor) { this.conversionFactor = conversionFactor; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
