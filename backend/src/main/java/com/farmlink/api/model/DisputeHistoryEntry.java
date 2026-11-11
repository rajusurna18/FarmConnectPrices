package com.farmlink.api.model;

/**
 * Domain entity representing an immutable audit history record for a dispute, stored in subcollection 'disputes/{disputeId}/history'.
 */
public class DisputeHistoryEntry {
    private String historyId;
    private String disputeId;
    private String actorUid;
    private String actorRole; // FARMER, MEDIATOR_BUYER, CUSTOMER, SYSTEM
    private String actionType; // Controlled by DisputeHistoryAction
    private String previousStatus;
    private String newStatus;
    private String description;
    private String createdAt;

    public DisputeHistoryEntry() {}

    public DisputeHistoryEntry(String historyId, String disputeId, String actorUid,
                               String actorRole, String actionType, String previousStatus,
                               String newStatus, String description, String createdAt) {
        this.historyId = historyId;
        this.disputeId = disputeId;
        this.actorUid = actorUid;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }

    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }

    public String getActorUid() { return actorUid; }
    public void setActorUid(String actorUid) { this.actorUid = actorUid; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

