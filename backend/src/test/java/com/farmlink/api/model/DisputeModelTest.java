package com.farmlink.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dispute Domain Models & Enums Test Suite")
class DisputeModelTest {

    @Test
    @DisplayName("DisputeCategory enum validation and parsing")
    void testDisputeCategory() {
        assertEquals(9, DisputeCategory.values().length);

        assertTrue(DisputeCategory.isValid("PRODUCT_QUALITY"));
        assertTrue(DisputeCategory.isValid("damaged_product"));
        assertTrue(DisputeCategory.isValid("QUANTITY_MISMATCH"));
        assertTrue(DisputeCategory.isValid("WRONG_PRODUCT"));
        assertTrue(DisputeCategory.isValid("DELIVERY_DELAY"));
        assertTrue(DisputeCategory.isValid("DELIVERY_NOT_RECEIVED"));
        assertTrue(DisputeCategory.isValid("PAYMENT_ISSUE"));
        assertTrue(DisputeCategory.isValid("ORDER_CANCELLATION"));
        assertTrue(DisputeCategory.isValid("OTHER"));

        assertFalse(DisputeCategory.isValid(null));
        assertFalse(DisputeCategory.isValid(""));
        assertFalse(DisputeCategory.isValid("INVALID_CATEGORY"));

        assertEquals(DisputeCategory.PRODUCT_QUALITY, DisputeCategory.fromString("PRODUCT_QUALITY"));
        assertEquals(DisputeCategory.DELIVERY_DELAY, DisputeCategory.fromString("delivery_delay"));
        assertNull(DisputeCategory.fromString("UNKNOWN"));
        assertNull(DisputeCategory.fromString(null));
    }

    @Test
    @DisplayName("DisputeStatus enum lifecycle and valid state transitions")
    void testDisputeStatusTransitions() {
        assertEquals(6, DisputeStatus.values().length);

        // Terminal & Active status checks
        assertTrue(DisputeStatus.CLOSED.isTerminal());
        assertFalse(DisputeStatus.CLOSED.isActive());

        assertFalse(DisputeStatus.OPEN.isTerminal());
        assertTrue(DisputeStatus.OPEN.isActive());

        assertFalse(DisputeStatus.UNDER_REVIEW.isTerminal());
        assertTrue(DisputeStatus.UNDER_REVIEW.isActive());

        assertFalse(DisputeStatus.WAITING_FOR_INFORMATION.isTerminal());
        assertTrue(DisputeStatus.WAITING_FOR_INFORMATION.isActive());

        assertFalse(DisputeStatus.RESOLVED.isTerminal());
        assertFalse(DisputeStatus.RESOLVED.isActive());

        assertFalse(DisputeStatus.REJECTED.isTerminal());
        assertFalse(DisputeStatus.REJECTED.isActive());

        // Transitions from OPEN
        assertTrue(DisputeStatus.OPEN.canTransitionTo(DisputeStatus.UNDER_REVIEW));
        assertTrue(DisputeStatus.OPEN.canTransitionTo(DisputeStatus.WAITING_FOR_INFORMATION));
        assertTrue(DisputeStatus.OPEN.canTransitionTo(DisputeStatus.RESOLVED));
        assertTrue(DisputeStatus.OPEN.canTransitionTo(DisputeStatus.REJECTED));
        assertTrue(DisputeStatus.OPEN.canTransitionTo(DisputeStatus.CLOSED));
        assertFalse(DisputeStatus.OPEN.canTransitionTo(DisputeStatus.OPEN));
        assertFalse(DisputeStatus.OPEN.canTransitionTo(null));

        // Transitions from UNDER_REVIEW
        assertTrue(DisputeStatus.UNDER_REVIEW.canTransitionTo(DisputeStatus.WAITING_FOR_INFORMATION));
        assertTrue(DisputeStatus.UNDER_REVIEW.canTransitionTo(DisputeStatus.RESOLVED));
        assertTrue(DisputeStatus.UNDER_REVIEW.canTransitionTo(DisputeStatus.REJECTED));
        assertTrue(DisputeStatus.UNDER_REVIEW.canTransitionTo(DisputeStatus.CLOSED));
        assertFalse(DisputeStatus.UNDER_REVIEW.canTransitionTo(DisputeStatus.OPEN));

        // Transitions from WAITING_FOR_INFORMATION
        assertTrue(DisputeStatus.WAITING_FOR_INFORMATION.canTransitionTo(DisputeStatus.UNDER_REVIEW));
        assertTrue(DisputeStatus.WAITING_FOR_INFORMATION.canTransitionTo(DisputeStatus.RESOLVED));
        assertTrue(DisputeStatus.WAITING_FOR_INFORMATION.canTransitionTo(DisputeStatus.REJECTED));
        assertTrue(DisputeStatus.WAITING_FOR_INFORMATION.canTransitionTo(DisputeStatus.CLOSED));
        assertFalse(DisputeStatus.WAITING_FOR_INFORMATION.canTransitionTo(DisputeStatus.OPEN));

        // Transitions from RESOLVED / REJECTED
        assertTrue(DisputeStatus.RESOLVED.canTransitionTo(DisputeStatus.CLOSED));
        assertFalse(DisputeStatus.RESOLVED.canTransitionTo(DisputeStatus.UNDER_REVIEW));
        assertTrue(DisputeStatus.REJECTED.canTransitionTo(DisputeStatus.CLOSED));
        assertFalse(DisputeStatus.REJECTED.canTransitionTo(DisputeStatus.OPEN));

        // Transitions from CLOSED (terminal)
        assertFalse(DisputeStatus.CLOSED.canTransitionTo(DisputeStatus.OPEN));
        assertFalse(DisputeStatus.CLOSED.canTransitionTo(DisputeStatus.UNDER_REVIEW));
        assertFalse(DisputeStatus.CLOSED.canTransitionTo(DisputeStatus.RESOLVED));

        // String helper methods
        assertTrue(DisputeStatus.isValid("OPEN"));
        assertTrue(DisputeStatus.isValid("under_review"));
        assertFalse(DisputeStatus.isValid("INVALID"));
        assertFalse(DisputeStatus.isValid(null));
        assertEquals(DisputeStatus.WAITING_FOR_INFORMATION, DisputeStatus.fromString("WAITING_FOR_INFORMATION"));
        assertNull(DisputeStatus.fromString(""));
    }

    @Test
    @DisplayName("DisputeHistoryAction enum validation")
    void testDisputeHistoryAction() {
        assertEquals(9, DisputeHistoryAction.values().length);
        assertTrue(DisputeHistoryAction.isValid("DISPUTE_CREATED"));
        assertTrue(DisputeHistoryAction.isValid("RESPONSE_ADDED"));
        assertTrue(DisputeHistoryAction.isValid("EVIDENCE_ADDED"));
        assertTrue(DisputeHistoryAction.isValid("INFORMATION_REQUESTED"));
        assertTrue(DisputeHistoryAction.isValid("INFORMATION_SUBMITTED"));
        assertTrue(DisputeHistoryAction.isValid("STATUS_CHANGED"));
        assertTrue(DisputeHistoryAction.isValid("DISPUTE_RESOLVED"));
        assertTrue(DisputeHistoryAction.isValid("DISPUTE_REJECTED"));
        assertTrue(DisputeHistoryAction.isValid("DISPUTE_CLOSED"));

        assertFalse(DisputeHistoryAction.isValid("UNKNOWN_ACTION"));
        assertFalse(DisputeHistoryAction.isValid(null));
    }

    @Test
    @DisplayName("DisputeResolutionOutcome enum validation")
    void testDisputeResolutionOutcome() {
        assertEquals(5, DisputeResolutionOutcome.values().length);
        assertTrue(DisputeResolutionOutcome.isValid("ISSUE_CONFIRMED"));
        assertTrue(DisputeResolutionOutcome.isValid("ISSUE_NOT_CONFIRMED"));
        assertTrue(DisputeResolutionOutcome.isValid("PARTIAL_RESOLUTION"));
        assertTrue(DisputeResolutionOutcome.isValid("NO_ACTION_REQUIRED"));
        assertTrue(DisputeResolutionOutcome.isValid("OTHER"));

        assertFalse(DisputeResolutionOutcome.isValid("INVALID_OUTCOME"));
        assertFalse(DisputeResolutionOutcome.isValid(null));
    }

    @Test
    @DisplayName("Dispute entity construction, getters and setters without speculative fields")
    void testDisputeEntity() {
        String now = Instant.now().toString();
        Dispute dispute = new Dispute(
                "disp_123", "order_456", "list_789", "off_101", "crop_tomato",
                "Tomato (Hybrid)", "buyer_01", "CUSTOMER", "farmer_01",
                "buyer_01", "CUSTOMER",
                DisputeCategory.PRODUCT_QUALITY.name(), "Damaged tomatoes",
                "Boxes arrived with rot", DisputeStatus.OPEN.name(),
                null, null, null, null, null, now, now, now
        );

        assertEquals("disp_123", dispute.getDisputeId());
        assertEquals("order_456", dispute.getOrderId());
        assertEquals("list_789", dispute.getListingId());
        assertEquals("off_101", dispute.getOfferId());
        assertEquals("crop_tomato", dispute.getCropId());
        assertEquals("Tomato (Hybrid)", dispute.getCropName());
        assertEquals("buyer_01", dispute.getCreatedByUid());
        assertEquals("CUSTOMER", dispute.getCreatedByRole());
        assertEquals("farmer_01", dispute.getFarmerUid());
        assertEquals("buyer_01", dispute.getBuyerUid());
        assertEquals("CUSTOMER", dispute.getBuyerRole());
        assertEquals("PRODUCT_QUALITY", dispute.getIssueCategory());
        assertEquals("Damaged tomatoes", dispute.getSubject());
        assertEquals("Boxes arrived with rot", dispute.getDescription());
        assertEquals("OPEN", dispute.getStatus());
        assertEquals(now, dispute.getCreatedAt());
        assertEquals(now, dispute.getUpdatedAt());
        assertEquals(now, dispute.getLastActivityAt());

        // Test setters and no-arg constructor
        Dispute emptyDispute = new Dispute();
        emptyDispute.setDisputeId("disp_999");
        emptyDispute.setStatus("CLOSED");
        emptyDispute.setResolutionSummary("Resolved peacefully");
        emptyDispute.setResolutionOutcome("NO_ACTION_REQUIRED");
        emptyDispute.setResolvedBy("support_ref");
        emptyDispute.setResolvedAt(now);
        emptyDispute.setClosedAt(now);

        assertEquals("disp_999", emptyDispute.getDisputeId());
        assertEquals("CLOSED", emptyDispute.getStatus());
        assertEquals("Resolved peacefully", emptyDispute.getResolutionSummary());
        assertEquals("NO_ACTION_REQUIRED", emptyDispute.getResolutionOutcome());
        assertEquals("support_ref", emptyDispute.getResolvedBy());
        assertEquals(now, emptyDispute.getResolvedAt());
        assertEquals(now, emptyDispute.getClosedAt());
    }

    @Test
    @DisplayName("DisputeResponseRecord entity construction and accessors")
    void testDisputeResponseRecordEntity() {
        String now = Instant.now().toString();
        DisputeResponseRecord response = new DisputeResponseRecord(
                "resp_01", "disp_123", "farmer_01", "FARMER",
                "We inspected before dispatch, please share box labels.", now
        );

        assertEquals("resp_01", response.getResponseId());
        assertEquals("disp_123", response.getDisputeId());
        assertEquals("farmer_01", response.getAuthorUid());
        assertEquals("FARMER", response.getAuthorRole());
        assertEquals("We inspected before dispatch, please share box labels.", response.getMessage());
        assertEquals(now, response.getCreatedAt());

        DisputeResponseRecord emptyResponse = new DisputeResponseRecord();
        emptyResponse.setMessage("Updated reply");
        assertEquals("Updated reply", emptyResponse.getMessage());
    }

    @Test
    @DisplayName("DisputeEvidence entity construction and accessors")
    void testDisputeEvidenceEntity() {
        String now = Instant.now().toString();
        DisputeEvidence evidence = new DisputeEvidence(
                "evi_01", "disp_123", "buyer_01", "CUSTOMER",
                "damage_photo_1.jpg", "disputes/disp_123/damage_photo_1.jpg",
                "image/jpeg", 2048500L, "Close-up of damaged produce crates", now
        );

        assertEquals("evi_01", evidence.getEvidenceId());
        assertEquals("disp_123", evidence.getDisputeId());
        assertEquals("buyer_01", evidence.getUploadedByUid());
        assertEquals("CUSTOMER", evidence.getUploadedByRole());
        assertEquals("damage_photo_1.jpg", evidence.getFileName());
        assertEquals("disputes/disp_123/damage_photo_1.jpg", evidence.getStoragePath());
        assertEquals("image/jpeg", evidence.getContentType());
        assertEquals(2048500L, evidence.getFileSize());
        assertEquals("Close-up of damaged produce crates", evidence.getDescription());
        assertEquals(now, evidence.getCreatedAt());

        DisputeEvidence emptyEvidence = new DisputeEvidence();
        emptyEvidence.setFileName("doc.pdf");
        assertEquals("doc.pdf", emptyEvidence.getFileName());
    }

    @Test
    @DisplayName("DisputeHistoryEntry entity construction and accessors")
    void testDisputeHistoryEntryEntity() {
        String now = Instant.now().toString();
        DisputeHistoryEntry entry = new DisputeHistoryEntry(
                "hist_01", "disp_123", "buyer_01", "CUSTOMER",
                DisputeHistoryAction.DISPUTE_CREATED.name(), null, "OPEN",
                "Dispute opened for damaged produce", now
        );

        assertEquals("hist_01", entry.getHistoryId());
        assertEquals("disp_123", entry.getDisputeId());
        assertEquals("buyer_01", entry.getActorUid());
        assertEquals("CUSTOMER", entry.getActorRole());
        assertEquals("DISPUTE_CREATED", entry.getActionType());
        assertNull(entry.getPreviousStatus());
        assertEquals("OPEN", entry.getNewStatus());
        assertEquals("Dispute opened for damaged produce", entry.getDescription());
        assertEquals(now, entry.getCreatedAt());

        DisputeHistoryEntry emptyEntry = new DisputeHistoryEntry();
        emptyEntry.setNewStatus("UNDER_REVIEW");
        assertEquals("UNDER_REVIEW", emptyEntry.getNewStatus());
    }
}
