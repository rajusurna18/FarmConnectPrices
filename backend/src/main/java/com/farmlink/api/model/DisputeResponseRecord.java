package com.farmlink.api.model;

/**
 * Domain entity representing a counterparty response to a dispute, stored in subcollection 'disputes/{disputeId}/responses'.
 */
public class DisputeResponseRecord {
    private String responseId;
    private String disputeId;
    private String authorUid;
    private String authorRole; // FARMER, MEDIATOR_BUYER, CUSTOMER
    private String message;
    private String createdAt;

    public DisputeResponseRecord() {}

    public DisputeResponseRecord(String responseId, String disputeId, String authorUid,
                                 String authorRole, String message, String createdAt) {
        this.responseId = responseId;
        this.disputeId = disputeId;
        this.authorUid = authorUid;
        this.authorRole = authorRole;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }

    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
