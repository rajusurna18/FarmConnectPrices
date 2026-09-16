package com.farmlink.api.dto;

import com.farmlink.api.model.NotificationType;

import java.util.HashMap;
import java.util.Map;

public class CreateNotificationRequest {
    private String recipientUid;
    private String recipientRole;
    private NotificationType type;
    private String title;
    private String message;
    private String relatedEntityType;
    private String relatedEntityId;
    private String relatedRoute;
    private Map<String, Object> metadata = new HashMap<>();
    private String sourceEventId;

    public CreateNotificationRequest() {}

    public CreateNotificationRequest(String recipientUid, String recipientRole, NotificationType type,
                                     String title, String message, String relatedEntityType,
                                     String relatedEntityId, String relatedRoute,
                                     Map<String, Object> metadata, String sourceEventId) {
        this.recipientUid = recipientUid;
        this.recipientRole = recipientRole;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.relatedRoute = relatedRoute;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.sourceEventId = sourceEventId;
    }

    public String getRecipientUid() {
        return recipientUid;
    }

    public void setRecipientUid(String recipientUid) {
        this.recipientUid = recipientUid;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(String relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getRelatedRoute() {
        return relatedRoute;
    }

    public void setRelatedRoute(String relatedRoute) {
        this.relatedRoute = relatedRoute;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }
}
