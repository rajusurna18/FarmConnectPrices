package com.farmlink.api.model;

/**
 * Domain entity representing a Marketplace Transaction Dispute stored in Firestore collection 'disputes'.
 */
public class Dispute {
    private String disputeId;
    private String orderId;
    private String listingId;
    private String offerId;
    private String cropId;
    private String cropName;

    private String createdByUid;
    private String createdByRole; // FARMER, MEDIATOR_BUYER, CUSTOMER

    private String farmerUid;
    private String buyerUid;
    private String buyerRole; // MEDIATOR_BUYER, CUSTOMER
    private String counterpartyUid;
    private String counterpartyRole;

    private String issueCategory; // Controlled by DisputeCategory
    private String subject;
    private String description;
    private String status; // Controlled by DisputeStatus

    private String resolutionSummary;
    private String resolutionOutcome; // Controlled by DisputeResolutionOutcome
    private String resolvedBy;
    private String resolvedAt;
    private String closedAt;

    private String createdAt;
    private String updatedAt;
    private String lastActivityAt;

    public Dispute() {}

    public Dispute(String disputeId, String orderId, String listingId, String offerId, String cropId,
                   String cropName, String createdByUid, String createdByRole, String farmerUid,
                   String buyerUid, String buyerRole, String issueCategory, String subject,
                   String description, String status, String resolutionSummary,
                   String resolutionOutcome, String resolvedBy, String resolvedAt, String closedAt,
                   String createdAt, String updatedAt, String lastActivityAt) {
        this.disputeId = disputeId;
        this.orderId = orderId;
        this.listingId = listingId;
        this.offerId = offerId;
        this.cropId = cropId;
        this.cropName = cropName;
        this.createdByUid = createdByUid;
        this.createdByRole = createdByRole;
        this.farmerUid = farmerUid;
        this.buyerUid = buyerUid;
        this.buyerRole = buyerRole;
        this.issueCategory = issueCategory;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.resolutionSummary = resolutionSummary;
        this.resolutionOutcome = resolutionOutcome;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActivityAt = lastActivityAt;
        if (createdByUid != null) {
            if (createdByUid.equals(farmerUid)) {
                this.counterpartyUid = buyerUid;
                this.counterpartyRole = buyerRole;
            } else if (createdByUid.equals(buyerUid)) {
                this.counterpartyUid = farmerUid;
                this.counterpartyRole = "FARMER";
            }
        }
    }

    public String getCounterpartyUid() { return counterpartyUid; }
    public void setCounterpartyUid(String counterpartyUid) { this.counterpartyUid = counterpartyUid; }

    public String getCounterpartyRole() { return counterpartyRole; }
    public void setCounterpartyRole(String counterpartyRole) { this.counterpartyRole = counterpartyRole; }

    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getCreatedByUid() { return createdByUid; }
    public void setCreatedByUid(String createdByUid) { this.createdByUid = createdByUid; }

    public String getCreatedByRole() { return createdByRole; }
    public void setCreatedByRole(String createdByRole) { this.createdByRole = createdByRole; }

    public String getFarmerUid() { return farmerUid; }
    public void setFarmerUid(String farmerUid) { this.farmerUid = farmerUid; }

    public String getBuyerUid() { return buyerUid; }
    public void setBuyerUid(String buyerUid) { this.buyerUid = buyerUid; }

    public String getBuyerRole() { return buyerRole; }
    public void setBuyerRole(String buyerRole) { this.buyerRole = buyerRole; }

    public String getIssueCategory() { return issueCategory; }
    public void setIssueCategory(String issueCategory) { this.issueCategory = issueCategory; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }

    public String getResolutionOutcome() { return resolutionOutcome; }
    public void setResolutionOutcome(String resolutionOutcome) { this.resolutionOutcome = resolutionOutcome; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getClosedAt() { return closedAt; }
    public void setClosedAt(String closedAt) { this.closedAt = closedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(String lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}
