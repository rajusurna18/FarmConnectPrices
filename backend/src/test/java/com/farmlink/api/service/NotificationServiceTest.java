package com.farmlink.api.service;

import com.farmlink.api.dto.CreateNotificationRequest;
import com.farmlink.api.dto.NotificationListResponse;
import com.farmlink.api.dto.NotificationResponse;
import com.farmlink.api.dto.UnreadCountResponse;
import com.farmlink.api.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationServiceTest {

    private NotificationService notificationService;

    private static final String USER_A = "user_uid_100";
    private static final String USER_B = "user_uid_200";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(null, null);
    }

    @Test
    void testCreateNotification_Success() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                USER_A,
                "FARMER",
                NotificationType.NEW_OFFER,
                "New Offer Received",
                "Ramesh offered ₹2,500 for Tomatoes.",
                "OFFER",
                "offer_123",
                "/marketplace/offers/offer_123",
                Map.of("listingId", "list_1"),
                "event_offer_123"
        );

        NotificationResponse response = notificationService.createNotification(request);

        assertNotNull(response);
        assertNotNull(response.getNotificationId());
        assertEquals(USER_A, response.getRecipientUid());
        assertEquals("FARMER", response.getRecipientRole());
        assertEquals(NotificationType.NEW_OFFER, response.getType());
        assertEquals("New Offer Received", response.getTitle());
        assertEquals("Ramesh offered ₹2,500 for Tomatoes.", response.getMessage());
        assertFalse(response.isRead());
        assertEquals("OFFER", response.getRelatedEntityType());
        assertEquals("offer_123", response.getRelatedEntityId());
        assertEquals("/marketplace/offers/offer_123", response.getRelatedRoute());
        assertEquals("event_offer_123", response.getSourceEventId());
        assertNotNull(response.getCreatedAt());
        assertNull(response.getReadAt());
    }

    @Test
    void testCreateNotification_ValidationFailures() {
        assertThrows(IllegalArgumentException.class, () -> notificationService.createNotification(null));

        CreateNotificationRequest missingRecipient = new CreateNotificationRequest(
                "", "FARMER", NotificationType.NEW_OFFER, "Title", "Message", "", "", "", Map.of(), ""
        );
        assertThrows(IllegalArgumentException.class, () -> notificationService.createNotification(missingRecipient));

        CreateNotificationRequest missingType = new CreateNotificationRequest(
                USER_A, "FARMER", null, "Title", "Message", "", "", "", Map.of(), ""
        );
        assertThrows(IllegalArgumentException.class, () -> notificationService.createNotification(missingType));

        CreateNotificationRequest missingTitle = new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "  ", "Message", "", "", "", Map.of(), ""
        );
        assertThrows(IllegalArgumentException.class, () -> notificationService.createNotification(missingTitle));

        CreateNotificationRequest missingMessage = new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "Title", "   ", "", "", "", Map.of(), ""
        );
        assertThrows(IllegalArgumentException.class, () -> notificationService.createNotification(missingMessage));
    }

    @Test
    void testCreateNotification_IdempotencyDuplicateProtection() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.OFFER_ACCEPTED, "Offer Accepted",
                "Your offer was accepted", "OFFER", "offer_123", "/marketplace/offers/offer_123",
                Map.of(), "event_offer_accept_123"
        );

        NotificationResponse n1 = notificationService.createNotification(request);
        NotificationResponse n2 = notificationService.createNotification(request);

        assertNotNull(n1);
        assertNotNull(n2);
        assertEquals(n1.getNotificationId(), n2.getNotificationId());
    }

    @Test
    void testGetUnreadCountAndMarkAsRead() {
        notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "Offer 1", "Message 1", "", "", "", Map.of(), ""
        ));
        NotificationResponse n2 = notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.ORDER_CREATED, "Order 1", "Message 2", "", "", "", Map.of(), ""
        ));

        UnreadCountResponse initialCount = notificationService.getUnreadCount(USER_A);
        assertEquals(2, initialCount.getCount());

        NotificationResponse readResponse = notificationService.markAsRead(n2.getNotificationId(), USER_A);
        assertTrue(readResponse.isRead());
        assertNotNull(readResponse.getReadAt());

        UnreadCountResponse updatedCount = notificationService.getUnreadCount(USER_A);
        assertEquals(1, updatedCount.getCount());
    }

    @Test
    void testMarkAllAsRead() {
        notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "Offer 1", "Message 1", "", "", "", Map.of(), ""
        ));
        notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "Offer 2", "Message 2", "", "", "", Map.of(), ""
        ));

        assertEquals(2, notificationService.getUnreadCount(USER_A).getCount());

        UnreadCountResponse res = notificationService.markAllAsRead(USER_A);
        assertEquals(0, res.getCount());
        assertEquals(0, notificationService.getUnreadCount(USER_A).getCount());
    }

    @Test
    void testGetUserNotifications_OwnershipAndFiltering() {
        notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "User A Notif", "Message A", "", "", "", Map.of(), ""
        ));
        notificationService.createNotification(new CreateNotificationRequest(
                USER_B, "CUSTOMER", NotificationType.ORDER_CONFIRMED, "User B Notif", "Message B", "", "", "", Map.of(), ""
        ));

        NotificationListResponse userANotifs = notificationService.getUserNotifications(USER_A, false, 0, 10);
        assertEquals(1, userANotifs.getNotifications().size());
        assertEquals("User A Notif", userANotifs.getNotifications().get(0).getTitle());

        NotificationListResponse userBNotifs = notificationService.getUserNotifications(USER_B, false, 0, 10);
        assertEquals(1, userBNotifs.getNotifications().size());
        assertEquals("User B Notif", userBNotifs.getNotifications().get(0).getTitle());
    }

    @Test
    void testGetNotificationById_OwnershipDenied() {
        NotificationResponse n1 = notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "User A Notif", "Message A", "", "", "", Map.of(), ""
        ));

        assertThrows(AccessDeniedException.class, () ->
                notificationService.getNotificationById(n1.getNotificationId(), USER_B)
        );

        NotificationResponse retrieved = notificationService.getNotificationById(n1.getNotificationId(), USER_A);
        assertEquals(n1.getNotificationId(), retrieved.getNotificationId());
    }

    @Test
    void testDeleteNotification_SuccessAndOwnershipCheck() {
        NotificationResponse n1 = notificationService.createNotification(new CreateNotificationRequest(
                USER_A, "FARMER", NotificationType.NEW_OFFER, "User A Notif", "Message A", "", "", "", Map.of(), ""
        ));

        assertThrows(AccessDeniedException.class, () ->
                notificationService.deleteNotification(n1.getNotificationId(), USER_B)
        );

        notificationService.deleteNotification(n1.getNotificationId(), USER_A);

        assertThrows(NoSuchElementException.class, () ->
                notificationService.getNotificationById(n1.getNotificationId(), USER_A)
        );
    }
}
