package com.farmlink.api.service;

import com.farmlink.api.common.event.MarketplaceDomainEvent;
import com.farmlink.api.dto.CreateNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener that converts published MarketplaceDomainEvents into persisted in-app notifications.
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    @Autowired
    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void handleMarketplaceDomainEvent(MarketplaceDomainEvent event) {
        if (event == null) {
            return;
        }

        try {
            CreateNotificationRequest request = new CreateNotificationRequest(
                    event.getRecipientUid(),
                    event.getRecipientRole(),
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getRelatedEntityType(),
                    event.getRelatedEntityId(),
                    event.getRelatedRoute(),
                    event.getMetadata(),
                    event.getSourceEventId()
            );

            notificationService.createNotification(request);
            logger.info("Successfully handled event {} for recipient {}", event.getType(), event.getRecipientUid());
        } catch (Exception e) {
            logger.error("Failed to process MarketplaceDomainEvent for type {}: {}",
                    event.getType(), e.getMessage(), e);
            // Core transaction will NOT be disrupted because event handling exception is swallowed safely here
        }
    }
}
