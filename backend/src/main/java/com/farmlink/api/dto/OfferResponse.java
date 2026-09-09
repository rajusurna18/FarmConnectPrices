package com.farmlink.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class OfferResponse {
    private String offerId;
    private String listingId;
    private String farmerId;
    private String buyerUid;
    private String buyerRole;

    private String cropId;
    private String cropName;

    private Double offeredQuantity;
    private String quantityUnit;

    private BigDecimal offeredPrice;
    private String priceUnit;

    private BigDecimal askingPriceReference;

    private String message;
    private String status;

    private int roundNumber;
    private String currentResponderUid;

    private Double agreedQuantity;
    private BigDecimal agreedPrice;
    private String agreedPriceUnit;

    private List<OfferRoundResponse> rounds;

    private String createdAt;
    private String updatedAt;
    private String expiresAt;
    private String respondedAt;

    private boolean canAccept;
    private boolean canReject;
    private boolean canCounter;
    private boolean canCancel;

    public OfferResponse() {}

    public OfferResponse(String offerId, String listingId, String farmerId, String buyerUid, String buyerRole,
                         String cropId, String cropName, Double offeredQuantity, String quantityUnit,
                         BigDecimal offeredPrice, String priceUnit, BigDecimal askingPriceReference, String message,
                         String status, int roundNumber, String currentResponderUid, Double agreedQuantity,
                         BigDecimal agreedPrice, String agreedPriceUnit, List<OfferRoundResponse> rounds,
                         String createdAt, String updatedAt, String expiresAt, String respondedAt,
                         boolean canAccept, boolean canReject, boolean canCounter, boolean canCancel) {
        this.offerId = offerId;
        this.listingId = listingId;
        this.farmerId = farmerId;
        this.buyerUid = buyerUid;
        this.buyerRole = buyerRole;
        this.cropId = cropId;
        this.cropName = cropName;
        this.offeredQuantity = offeredQuantity;
        this.quantityUnit = quantityUnit;
        this.offeredPrice = offeredPrice;
        this.priceUnit = priceUnit;
        this.askingPriceReference = askingPriceReference;
        this.message = message;
        this.status = status;
        this.roundNumber = roundNumber;
        this.currentResponderUid = currentResponderUid;
        this.agreedQuantity = agreedQuantity;
        this.agreedPrice = agreedPrice;
        this.agreedPriceUnit = agreedPriceUnit;
        this.rounds = rounds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.respondedAt = respondedAt;
        this.canAccept = canAccept;
        this.canReject = canReject;
        this.canCounter = canCounter;
        this.canCancel = canCancel;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getBuyerUid() {
        return buyerUid;
    }

    public void setBuyerUid(String buyerUid) {
        this.buyerUid = buyerUid;
    }

    public String getBuyerRole() {
        return buyerRole;
    }

    public void setBuyerRole(String buyerRole) {
        this.buyerRole = buyerRole;
    }

    public String getCropId() {
        return cropId;
    }

    public void setCropId(String cropId) {
        this.cropId = cropId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public Double getOfferedQuantity() {
        return offeredQuantity;
    }

    public void setOfferedQuantity(Double offeredQuantity) {
        this.offeredQuantity = offeredQuantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public BigDecimal getOfferedPrice() {
        return offeredPrice;
    }

    public void setOfferedPrice(BigDecimal offeredPrice) {
        this.offeredPrice = offeredPrice;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public BigDecimal getAskingPriceReference() {
        return askingPriceReference;
    }

    public void setAskingPriceReference(BigDecimal askingPriceReference) {
        this.askingPriceReference = askingPriceReference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getCurrentResponderUid() {
        return currentResponderUid;
    }

    public void setCurrentResponderUid(String currentResponderUid) {
        this.currentResponderUid = currentResponderUid;
    }

    public Double getAgreedQuantity() {
        return agreedQuantity;
    }

    public void setAgreedQuantity(Double agreedQuantity) {
        this.agreedQuantity = agreedQuantity;
    }

    public BigDecimal getAgreedPrice() {
        return agreedPrice;
    }

    public void setAgreedPrice(BigDecimal agreedPrice) {
        this.agreedPrice = agreedPrice;
    }

    public String getAgreedPriceUnit() {
        return agreedPriceUnit;
    }

    public void setAgreedPriceUnit(String agreedPriceUnit) {
        this.agreedPriceUnit = agreedPriceUnit;
    }

    public List<OfferRoundResponse> getRounds() {
        return rounds;
    }

    public void setRounds(List<OfferRoundResponse> rounds) {
        this.rounds = rounds;
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

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(String respondedAt) {
        this.respondedAt = respondedAt;
    }

    public boolean isCanAccept() {
        return canAccept;
    }

    public void setCanAccept(boolean canAccept) {
        this.canAccept = canAccept;
    }

    public boolean isCanReject() {
        return canReject;
    }

    public void setCanReject(boolean canReject) {
        this.canReject = canReject;
    }

    public boolean isCanCounter() {
        return canCounter;
    }

    public void setCanCounter(boolean canCounter) {
        this.canCounter = canCounter;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }
}
