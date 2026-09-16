package com.farmlink.api.dto;

import com.farmlink.api.model.Notification;
import com.farmlink.api.model.NotificationType;

import java.util.Map;

public class NotificationResponse {
    private String notificationId;
    private String recipientUid;
    private String recipientRole;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private String relatedEntityType;
    private String relatedEntityId;
    private String relatedRoute;
    private Map<String, Object> metadata;
    private String sourceEventId;
    private String createdAt;
    private String readAt;

    public NotificationResponse() {}

    public NotificationResponse(Notification n) {
        if (n != null) {
            this.notificationId = n.getNotificationId();
            this.recipientUid = n.getRecipientUid();
            this.recipientRole = n.getRecipientRole();
            this.type = n.getType();
            this.title = n.getTitle();
            this.message = n.getMessage();
            this.read = n.isRead();
            this.relatedEntityType = n.getRelatedEntityType();
            this.relatedEntityId = n.getRelatedEntityId();
            this.relatedRoute = n.getRelatedRoute();
            this.metadata = n.getMetadata();
            this.sourceEventId = n.getSourceEventId();
            this.createdAt = n.getCreatedAt();
            this.readAt = n.getReadAt();
        }
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getReadAt() {
        return readAt;
    }

    public void setReadAt(String readAt) {
        this.readAt = readAt;
    }
}
