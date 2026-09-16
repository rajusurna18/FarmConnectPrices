package com.farmlink.api.common.event;

import com.farmlink.api.model.NotificationType;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain event published by marketplace services (Offers, Orders, Payments, Delivery, Reviews)
 * to trigger decoupled notifications without blocking core business transactions.
 */
public class MarketplaceDomainEvent extends ApplicationEvent {

    private final String recipientUid;
    private final String recipientRole;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String relatedEntityType;
    private final String relatedEntityId;
    private final String relatedRoute;
    private final Map<String, Object> metadata;
    private final String sourceEventId;

    public MarketplaceDomainEvent(Object source,
                                  String recipientUid,
                                  String recipientRole,
                                  NotificationType type,
                                  String title,
                                  String message,
                                  String relatedEntityType,
                                  String relatedEntityId,
                                  String relatedRoute,
                                  Map<String, Object> metadata,
                                  String sourceEventId) {
        super(source);
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

    public String getRecipientRole() {
        return recipientRole;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getRelatedRoute() {
        return relatedRoute;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }
}
