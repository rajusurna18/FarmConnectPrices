package com.farmlink.api.dto;

import java.math.BigDecimal;

public class OfferRoundResponse {
    private int roundNumber;
    private String senderUid;
    private String senderRole;
    private String action;
    private Double quantity;
    private String quantityUnit;
    private BigDecimal price;
    private String priceUnit;
    private String message;
    private String timestamp;

    public OfferRoundResponse() {}

    public OfferRoundResponse(int roundNumber, String senderUid, String senderRole, String action,
                              Double quantity, String quantityUnit, BigDecimal price, String priceUnit,
                              String message, String timestamp) {
        this.roundNumber = roundNumber;
        this.senderUid = senderUid;
        this.senderRole = senderRole;
        this.action = action;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.price = price;
        this.priceUnit = priceUnit;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
