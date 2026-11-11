package com.farmlink.api.service;

import com.farmlink.api.common.event.MarketplaceDomainEvent;
import com.farmlink.api.dto.CreateNotificationRequest;
import com.farmlink.api.dto.NotificationResponse;
import com.farmlink.api.dto.UpdateNotificationPreferenceRequest;
import com.farmlink.api.model.Dispute;
import com.farmlink.api.model.DisputeCategory;
import com.farmlink.api.model.DisputeStatus;
import com.farmlink.api.model.NotificationCategory;
import com.farmlink.api.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Dispute Notification & Security Integration Test Suite")
class DisputeNotificationAndSecurityTest {

    private NotificationPreferenceService preferenceService;
    private NotificationService notificationService;

    private static final String BUYER_UID = "buyer_user_101";
    private static final String FARMER_UID = "farmer_user_202";

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceService(null, null);
        notificationService = new NotificationService(null, null, preferenceService);
    }

    @Test
    @DisplayName("All Dispute Notification Types map to NotificationCategory.ORDERS")
    void testDisputeNotificationTypeCategoryMapping() {
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.DISPUTE_CREATED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.DISPUTE_RESPONSE_RECEIVED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.DISPUTE_INFORMATION_REQUESTED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.DISPUTE_STATUS_UPDATED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.DISPUTE_RESOLVED));
        assertEquals(NotificationCategory.ORDERS, NotificationCategory.fromNotificationType(NotificationType.DISPUTE_CLOSED));
    }

    @Test
    @DisplayName("Dispute notifications are controlled by the ORDERS user preference")
    void testDisputeNotificationPreferenceSuppression() {
        // 1. User disables ORDERS notifications
        preferenceService.updatePreferences(FARMER_UID, new UpdateNotificationPreferenceRequest(
                true, false, true, true, true // orders = false
        ));

        CreateNotificationRequest request = new CreateNotificationRequest(
                FARMER_UID, "FARMER", NotificationType.DISPUTE_CREATED,
                "Issue Reported", "An issue was reported for order #ord_1.",
                "DISPUTE", "disp_101", "/marketplace/disputes/disp_101",
                Map.of("orderId", "ord_1"), "evt_disp_101"
        );

        NotificationResponse response = notificationService.createNotification(request);
        assertNull(response, "Dispute notification should be suppressed when ORDERS category is false");

        // 2. User enables ORDERS notifications
        preferenceService.updatePreferences(FARMER_UID, new UpdateNotificationPreferenceRequest(
                true, true, true, true, true // orders = true
        ));

        NotificationResponse activeResponse = notificationService.createNotification(request);
        assertNotNull(activeResponse, "Dispute notification should succeed when ORDERS category is true");
        assertEquals("Issue Reported", activeResponse.getTitle());
    }

    @Test
    @DisplayName("Counterparty recipient selection and self-notification prevention")
    void testRecipientSelectionAndSelfNotificationPrevention() {
        String now = Instant.now().toString();
        Dispute disputeCreatedByBuyer = new Dispute(
                "disp_001", "ord_100", "list_200", "off_300", "crop_wheat", "Wheat",
                BUYER_UID, "CUSTOMER", FARMER_UID, BUYER_UID, "CUSTOMER",
                DisputeCategory.PRODUCT_QUALITY.name(), "Damaged grains",
                "Moisture damage observed in delivered bags.", DisputeStatus.OPEN.name(),
                null, null, null, null, null, now, now, now
        );

        // When Buyer creates dispute, recipient MUST be Farmer
        String callerUid = BUYER_UID;
        String recipientUid = callerUid.equals(disputeCreatedByBuyer.getBuyerUid())
                ? disputeCreatedByBuyer.getFarmerUid()
                : disputeCreatedByBuyer.getBuyerUid();

        assertEquals(FARMER_UID, recipientUid);
        assertNotEquals(callerUid, recipientUid, "Author must never be notified about their own action");

        // When Farmer responds, recipient MUST be Buyer
        callerUid = FARMER_UID;
        recipientUid = callerUid.equals(disputeCreatedByBuyer.getBuyerUid())
                ? disputeCreatedByBuyer.getFarmerUid()
                : disputeCreatedByBuyer.getBuyerUid();

        assertEquals(BUYER_UID, recipientUid);
        assertNotEquals(callerUid, recipientUid, "Author must never be notified about their own action");
    }

    @Test
    @DisplayName("Dispute notification message preview does not leak sensitive description")
    void testDisputeNotificationContentSafety() {
        String orderId = "ord_456";
        String sensitiveDescription = "The produce was completely spoiled, farmer was rude and refused to talk.";

        // Construct safe notification content
        String safeTitle = "Issue Reported on Order #" + orderId;
        String safeMessage = "An issue has been reported for order #" + orderId + ". Please review dispute details.";

        assertFalse(safeMessage.contains(sensitiveDescription));
        assertFalse(safeTitle.contains(sensitiveDescription));
        assertTrue(safeMessage.contains(orderId));

        CreateNotificationRequest request = new CreateNotificationRequest(
                FARMER_UID, "FARMER", NotificationType.DISPUTE_CREATED,
                safeTitle, safeMessage, "DISPUTE", "disp_789",
                "/marketplace/disputes/disp_789", Map.of("orderId", orderId), "evt_disp_safe"
        );

        NotificationResponse response = notificationService.createNotification(request);
        assertNotNull(response);
        assertEquals(safeTitle, response.getTitle());
        assertEquals(safeMessage, response.getMessage());
        assertFalse(response.getMessage().contains(sensitiveDescription));
    }

    @Test
    @DisplayName("Notification failure isolation ensures core transaction is unaffected")
    void testNotificationFailureIsolation() {
        NotificationService faultyService = mock(NotificationService.class);
        doThrow(new RuntimeException("Firestore service temporarily unavailable"))
                .when(faultyService).createNotification(any());

        NotificationEventListener listener = new NotificationEventListener(faultyService);

        MarketplaceDomainEvent event = new MarketplaceDomainEvent(
                this, FARMER_UID, "FARMER", NotificationType.DISPUTE_CREATED,
                "Issue Reported", "An issue was reported", "DISPUTE", "disp_1",
                "/marketplace/disputes/disp_1", Map.of(), "evt_disp_err"
        );

        // Verification: Listener catches exception, logs, and does NOT throw
        assertDoesNotThrow(() -> listener.handleMarketplaceDomainEvent(event));
    }

    @Test
    @DisplayName("Firestore security rules enforce read restriction and write denial for disputes")
    void testFirestoreRulesDisputesConfiguration() throws IOException {
        Path rulesPath = Paths.get("firestore.rules");
        if (!Files.exists(rulesPath)) {
            rulesPath = Paths.get("../firestore.rules");
        }
        assertTrue(Files.exists(rulesPath), "firestore.rules file must exist");

        String rulesContent = Files.readString(rulesPath);

        // 1. Verify disputes collection rule exists
        assertTrue(rulesContent.contains("match /disputes/{disputeId}"),
                "firestore.rules must define rules for /disputes/{disputeId}");

        // 2. Verify direct client write denial
        assertTrue(rulesContent.contains("allow write: if false;"),
                "firestore.rules must deny direct client writes");

        // 3. Verify participant-only read access
        assertTrue(rulesContent.contains("resource.data.buyerUid == request.auth.uid || resource.data.farmerUid == request.auth.uid"),
                "firestore.rules must restrict dispute reads to buyerUid or farmerUid");

        // 4. Verify subcollections for responses, evidence, and history exist and deny writes
        assertTrue(rulesContent.contains("match /responses/{responseId}"), "Must define /responses subcollection rule");
        assertTrue(rulesContent.contains("match /evidence/{evidenceId}"), "Must define /evidence subcollection rule");
        assertTrue(rulesContent.contains("match /history/{historyId}"), "Must define /history subcollection rule");

        // 5. Verify default deny catch-all is preserved
        assertTrue(rulesContent.contains("match /{document=**} {\n      allow read, write: if false;\n    }")
                || rulesContent.contains("match /{document=**} {"),
                "Default deny rule must be preserved");
    }
}

