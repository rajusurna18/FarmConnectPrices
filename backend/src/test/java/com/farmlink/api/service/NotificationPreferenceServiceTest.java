package com.farmlink.api.service;

import com.farmlink.api.dto.CreateNotificationRequest;
import com.farmlink.api.dto.NotificationPreferenceResponse;
import com.farmlink.api.dto.NotificationResponse;
import com.farmlink.api.dto.UpdateNotificationPreferenceRequest;
import com.farmlink.api.model.NotificationCategory;
import com.farmlink.api.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationPreferenceServiceTest {

    private NotificationPreferenceService preferenceService;
    private NotificationService notificationService;

    private static final String USER_UID = "user_pref_test_100";

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceService(null, null);
        notificationService = new NotificationService(null, null, preferenceService);
    }

    @Test
    void testGetPreferences_DefaultValues() {
        NotificationPreferenceResponse pref = preferenceService.getPreferences(USER_UID);

        assertNotNull(pref);
        assertTrue(pref.isOffers());
        assertTrue(pref.isOrders());
        assertTrue(pref.isPayments());
        assertTrue(pref.isDelivery());
        assertTrue(pref.isReviews());
    }

    @Test
    void testUpdatePreferences_Success() {
        UpdateNotificationPreferenceRequest update = new UpdateNotificationPreferenceRequest(
                false, // offers
                true,  // orders
                true,  // payments
                false, // delivery
                true   // reviews
        );

        NotificationPreferenceResponse updated = preferenceService.updatePreferences(USER_UID, update);

        assertNotNull(updated);
        assertFalse(updated.isOffers());
        assertTrue(updated.isOrders());
        assertTrue(updated.isPayments());
        assertFalse(updated.isDelivery());
        assertTrue(updated.isReviews());

        // Verify GET returns updated values from cache
        NotificationPreferenceResponse cached = preferenceService.getPreferences(USER_UID);
        assertFalse(cached.isOffers());
        assertFalse(cached.isDelivery());
    }

    @Test
    void testNotificationCategoryMapping() {
        // OFFERS
        assertEquals(NotificationCategory.OFFERS, NotificationCategory.fromNotificationType(NotificationType.NEW_OFFER));
        assertEquals(NotificationCategory.OFFERS, NotificationCategory.fromNotificationType(NotificationType.COUNTER_OFFER_RECEIVED));
        assertEquals(NotificationCategory.OFFERS, NotificationCategory.fromNotificationType(NotificationType.OFFER_ACCEPTED));
        assertEquals(NotificationCategory.OFFERS, NotificationCategory.fromNotificationType(NotificationType.OFFER_REJECTED));
        assertEquals(NotificationCategory.OFFERS, NotificationCategory.fromNotificationType(NotificationType.OFFER_CANCELLED));

        // ORDERS
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.ORDER_CREATED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.ORDER_CONFIRMED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.ORDER_PROCESSING));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.ORDER_CANCELLED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.ORDER_COMPLETED));

        // PAYMENTS
        assertEquals(NotificationCategory.PAYMENTS, NotificationCategory.fromNotificationType(NotificationType.PAYMENT_CREATED));
        assertEquals(NotificationCategory.PAYMENTS, NotificationCategory.fromNotificationType(NotificationType.PAYMENT_SUCCESS));
        assertEquals(NotificationCategory.PAYMENTS, NotificationCategory.fromNotificationType(NotificationType.PAYMENT_FAILED));

        // DELIVERY
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_ASSIGNED));
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_READY_FOR_PICKUP));
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_PICKED_UP));
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_IN_TRANSIT));
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_OUT_FOR_DELIVERY));
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_DELIVERED));
        assertEquals(NotificationCategory.DELIVERY, NotificationCategory.fromNotificationType(NotificationType.DELIVERY_CANCELLED));

        // REVIEWS
        assertEquals(NotificationCategory.REVIEWS, NotificationCategory.fromNotificationType(NotificationType.NEW_REVIEW));
    }

    @Test
    void testNotificationServiceSuppressionByPreference() {
        // 1. Disable DELIVERY category for user
        preferenceService.updatePreferences(USER_UID, new UpdateNotificationPreferenceRequest(
                true, true, true, false, true
        ));

        // 2. Attempt to create DELIVERY notification -> should return null (suppressed)
        CreateNotificationRequest deliveryReq = new CreateNotificationRequest(
                USER_UID, "FARMER", NotificationType.DELIVERY_IN_TRANSIT,
                "Package in Transit", "Driver picked up your produce.",
                "DELIVERY", "del_1", "/marketplace/deliveries/del_1", Map.of(), "evt_del_1"
        );
        NotificationResponse deliveryRes = notificationService.createNotification(deliveryReq);
        assertNull(deliveryRes, "Delivery notification should be suppressed when delivery preference is false");

        // 3. Attempt to create OFFERS notification -> should succeed
        CreateNotificationRequest offerReq = new CreateNotificationRequest(
                USER_UID, "FARMER", NotificationType.NEW_OFFER,
                "New Offer Received", "Ramesh offered ₹2,500.",
                "OFFER", "off_1", "/marketplace/offers/off_1", Map.of(), "evt_off_1"
        );
        NotificationResponse offerRes = notificationService.createNotification(offerReq);
        assertNotNull(offerRes, "Offer notification should be created when offers preference is true");
        assertEquals("New Offer Received", offerRes.getTitle());
    }

    @Test
    void testValidation_NullUidOrRequest() {
        assertThrows(IllegalArgumentException.class, () -> preferenceService.getPreferences(null));
        assertThrows(IllegalArgumentException.class, () -> preferenceService.getPreferences(""));
        assertThrows(IllegalArgumentException.class, () -> preferenceService.updatePreferences(USER_UID, null));
    }
}
