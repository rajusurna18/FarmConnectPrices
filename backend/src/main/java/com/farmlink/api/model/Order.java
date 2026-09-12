package com.farmlink.api.model;

import java.math.BigDecimal;

/**
 * Domain entity representing a Marketplace Order stored in Firestore collection 'orders'.
 */
public class Order {
    private String orderId;
    private String offerId;
    private String listingId;
    private String farmerId;
    private String buyerUid;
    private String buyerRole; // MEDIATOR_BUYER or CUSTOMER

    private String cropId;
    private String cropName;

    private String status; // PENDING, CONFIRMED, PROCESSING, COMPLETED, CANCELLED

    private Double totalQuantity;
    private String quantityUnit;

    private BigDecimal agreedPrice;
    private String priceUnit;

    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal otherCost;
    private BigDecimal totalAmount;
    private String currency; // "INR"

    private OrderItem item;

    private String cancellationReason;
    private String cancelledByUid;

    private String createdAt;
    private String updatedAt;
    private String confirmedAt;
    private String processedAt;
    private String completedAt;
    private String cancelledAt;

    public Order() {}

    public Order(String orderId, String offerId, String listingId, String farmerId, String buyerUid,
                 String buyerRole, String cropId, String cropName, String status, Double totalQuantity,
                 String quantityUnit, BigDecimal agreedPrice, String priceUnit, BigDecimal subtotal,
                 BigDecimal shippingCost, BigDecimal otherCost, BigDecimal totalAmount, String currency,
                 OrderItem item, String cancellationReason, String cancelledByUid, String createdAt,
                 String updatedAt, String confirmedAt, String processedAt, String completedAt, String cancelledAt) {
        this.orderId = orderId;
        this.offerId = offerId;
        this.listingId = listingId;
        this.farmerId = farmerId;
        this.buyerUid = buyerUid;
        this.buyerRole = buyerRole;
        this.cropId = cropId;
        this.cropName = cropName;
        this.status = status;
        this.totalQuantity = totalQuantity;
        this.quantityUnit = quantityUnit;
        this.agreedPrice = agreedPrice;
        this.priceUnit = priceUnit;
        this.subtotal = subtotal;
        this.shippingCost = shippingCost;
        this.otherCost = otherCost;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.item = item;
        this.cancellationReason = cancellationReason;
        this.cancelledByUid = cancelledByUid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmedAt = confirmedAt;
        this.processedAt = processedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getBuyerUid() { return buyerUid; }
    public void setBuyerUid(String buyerUid) { this.buyerUid = buyerUid; }

    public String getBuyerRole() { return buyerRole; }
    public void setBuyerRole(String buyerRole) { this.buyerRole = buyerRole; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Double totalQuantity) { this.totalQuantity = totalQuantity; }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public BigDecimal getAgreedPrice() { return agreedPrice; }
    public void setAgreedPrice(BigDecimal agreedPrice) { this.agreedPrice = agreedPrice; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal otherCost) { this.otherCost = otherCost; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public OrderItem getItem() { return item; }
    public void setItem(OrderItem item) { this.item = item; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getCancelledByUid() { return cancelledByUid; }
    public void setCancelledByUid(String cancelledByUid) { this.cancelledByUid = cancelledByUid; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(String confirmedAt) { this.confirmedAt = confirmedAt; }

    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
}
