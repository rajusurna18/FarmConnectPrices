package com.farmlink.api.dto;

import java.math.BigDecimal;

public class OrderItemResponse {
    private String orderItemId;
    private String orderId;
    private String listingId;
    private String cropId;
    private String cropName;
    private Double quantity;
    private String quantityUnit;
    private BigDecimal agreedUnitPrice;
    private String priceUnit;
    private BigDecimal lineTotal;

    public OrderItemResponse() {}

    public OrderItemResponse(String orderItemId, String orderId, String listingId, String cropId,
                             String cropName, Double quantity, String quantityUnit,
                             BigDecimal agreedUnitPrice, String priceUnit, BigDecimal lineTotal) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.listingId = listingId;
        this.cropId = cropId;
        this.cropName = cropName;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.agreedUnitPrice = agreedUnitPrice;
        this.priceUnit = priceUnit;
        this.lineTotal = lineTotal;
    }

    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public BigDecimal getAgreedUnitPrice() { return agreedUnitPrice; }
    public void setAgreedUnitPrice(BigDecimal agreedUnitPrice) { this.agreedUnitPrice = agreedUnitPrice; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
