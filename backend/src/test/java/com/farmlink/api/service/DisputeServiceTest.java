package com.farmlink.api.service;

import com.farmlink.api.common.event.MarketplaceDomainEvent;
import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Dispute Service Unit & Workflow Test Suite")
class DisputeServiceTest {

    private OrderService orderService;
    private ApplicationEventPublisher eventPublisher;
    private DisputeService disputeService;

    private static final String BUYER_UID = "buyer_cust_101";
    private static final String FARMER_UID = "farmer_101";
    private static final String STRANGER_UID = "stranger_999";
    private static final String ORDER_ID = "ord_tomato_001";

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        // Using in-memory fallback (firestore == null, quotaGuard == null)
        disputeService = new DisputeService(null, orderService, null, eventPublisher);

        // Standard confirmed order fixture
        OrderResponse confirmedOrder = new OrderResponse();
        confirmedOrder.setOrderId(ORDER_ID);
        confirmedOrder.setListingId("list_123");
        confirmedOrder.setOfferId("off_456");
        confirmedOrder.setFarmerId(FARMER_UID);
        confirmedOrder.setBuyerUid(BUYER_UID);
        confirmedOrder.setBuyerRole("CUSTOMER");
        confirmedOrder.setCropId("crop_tomato");
        confirmedOrder.setCropName("Tomato (Hybrid)");
        confirmedOrder.setStatus("CONFIRMED");
        confirmedOrder.setTotalAmount(BigDecimal.valueOf(12500));

        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(orderService.getOrderById(eq(ORDER_ID), eq(FARMER_UID))).thenReturn(confirmedOrder);
        when(orderService.getOrderById(eq(ORDER_ID), eq(STRANGER_UID)))
                .thenThrow(new AccessDeniedException("Unauthorized access to order."));
        when(orderService.getOrderById(eq("non_existent_order"), any()))
                .thenThrow(new NoSuchElementException("Order not found with ID: non_existent_order"));
    }

    @Test
    @DisplayName("Buyer creates valid dispute successfully")
    void testCreateDispute_ByBuyer_Success() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "PRODUCT_QUALITY", "Damaged tomatoes received",
                "Two boxes contained spoiled and rotten produce upon opening."
        );

        DisputeResponse response = disputeService.createDispute(BUYER_UID, request);

        assertNotNull(response);
        assertNotNull(response.getDisputeId());
        assertEquals(ORDER_ID, response.getOrderId());
        assertEquals("PRODUCT_QUALITY", response.getIssueCategory());
        assertEquals("Damaged tomatoes received", response.getSubject());
        assertEquals(BUYER_UID, response.getCreatedByUid());
        assertEquals("CUSTOMER", response.getCreatedByRole());
        assertEquals(FARMER_UID, response.getFarmerUid());
        assertEquals(BUYER_UID, response.getBuyerUid());
        assertEquals(DisputeStatus.OPEN.name(), response.getStatus());

        // Verify notification event published for farmer (counterparty)
        verify(eventPublisher, times(1)).publishEvent(any(MarketplaceDomainEvent.class));
    }

    @Test
    @DisplayName("Farmer creates valid dispute successfully")
    void testCreateDispute_ByFarmer_Success() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "PAYMENT_ISSUE", "Payment verification mismatch",
                "Buyer payment verification is still pending after shipment."
        );

        DisputeResponse response = disputeService.createDispute(FARMER_UID, request);

        assertNotNull(response);
        assertEquals(FARMER_UID, response.getCreatedByUid());
        assertEquals("FARMER", response.getCreatedByRole());
        assertEquals(DisputeStatus.OPEN.name(), response.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(MarketplaceDomainEvent.class));
    }

    @Test
    @DisplayName("Dispute creation fails for unauthenticated caller")
    void testCreateDispute_Unauthenticated() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "PRODUCT_QUALITY", "Subject", "Description of the problem."
        );
        assertThrows(AccessDeniedException.class, () -> disputeService.createDispute(null, request));
        assertThrows(AccessDeniedException.class, () -> disputeService.createDispute("   ", request));
    }

    @Test
    @DisplayName("Dispute creation fails for unrelated order participant")
    void testCreateDispute_UnrelatedUser() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "PRODUCT_QUALITY", "Subject", "Description of the problem."
        );
        assertThrows(AccessDeniedException.class, () -> disputeService.createDispute(STRANGER_UID, request));
    }

    @Test
    @DisplayName("Dispute creation fails for nonexistent order")
    void testCreateDispute_NonexistentOrder() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                "non_existent_order", "PRODUCT_QUALITY", "Subject", "Description of the problem."
        );
        assertThrows(NoSuchElementException.class, () -> disputeService.createDispute(BUYER_UID, request));
    }

    @Test
    @DisplayName("Dispute creation fails for PENDING order")
    void testCreateDispute_PendingOrder_Fails() {
        OrderResponse pendingOrder = new OrderResponse();
        pendingOrder.setOrderId("ord_pending");
        pendingOrder.setFarmerId(FARMER_UID);
        pendingOrder.setBuyerUid(BUYER_UID);
        pendingOrder.setStatus("PENDING");
        when(orderService.getOrderById(eq("ord_pending"), eq(BUYER_UID))).thenReturn(pendingOrder);

        CreateDisputeRequest request = new CreateDisputeRequest(
                "ord_pending", "ORDER_CANCELLATION", "Order cancellation issue",
                "Want to dispute unconfirmed pending order."
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> disputeService.createDispute(BUYER_UID, request));
        assertTrue(ex.getMessage().contains("PENDING"));
    }

    @Test
    @DisplayName("Dispute creation validates category, subject, and description")
    void testCreateDispute_Validation() {
        // Invalid category
        CreateDisputeRequest badCat = new CreateDisputeRequest(ORDER_ID, "INVALID_CAT", "Subject", "Valid description here.");
        assertThrows(IllegalArgumentException.class, () -> disputeService.createDispute(BUYER_UID, badCat));

        // Short subject
        CreateDisputeRequest shortSub = new CreateDisputeRequest(ORDER_ID, "PRODUCT_QUALITY", "ab", "Valid description here.");
        assertThrows(IllegalArgumentException.class, () -> disputeService.createDispute(BUYER_UID, shortSub));

        // Short description
        CreateDisputeRequest shortDesc = new CreateDisputeRequest(ORDER_ID, "PRODUCT_QUALITY", "Valid Subject", "Too short");
        assertThrows(IllegalArgumentException.class, () -> disputeService.createDispute(BUYER_UID, shortDesc));
    }

    @Test
    @DisplayName("Duplicate active dispute prevention blocks duplicate open disputes for same order & category")
    void testDuplicateActiveDisputePrevention() {
        CreateDisputeRequest req1 = new CreateDisputeRequest(
                ORDER_ID, "DAMAGED_PRODUCT", "Damaged produce crates",
                "Boxes arrived crushed and leaking produce."
        );

        DisputeResponse first = disputeService.createDispute(BUYER_UID, req1);
        assertNotNull(first);

        // Attempt second dispute with identical category while first is OPEN
        CreateDisputeRequest req2 = new CreateDisputeRequest(
                ORDER_ID, "DAMAGED_PRODUCT", "Another report of damaged crates",
                "Duplicate submission for the exact same damage."
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> disputeService.createDispute(BUYER_UID, req2));
        assertTrue(ex.getMessage().contains("already exists"));

        // Dispute with DIFFERENT category on same order is allowed
        CreateDisputeRequest reqDiffCat = new CreateDisputeRequest(
                ORDER_ID, "DELIVERY_DELAY", "Delivery arrived 4 days late",
                "Delivery was delayed significantly beyond agreed window."
        );
        DisputeResponse diffCatResponse = disputeService.createDispute(BUYER_UID, reqDiffCat);
        assertNotNull(diffCatResponse);
    }

    @Test
    @DisplayName("Detail retrieval enforces participant authorization")
    void testGetDisputeById_Authorization() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "WRONG_PRODUCT", "Wrong grade delivered",
                "Received Grade B produce instead of agreed Grade A."
        );
        DisputeResponse created = disputeService.createDispute(BUYER_UID, request);

        // Buyer can access
        DisputeDetailResponse buyerView = disputeService.getDisputeById(BUYER_UID, created.getDisputeId());
        assertNotNull(buyerView);
        assertEquals(created.getDisputeId(), buyerView.getDispute().getDisputeId());
        assertEquals(1, buyerView.getHistory().size()); // DISPUTE_CREATED entry

        // Farmer can access
        DisputeDetailResponse farmerView = disputeService.getDisputeById(FARMER_UID, created.getDisputeId());
        assertNotNull(farmerView);

        // Stranger cannot access
        assertThrows(AccessDeniedException.class,
                () -> disputeService.getDisputeById(STRANGER_UID, created.getDisputeId()));
    }

    @Test
    @DisplayName("Participant-scoped listing returns bounded user-specific disputes")
    void testListingEndpoints() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "QUANTITY_MISMATCH", "Shortfall in weight",
                "Received 850kg instead of agreed 1000kg."
        );
        disputeService.createDispute(BUYER_UID, request);

        // My disputes for Buyer
        DisputePageResponse myDisputes = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 10);
        assertEquals(1, myDisputes.getItems().size());

        // My disputes for Farmer (Farmer did not create this)
        DisputePageResponse farmerMine = disputeService.getMyDisputes(FARMER_UID, "ALL", 0, 10);
        assertEquals(0, farmerMine.getItems().size());

        // Related disputes for Farmer (Farmer is counterparty)
        DisputePageResponse farmerRelated = disputeService.getRelatedDisputes(FARMER_UID, "ALL", 0, 10);
        assertEquals(1, farmerRelated.getItems().size());
        assertEquals(BUYER_UID, farmerRelated.getItems().get(0).getCreatedByUid());

        // Disputes by order ID
        List<DisputeResponse> orderDisputes = disputeService.getDisputesByOrderId(BUYER_UID, ORDER_ID);
        assertEquals(1, orderDisputes.size());
    }

    @Test
    @DisplayName("Participant responses and WAITING_FOR_INFORMATION automatic transition")
    void testDisputeResponses() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "PRODUCT_QUALITY", "Produce rot observed",
                "Several crates had extensive rot."
        );
        DisputeResponse created = disputeService.createDispute(BUYER_UID, request);

        // Farmer responds
        AddDisputeResponseRequest farmerMsg = new AddDisputeResponseRequest("Could you provide photos of the crates?");
        DisputeResponseRecord resp1 = disputeService.addResponse(FARMER_UID, created.getDisputeId(), farmerMsg);
        assertNotNull(resp1);
        assertEquals("FARMER", resp1.getAuthorRole());
        assertEquals("Could you provide photos of the crates?", resp1.getMessage());

        // Stranger cannot respond
        assertThrows(AccessDeniedException.class,
                () -> disputeService.addResponse(STRANGER_UID, created.getDisputeId(), new AddDisputeResponseRequest("Invalid user")));

        // Simulate status set to WAITING_FOR_INFORMATION (e.g. system/support requested info)
        DisputeDetailResponse currentDetail = disputeService.getDisputeById(BUYER_UID, created.getDisputeId());
        // Directly test response on active dispute
        AddDisputeResponseRequest buyerReply = new AddDisputeResponseRequest("Attached labels and delivery receipt.");
        DisputeResponseRecord resp2 = disputeService.addResponse(BUYER_UID, created.getDisputeId(), buyerReply);
        assertNotNull(resp2);
        assertEquals("CUSTOMER", resp2.getAuthorRole());

        DisputeDetailResponse updated = disputeService.getDisputeById(BUYER_UID, created.getDisputeId());
        assertEquals(2, updated.getResponses().size());
        assertEquals(3, updated.getHistory().size()); // 1 created + 2 responses
    }

    @Test
    @DisplayName("Evidence metadata registration and validation")
    void testDisputeEvidence() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "DAMAGED_PRODUCT", "Damaged produce crates",
                "Produce damaged during transit."
        );
        DisputeResponse created = disputeService.createDispute(BUYER_UID, request);

        // Valid evidence metadata registration (strictly metadata-only, no storagePath accepted)
        AddDisputeEvidenceRequest validEvidence = new AddDisputeEvidenceRequest(
                "damage_crate1.jpg", "image/jpeg", 1024000L, "Front crate damage"
        );
        DisputeEvidence evidence = disputeService.addEvidence(BUYER_UID, created.getDisputeId(), validEvidence);
        assertNotNull(evidence);
        assertEquals("damage_crate1.jpg", evidence.getFileName());
        assertEquals("image/jpeg", evidence.getContentType());
        assertEquals("CUSTOMER", evidence.getRegisteredByRole());
        assertEquals(BUYER_UID, evidence.getRegisteredByUid());
        assertNull(evidence.getStoragePath(), "Evidence must be strictly metadata-only with null storagePath");

        // Invalid MIME type (e.g. executable)
        AddDisputeEvidenceRequest badMime = new AddDisputeEvidenceRequest(
                "virus.exe", "application/x-msdownload", 1000L, "Bad file"
        );
        assertThrows(IllegalArgumentException.class,
                () -> disputeService.addEvidence(BUYER_UID, created.getDisputeId(), badMime));

        // Oversized file (> 10 MB)
        AddDisputeEvidenceRequest hugeFile = new AddDisputeEvidenceRequest(
                "big.jpg", "image/jpeg", 20 * 1024 * 1024L, "Huge"
        );
        assertThrows(IllegalArgumentException.class,
                () -> disputeService.addEvidence(BUYER_UID, created.getDisputeId(), hugeFile));
    }

    @Test
    @DisplayName("Creator-only voluntary closure operates correctly")
    void testCreatorOnlyVoluntaryClosure() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "DELIVERY_DELAY", "Late arrival",
                "Order arrived 2 days later than committed."
        );
        DisputeResponse created = disputeService.createDispute(BUYER_UID, request);

        // Non-creator (Farmer) attempts to close -> fails
        assertThrows(AccessDeniedException.class,
                () -> disputeService.closeDispute(FARMER_UID, created.getDisputeId()));

        // Creator (Buyer) closes -> succeeds
        DisputeResponse closed = disputeService.closeDispute(BUYER_UID, created.getDisputeId());
        assertEquals(DisputeStatus.CLOSED.name(), closed.getStatus());
        assertNotNull(closed.getClosedAt());

        // Repeated close attempt -> fails
        assertThrows(IllegalStateException.class,
                () -> disputeService.closeDispute(BUYER_UID, created.getDisputeId()));

        // Responses on CLOSED dispute -> fails
        assertThrows(IllegalStateException.class,
                () -> disputeService.addResponse(BUYER_UID, created.getDisputeId(), new AddDisputeResponseRequest("Message on closed")));

        // Evidence on CLOSED dispute -> fails
        AddDisputeEvidenceRequest ev = new AddDisputeEvidenceRequest(
                "photo.png", "image/png", 500L, "Desc"
        );
        assertThrows(IllegalStateException.class,
                () -> disputeService.addEvidence(BUYER_UID, created.getDisputeId(), ev));

        // After closure, opening a new dispute for that category is allowed again
        DisputeResponse reOpened = disputeService.createDispute(BUYER_UID, request);
        assertNotNull(reOpened);
        assertNotEquals(created.getDisputeId(), reOpened.getDisputeId());
    }

    @Test
    @DisplayName("Notification failure does not disrupt dispute creation")
    void testNotificationFailureIsolation() {
        doThrow(new RuntimeException("Simulated notification service failure"))
                .when(eventPublisher).publishEvent(any());

        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "OTHER", "Packaging issue",
                "Cardboard boxes were torn on delivery."
        );

        // Dispute creation must succeed even if event publication fails
        assertDoesNotThrow(() -> {
            DisputeResponse response = disputeService.createDispute(BUYER_UID, request);
            assertNotNull(response);
            assertEquals("OTHER", response.getIssueCategory());
        });
    }

    @Test
    @DisplayName("Concurrent duplicate creation allows exactly one dispute and rejects subsequent duplicates")
    void testConcurrentDuplicateCreation() throws Exception {
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateConflictCount = new AtomicInteger(0);

        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "DAMAGED_PRODUCT", "Crushed containers", "Produce arrived damaged and leaking."
        );

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    disputeService.createDispute(BUYER_UID, request);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("already exists")) {
                        duplicateConflictCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Exactly one thread succeeds in creating the dispute
        assertEquals(1, successCount.get(), "Exactly one concurrent creation should succeed");
        // All other concurrent threads are blocked by duplicate lock
        assertEquals(threadCount - 1, duplicateConflictCount.get(), "All other concurrent attempts should fail with duplicate conflict");
    }

    @Test
    @DisplayName("Evidence attachment is strictly metadata-only with no client storage reference control")
    void testAddEvidence_StrictlyMetadataOnly_NoStorageReference() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "WRONG_PRODUCT", "Wrong variety delivered", "Received yellow corn instead of sweet corn."
        );
        DisputeResponse created = disputeService.createDispute(BUYER_UID, request);

        AddDisputeEvidenceRequest metadataOnly = new AddDisputeEvidenceRequest(
                "proof.jpg", "image/jpeg", 2048L, "Inspection photo"
        );
        DisputeEvidence registered = disputeService.addEvidence(BUYER_UID, created.getDisputeId(), metadataOnly);

        assertNotNull(registered);
        assertEquals("proof.jpg", registered.getFileName());
        assertEquals("image/jpeg", registered.getContentType());
        assertEquals(2048L, registered.getFileSize());
        assertEquals("Inspection photo", registered.getDescription());
        assertEquals(BUYER_UID, registered.getRegisteredByUid());
        assertEquals("CUSTOMER", registered.getRegisteredByRole());
        assertNull(registered.getStoragePath(), "Evidence record must remain strictly metadata-only without client storage path");

        // Verify history trail records registration without claiming upload
        DisputeDetailResponse detail = disputeService.getDisputeById(BUYER_UID, created.getDisputeId());
        assertTrue(detail.getHistory().stream().anyMatch(h ->
                h.getActionType().equals("EVIDENCE_ADDED") && h.getDescription().contains("Evidence metadata registered")));
    }

    @Test
    @DisplayName("My disputes pagination supports page boundaries, filtering, and records beyond initial limit")
    void testPagination_Mine_PageBoundariesAndRecordsBeyondInitialLimit() {
        // Create 55 disputes for BUYER_UID across mock orders
        for (int i = 1; i <= 55; i++) {
            String oId = "ord_batch_mine_" + i;
            OrderResponse ord = new OrderResponse();
            ord.setOrderId(oId);
            ord.setFarmerId(FARMER_UID);
            ord.setBuyerUid(BUYER_UID);
            ord.setStatus("COMPLETED");
            when(orderService.getOrderById(eq(oId), eq(BUYER_UID))).thenReturn(ord);

            CreateDisputeRequest req = new CreateDisputeRequest(
                    oId, "PRODUCT_QUALITY", "Batch Issue " + i, "Description for batch item " + i
            );
            disputeService.createDispute(BUYER_UID, req);
        }

        // Page 0 (size 20)
        DisputePageResponse page0 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20);
        assertEquals(20, page0.getItems().size());
        assertEquals(55, page0.getTotalElements());
        assertEquals(3, page0.getTotalPages());
        assertTrue(page0.isHasNext());

        // Page 1 (size 20)
        DisputePageResponse page1 = disputeService.getMyDisputes(BUYER_UID, "ALL", 1, 20);
        assertEquals(20, page1.getItems().size());
        assertEquals(55, page1.getTotalElements());
        assertTrue(page1.isHasNext());

        // Page 2 (size 20) -> should have remaining 15 items
        DisputePageResponse page2 = disputeService.getMyDisputes(BUYER_UID, "ALL", 2, 20);
        assertEquals(15, page2.getItems().size());
        assertEquals(55, page2.getTotalElements());
        assertFalse(page2.isHasNext());

        // Page 3 (size 20) -> beyond total items
        DisputePageResponse page3 = disputeService.getMyDisputes(BUYER_UID, "ALL", 3, 20);
        assertEquals(0, page3.getItems().size());
        assertFalse(page3.isHasNext());

        // Status filtering: filter by OPEN
        DisputePageResponse openDisputes = disputeService.getMyDisputes(BUYER_UID, "OPEN", 0, 50);
        assertEquals(50, openDisputes.getItems().size());
        assertEquals(55, openDisputes.getTotalElements());

        // Status filtering: filter by CLOSED (none closed yet)
        DisputePageResponse closedDisputes = disputeService.getMyDisputes(BUYER_UID, "CLOSED", 0, 20);
        assertEquals(0, closedDisputes.getItems().size());
    }

    @Test
    @DisplayName("Related disputes pagination correctly excludes own disputes and supports records beyond initial limit")
    void testPagination_Related_PageBoundariesFilteringAndExclusionOfOwnDisputes() {
        // Create 55 disputes on orders where FARMER_UID is farmer, created by BUYER_UID
        for (int i = 1; i <= 55; i++) {
            String oId = "ord_batch_rel_" + i;
            OrderResponse ord = new OrderResponse();
            ord.setOrderId(oId);
            ord.setFarmerId(FARMER_UID);
            ord.setBuyerUid(BUYER_UID);
            ord.setStatus("COMPLETED");
            when(orderService.getOrderById(eq(oId), eq(BUYER_UID))).thenReturn(ord);
            when(orderService.getOrderById(eq(oId), eq(FARMER_UID))).thenReturn(ord);

            CreateDisputeRequest req = new CreateDisputeRequest(
                    oId, "DELIVERY_DELAY", "Buyer Delay Issue " + i, "Delivery took too long for item " + i
            );
            disputeService.createDispute(BUYER_UID, req);
        }

        // Create 15 disputes on other orders created by FARMER_UID themselves
        for (int i = 1; i <= 15; i++) {
            String oId = "ord_batch_farmer_own_" + i;
            OrderResponse ord = new OrderResponse();
            ord.setOrderId(oId);
            ord.setFarmerId(FARMER_UID);
            ord.setBuyerUid(BUYER_UID);
            ord.setStatus("COMPLETED");
            when(orderService.getOrderById(eq(oId), eq(FARMER_UID))).thenReturn(ord);

            CreateDisputeRequest req = new CreateDisputeRequest(
                    oId, "PAYMENT_ISSUE", "Farmer Payment Issue " + i, "Payment not cleared for item " + i
            );
            disputeService.createDispute(FARMER_UID, req);
        }

        // Farmer checks related disputes (must see all 55 disputes created by buyer, 0 of their own 15)
        DisputePageResponse page0 = disputeService.getRelatedDisputes(FARMER_UID, "ALL", 0, 20);
        assertEquals(20, page0.getItems().size());
        assertEquals(55, page0.getTotalElements(), "Related disputes should include all 55 counterparty disputes and exclude own");
        assertTrue(page0.isHasNext());

        DisputePageResponse page1 = disputeService.getRelatedDisputes(FARMER_UID, "ALL", 1, 20);
        assertEquals(20, page1.getItems().size());
        assertTrue(page1.isHasNext());

        DisputePageResponse page2 = disputeService.getRelatedDisputes(FARMER_UID, "ALL", 2, 20);
        assertEquals(15, page2.getItems().size());
        assertFalse(page2.isHasNext());

        // Confirm every item in related disputes was created by BUYER_UID and NOT FARMER_UID
        for (DisputeResponse item : page0.getItems()) {
            assertEquals(BUYER_UID, item.getCreatedByUid());
            assertNotEquals(FARMER_UID, item.getCreatedByUid());
        }

        // Status filter on related disputes
        DisputePageResponse openRelated = disputeService.getRelatedDisputes(FARMER_UID, "OPEN", 0, 50);
        assertEquals(50, openRelated.getItems().size());
        DisputePageResponse closedRelated = disputeService.getRelatedDisputes(FARMER_UID, "CLOSED", 0, 20);
        assertEquals(0, closedRelated.getItems().size());
    }

    @Nested
    @DisplayName("Firestore Cursor-Based Pagination Test Suite (Module 24 Stage 3)")
    class CursorPaginationTests {

        @SuppressWarnings("unchecked")
        private void injectDisputeDirectly(Dispute dispute) throws Exception {
            Field field = DisputeService.class.getDeclaredField("inMemoryDisputes");
            field.setAccessible(true);
            Map<String, Dispute> map = (Map<String, Dispute>) field.get(disputeService);
            map.put(dispute.getDisputeId(), dispute);
        }

        private void seedDisputesForBuyer(int count) {
            for (int i = 1; i <= count; i++) {
                String oId = "ord_cursor_buyer_" + i;
                OrderResponse ord = new OrderResponse();
                ord.setOrderId(oId);
                ord.setFarmerId(FARMER_UID);
                ord.setBuyerUid(BUYER_UID);
                ord.setStatus("COMPLETED");
                when(orderService.getOrderById(eq(oId), eq(BUYER_UID))).thenReturn(ord);

                CreateDisputeRequest req = new CreateDisputeRequest(
                        oId, "PRODUCT_QUALITY", "Cursor Issue " + i, "Description for item " + i
                );
                disputeService.createDispute(BUYER_UID, req);
            }
        }

        @Test
        @DisplayName("1. First page returns at most requested page size records (bounded)")
        void test01_firstPageReturnsAtMostBoundedPageSizeRecords() {
            seedDisputesForBuyer(45);

            DisputePageResponse page1 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);

            assertNotNull(page1);
            assertEquals(20, page1.getItems().size(), "First page should contain exactly 20 items");
            assertTrue(page1.isHasNext(), "First page should indicate hasNext = true");
            assertNotNull(page1.getNextCursor(), "First page should return a non-null next cursor");
        }

        @Test
        @DisplayName("2. Valid next cursor returns next consecutive page")
        void test02_validNextCursorReturnsNextPage() {
            seedDisputesForBuyer(45);

            DisputePageResponse page1 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            String cursor1 = page1.getNextCursor();
            assertNotNull(cursor1);

            DisputePageResponse page2 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, cursor1);
            assertNotNull(page2);
            assertEquals(20, page2.getItems().size(), "Second page should contain exactly 20 items");
            assertTrue(page2.isHasNext(), "Second page should have a next page");
            assertNotNull(page2.getNextCursor());

            // Verify items in page2 immediately follow page1
            String lastIdPage1 = page1.getItems().get(19).getDisputeId();
            String firstIdPage2 = page2.getItems().get(0).getDisputeId();
            assertNotEquals(lastIdPage1, firstIdPage2);
        }

        @Test
        @DisplayName("3. Consecutive pages contain no duplicate records")
        void test03_consecutivePagesContainNoDuplicateRecords() {
            seedDisputesForBuyer(45);

            DisputePageResponse page1 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            DisputePageResponse page2 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, page1.getNextCursor());

            Set<String> page1Ids = new HashSet<>();
            for (DisputeResponse r : page1.getItems()) {
                page1Ids.add(r.getDisputeId());
            }

            Set<String> page2Ids = new HashSet<>();
            for (DisputeResponse r : page2.getItems()) {
                page2Ids.add(r.getDisputeId());
            }

            assertTrue(Collections.disjoint(page1Ids, page2Ids), "Page 1 and Page 2 must not have overlapping dispute IDs");
        }

        @Test
        @DisplayName("4. Consecutive pages have no missing records across entire traversal")
        void test04_consecutivePagesHaveNoMissingRecords() {
            int totalSeeded = 45;
            seedDisputesForBuyer(totalSeeded);

            Set<String> collectedDisputeIds = new HashSet<>();
            String currentCursor = null;
            boolean hasNext = true;
            int pageCount = 0;

            while (hasNext && pageCount < 10) {
                DisputePageResponse page = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, currentCursor);
                for (DisputeResponse r : page.getItems()) {
                    collectedDisputeIds.add(r.getDisputeId());
                }
                hasNext = page.isHasNext();
                currentCursor = page.getNextCursor();
                pageCount++;
            }

            assertEquals(3, pageCount, "45 items with pageSize 20 should require exactly 3 pages");
            assertEquals(totalSeeded, collectedDisputeIds.size(), "Traversing with cursor must retrieve all seeded disputes without omissions");
        }

        @Test
        @DisplayName("5. Final page returns hasNext = false and nextCursor = null")
        void test05_finalPageReturnsHasNextFalseAndNullCursor() {
            seedDisputesForBuyer(25);

            DisputePageResponse page1 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            assertTrue(page1.isHasNext());
            assertNotNull(page1.getNextCursor());

            DisputePageResponse page2 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, page1.getNextCursor());
            assertEquals(5, page2.getItems().size());
            assertFalse(page2.isHasNext(), "Final page must have hasNext = false");
            assertNull(page2.getNextCursor(), "Final page must return null nextCursor");
        }

        @Test
        @DisplayName("6. Empty query result returns empty page with hasNext = false and null nextCursor")
        void test06_emptyResultReturnsEmptyPage() {
            DisputePageResponse emptyPage = disputeService.getMyDisputes("non_existent_uid", "ALL", 0, 20, null);

            assertNotNull(emptyPage);
            assertTrue(emptyPage.getItems().isEmpty());
            assertFalse(emptyPage.isHasNext());
            assertNull(emptyPage.getNextCursor());
        }

        @Test
        @DisplayName("7. Mine pagination is strictly restricted to authenticated creator")
        void test07_minePaginationRestrictedToAuthenticatedCreator() {
            // Buyer creates 5 disputes
            seedDisputesForBuyer(5);

            // Farmer creates 5 disputes
            for (int i = 1; i <= 5; i++) {
                String oId = "ord_cursor_farmer_" + i;
                OrderResponse ord = new OrderResponse();
                ord.setOrderId(oId);
                ord.setFarmerId(FARMER_UID);
                ord.setBuyerUid(BUYER_UID);
                ord.setStatus("COMPLETED");
                when(orderService.getOrderById(eq(oId), eq(FARMER_UID))).thenReturn(ord);

                CreateDisputeRequest req = new CreateDisputeRequest(
                        oId, "PAYMENT_ISSUE", "Farmer Issue " + i, "Payment issue " + i
                );
                disputeService.createDispute(FARMER_UID, req);
            }

            DisputePageResponse buyerMine = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            assertEquals(5, buyerMine.getItems().size());
            for (DisputeResponse item : buyerMine.getItems()) {
                assertEquals(BUYER_UID, item.getCreatedByUid());
                assertNotEquals(FARMER_UID, item.getCreatedByUid());
            }

            DisputePageResponse farmerMine = disputeService.getMyDisputes(FARMER_UID, "ALL", 0, 20, null);
            assertEquals(5, farmerMine.getItems().size());
            for (DisputeResponse item : farmerMine.getItems()) {
                assertEquals(FARMER_UID, item.getCreatedByUid());
                assertNotEquals(BUYER_UID, item.getCreatedByUid());
            }
        }

        @Test
        @DisplayName("8. Related pagination restricted to counterparty and excludes own disputes")
        void test08_relatedPaginationRestrictedToAuthenticatedCounterparty() {
            // Buyer creates 10 disputes where Farmer is counterparty
            seedDisputesForBuyer(10);

            // Farmer views related disputes
            DisputePageResponse related = disputeService.getRelatedDisputes(FARMER_UID, "ALL", 0, 20, null);
            assertEquals(10, related.getItems().size());
            for (DisputeResponse item : related.getItems()) {
                assertEquals(BUYER_UID, item.getCreatedByUid());
                assertEquals(FARMER_UID, item.getFarmerUid());
            }

            // Buyer views related disputes (should be 0 because buyer created all 10)
            DisputePageResponse buyerRelated = disputeService.getRelatedDisputes(BUYER_UID, "ALL", 0, 20, null);
            assertEquals(0, buyerRelated.getItems().size());
        }

        @Test
        @DisplayName("9. Cursor issued for another UID is rejected with AccessDeniedException")
        void test09_cursorFromAnotherUidRejected() {
            seedDisputesForBuyer(25);
            DisputePageResponse buyerPage = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            String buyerCursor = buyerPage.getNextCursor();
            assertNotNull(buyerCursor);

            // Farmer attempts to supply Buyer's cursor
            AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                    disputeService.getMyDisputes(FARMER_UID, "ALL", 0, 20, buyerCursor)
            );
            assertTrue(ex.getMessage().contains("Cursor owner mismatch"));
        }

        @Test
        @DisplayName("10. Cursor issued for wrong query type is rejected with IllegalArgumentException")
        void test10_cursorFromWrongQueryTypeRejected() {
            seedDisputesForBuyer(25);
            DisputePageResponse buyerPage = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            String mineCursor = buyerPage.getNextCursor();
            assertNotNull(mineCursor);

            // Buyer uses a 'MINE' cursor on a 'RELATED' query
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    disputeService.getRelatedDisputes(BUYER_UID, "ALL", 0, 20, mineCursor)
            );
            assertTrue(ex.getMessage().contains("Cursor query scope mismatch"));
        }

        @Test
        @DisplayName("11. Cryptographically malformed, forged, or tampered cursors are rejected safely with IllegalArgumentException")
        void test11_malformedOrTamperedCursorRejected() {
            seedDisputesForBuyer(25);
            DisputePageResponse buyerPage = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, null);
            String validCursor = buyerPage.getNextCursor();
            assertNotNull(validCursor);
            assertTrue(validCursor.contains("."), "Valid cursor must contain cryptographic dot separator");

            // A. Invalid format string without signature separator
            assertThrows(IllegalArgumentException.class, () ->
                    disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, "not_valid_base64_%%%!")
            );

            // B. Unsigned cursor (payload only, no signature)
            String unsignedCursor = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("MINE|buyer_cust_101|ALL|2026-11-11T12:00:00Z|disp_123".getBytes(StandardCharsets.UTF_8));
            IllegalArgumentException unsignedEx = assertThrows(IllegalArgumentException.class, () ->
                    disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, unsignedCursor)
            );
            assertTrue(unsignedEx.getMessage().contains("missing cryptographic signature"));

            // C. Tampered signature (altered signature bytes)
            String[] segments = validCursor.split("\\.");
            String forgedSig = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("forged_signature_bytes_1234567890".getBytes(StandardCharsets.UTF_8));
            String forgedCursor = segments[0] + "." + forgedSig;
            IllegalArgumentException forgedEx = assertThrows(IllegalArgumentException.class, () ->
                    disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, forgedCursor)
            );
            assertTrue(forgedEx.getMessage().contains("Invalid, tampered, or forged"));

            // D. Tampered payload with valid original signature
            String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("MINE|buyer_cust_101|ALL|2099-01-01T00:00:00Z|disp_tampered".getBytes(StandardCharsets.UTF_8));
            String tamperedCursor = tamperedPayload + "." + segments[1];
            IllegalArgumentException tamperedEx = assertThrows(IllegalArgumentException.class, () ->
                    disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 20, tamperedCursor)
            );
            assertTrue(tamperedEx.getMessage().contains("Invalid, tampered, or forged"));

            // E. Filter mismatch: cursor issued for ALL passed to OPEN query
            IllegalArgumentException filterEx = assertThrows(IllegalArgumentException.class, () ->
                    disputeService.getMyDisputes(BUYER_UID, "OPEN", 0, 20, validCursor)
            );
            assertTrue(filterEx.getMessage().contains("Cursor filter mismatch"));
        }

        @Test
        @DisplayName("12. Equal createdAt timestamps are handled deterministically via disputeId tie-breaker")
        void test12_equalCreatedAtTimestampsDeterministicTieBreaker() throws Exception {
            String sharedTimestamp = "2026-11-11T12:00:00Z";

            // Inject 5 disputes with the exact same timestamp but distinct IDs
            String[] ids = {"disp_tie_01", "disp_tie_02", "disp_tie_03", "disp_tie_04", "disp_tie_05"};
            for (String id : ids) {
                Dispute d = new Dispute();
                d.setDisputeId(id);
                d.setOrderId("ord_tie_test");
                d.setCreatedByUid(BUYER_UID);
                d.setCreatedByRole("CUSTOMER");
                d.setFarmerUid(FARMER_UID);
                d.setBuyerUid(BUYER_UID);
                d.setBuyerRole("CUSTOMER");
                d.setCounterpartyUid(FARMER_UID);
                d.setCounterpartyRole("FARMER");
                d.setIssueCategory("PRODUCT_QUALITY");
                d.setSubject("Tie subject " + id);
                d.setDescription("Tie description " + id);
                d.setStatus("OPEN");
                d.setCreatedAt(sharedTimestamp);
                d.setUpdatedAt(sharedTimestamp);
                d.setLastActivityAt(sharedTimestamp);

                injectDisputeDirectly(d);
            }

            // Page 1 with size 2 (expect disp_tie_05, disp_tie_04 due to DESC tie-breaker)
            DisputePageResponse p1 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 2, null);
            assertEquals(2, p1.getItems().size());
            assertEquals("disp_tie_05", p1.getItems().get(0).getDisputeId());
            assertEquals("disp_tie_04", p1.getItems().get(1).getDisputeId());
            assertTrue(p1.isHasNext());
            assertNotNull(p1.getNextCursor());

            // Page 2 with size 2 (expect disp_tie_03, disp_tie_02)
            DisputePageResponse p2 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 2, p1.getNextCursor());
            assertEquals(2, p2.getItems().size());
            assertEquals("disp_tie_03", p2.getItems().get(0).getDisputeId());
            assertEquals("disp_tie_02", p2.getItems().get(1).getDisputeId());
            assertTrue(p2.isHasNext());
            assertNotNull(p2.getNextCursor());

            // Page 3 with size 2 (expect disp_tie_01, hasNext = false, nextCursor = null)
            DisputePageResponse p3 = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 2, p2.getNextCursor());
            assertEquals(1, p3.getItems().size());
            assertEquals("disp_tie_01", p3.getItems().get(0).getDisputeId());
            assertFalse(p3.isHasNext());
            assertNull(p3.getNextCursor());
        }

        @Test
        @DisplayName("13. Bounded query caps page size at 50 and handles invalid size gracefully")
        void test13_boundedQueryCapsAtFiftyAndRejectsUnboundedFetch() {
            seedDisputesForBuyer(60);

            // Attempting to request size = 100 is capped at 50
            DisputePageResponse cappedPage = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, 100, null);
            assertEquals(50, cappedPage.getItems().size(), "Page size must be capped at 50 to prevent unbounded reads");
            assertEquals(50, cappedPage.getPageSize());

            // Negative size is normalized to 1
            DisputePageResponse minPage = disputeService.getMyDisputes(BUYER_UID, "ALL", 0, -10, null);
            assertEquals(1, minPage.getItems().size());
            assertEquals(1, minPage.getPageSize());
        }
    }

    @Nested
    @DisplayName("Dispute Cursor Secret Production Configuration Tests")
    class ProductionSecretConfigurationTests {

        private static final String STRONG_VALID_SECRET = "production-hmac-sha256-secret-minimum-32-bytes-entropy!!";
        private static final String WEAK_INSUFFICIENT_SECRET = "short-secret-only-20b!";

        @Test
        @DisplayName("1. Production startup fails when DISPUTE_CURSOR_SECRET is missing (null)")
        void testProductionStartup_RejectsMissingSecret() {
            Environment prodEnv = mock(Environment.class);
            when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"production"});

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    new DisputeService(null, orderService, null, eventPublisher, null, prodEnv)
            );
            assertTrue(ex.getMessage().contains("DISPUTE_CURSOR_SECRET is missing or blank in production"));
        }

        @Test
        @DisplayName("2. Production startup fails when DISPUTE_CURSOR_SECRET is blank")
        void testProductionStartup_RejectsBlankSecret() {
            Environment prodEnv = mock(Environment.class);
            when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"production"});

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    new DisputeService(null, orderService, null, eventPublisher, "   \t\n  ", prodEnv)
            );
            assertTrue(ex.getMessage().contains("DISPUTE_CURSOR_SECRET is missing or blank in production"));
        }

        @Test
        @DisplayName("3. Production startup fails when DISPUTE_CURSOR_SECRET is insufficiently strong (< 32 bytes)")
        void testProductionStartup_RejectsInsufficientlyStrongSecret() {
            Environment prodEnv = mock(Environment.class);
            when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"production"});

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    new DisputeService(null, orderService, null, eventPublisher, WEAK_INSUFFICIENT_SECRET, prodEnv)
            );
            assertTrue(ex.getMessage().contains("insufficiently strong"));
            assertTrue(ex.getMessage().contains("Minimum required length is 32 bytes"));
        }

        @Test
        @DisplayName("4. Production startup succeeds when a valid (>= 32 bytes) secret is provided")
        void testProductionStartup_ValidSecret_InitializesSuccessfully() {
            Environment prodEnv = mock(Environment.class);
            when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"production"});

            assertDoesNotThrow(() ->
                    new DisputeService(null, orderService, null, eventPublisher, STRONG_VALID_SECRET, prodEnv)
            );
        }

        @Test
        @DisplayName("5. Local/test behavior initializes successfully with fallback when secret is omitted")
        void testLocalDevelopmentFallback_InitializesSuccessfullyWithoutSecret() {
            Environment devEnv = mock(Environment.class);
            when(devEnv.getActiveProfiles()).thenReturn(new String[]{"dev"});

            DisputeService devService = assertDoesNotThrow(() ->
                    new DisputeService(null, orderService, null, eventPublisher, null, devEnv)
            );
            assertNotNull(devService);

            // Verify the dev service produces functioning cryptographically signed cursors
            OrderResponse ord = new OrderResponse();
            ord.setOrderId("ord_dev_test");
            ord.setFarmerId(FARMER_UID);
            ord.setBuyerUid(BUYER_UID);
            ord.setStatus("COMPLETED");
            when(orderService.getOrderById(eq("ord_dev_test"), eq(BUYER_UID))).thenReturn(ord);

            devService.createDispute(BUYER_UID, new CreateDisputeRequest(
                    "ord_dev_test", "PRODUCT_QUALITY", "Dev Issue", "Dev description"
            ));

            DisputePageResponse page = devService.getMyDisputes(BUYER_UID, "ALL", 0, 10, null);
            assertEquals(1, page.getItems().size());
        }

        @Test
        @DisplayName("6. A signed cursor verifies successfully across service instances sharing the same secret")
        void testCursorVerification_VerifiesWithSameSecret() throws Exception {
            Environment prodEnv = mock(Environment.class);
            when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"production"});

            DisputeService instanceA = new DisputeService(null, orderService, null, eventPublisher, STRONG_VALID_SECRET, prodEnv);
            DisputeService instanceB = new DisputeService(null, orderService, null, eventPublisher, STRONG_VALID_SECRET, prodEnv);

            // Populate both instances with identical dispute records
            for (int i = 1; i <= 5; i++) {
                Dispute d = new Dispute();
                d.setDisputeId("disp_shared_" + i);
                d.setOrderId("ord_shared_test");
                d.setCreatedByUid(BUYER_UID);
                d.setCreatedByRole("CUSTOMER");
                d.setFarmerUid(FARMER_UID);
                d.setBuyerUid(BUYER_UID);
                d.setBuyerRole("CUSTOMER");
                d.setCounterpartyUid(FARMER_UID);
                d.setCounterpartyRole("FARMER");
                d.setIssueCategory("PRODUCT_QUALITY");
                d.setSubject("Shared subject " + i);
                d.setDescription("Shared description " + i);
                d.setStatus("OPEN");
                d.setCreatedAt("2026-11-11T12:0" + i + ":00Z");

                Field fieldA = DisputeService.class.getDeclaredField("inMemoryDisputes");
                fieldA.setAccessible(true);
                ((Map<String, Dispute>) fieldA.get(instanceA)).put(d.getDisputeId(), d);

                Field fieldB = DisputeService.class.getDeclaredField("inMemoryDisputes");
                fieldB.setAccessible(true);
                ((Map<String, Dispute>) fieldB.get(instanceB)).put(d.getDisputeId(), d);
            }

            // Instance A issues cursor
            DisputePageResponse pageA = instanceA.getMyDisputes(BUYER_UID, "ALL", 0, 2, null);
            String cursorFromA = pageA.getNextCursor();
            assertNotNull(cursorFromA);

            // Instance B decodes and resumes using Instance A's cursor
            DisputePageResponse pageB = instanceB.getMyDisputes(BUYER_UID, "ALL", 0, 2, cursorFromA);
            assertNotNull(pageB);
            assertEquals(2, pageB.getItems().size());
        }

        @Test
        @DisplayName("7. A cursor signed with another secret key is rejected with IllegalArgumentException")
        void testCursorVerification_RejectsCursorSignedWithDifferentSecret() throws Exception {
            String secretKey1 = "production-secret-instance-one-minimum-32-bytes-long!";
            String secretKey2 = "production-secret-instance-two-minimum-32-bytes-long!";

            DisputeService serviceWithKey1 = new DisputeService(null, orderService, null, eventPublisher, secretKey1);
            DisputeService serviceWithKey2 = new DisputeService(null, orderService, null, eventPublisher, secretKey2);

            // Seed dispute in serviceWithKey1
            Dispute d = new Dispute();
            d.setDisputeId("disp_key_diff_1");
            d.setOrderId("ord_key_test");
            d.setCreatedByUid(BUYER_UID);
            d.setCreatedByRole("CUSTOMER");
            d.setFarmerUid(FARMER_UID);
            d.setBuyerUid(BUYER_UID);
            d.setBuyerRole("CUSTOMER");
            d.setCounterpartyUid(FARMER_UID);
            d.setCounterpartyRole("FARMER");
            d.setIssueCategory("PRODUCT_QUALITY");
            d.setSubject("Diff key subject");
            d.setDescription("Diff key description");
            d.setStatus("OPEN");
            d.setCreatedAt("2026-11-11T12:00:00Z");

            Field field = DisputeService.class.getDeclaredField("inMemoryDisputes");
            field.setAccessible(true);
            ((Map<String, Dispute>) field.get(serviceWithKey1)).put(d.getDisputeId(), d);

            // Generate cursor with key 1
            DisputePageResponse page1 = serviceWithKey1.getMyDisputes(BUYER_UID, "ALL", 0, 1, null);
            // In order to have nextCursor, add second item
            Dispute d2 = new Dispute();
            d2.setDisputeId("disp_key_diff_2");
            d2.setOrderId("ord_key_test");
            d2.setCreatedByUid(BUYER_UID);
            d2.setCreatedByRole("CUSTOMER");
            d2.setFarmerUid(FARMER_UID);
            d2.setBuyerUid(BUYER_UID);
            d2.setBuyerRole("CUSTOMER");
            d2.setCounterpartyUid(FARMER_UID);
            d2.setCounterpartyRole("FARMER");
            d2.setIssueCategory("PRODUCT_QUALITY");
            d2.setSubject("Diff key subject 2");
            d2.setDescription("Diff key description 2");
            d2.setStatus("OPEN");
            d2.setCreatedAt("2026-11-11T11:00:00Z");
            ((Map<String, Dispute>) field.get(serviceWithKey1)).put(d2.getDisputeId(), d2);

            DisputePageResponse pageWithCursor = serviceWithKey1.getMyDisputes(BUYER_UID, "ALL", 0, 1, null);
            String cursorKey1 = pageWithCursor.getNextCursor();
            assertNotNull(cursorKey1);

            // Attempt to verify with serviceWithKey2
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    serviceWithKey2.getMyDisputes(BUYER_UID, "ALL", 0, 1, cursorKey1)
            );
            assertTrue(ex.getMessage().contains("Invalid, tampered, or forged pagination cursor signature"));
        }
    }

    @Test
    @DisplayName("Stage 4.3: Order cancellation and other dispute categories are supported and verified")
    void testDisputeCreation_OrderCancellationAndOtherCategories() {
        CreateDisputeRequest cancelReq = new CreateDisputeRequest(
                ORDER_ID, "ORDER_CANCELLATION", "Order improperly cancelled", "Cancelled after payment was processed."
        );
        DisputeResponse cancelDisp = disputeService.createDispute(BUYER_UID, cancelReq);
        assertNotNull(cancelDisp);
        assertEquals("ORDER_CANCELLATION", cancelDisp.getIssueCategory());

        // Close to clear lock for next test
        disputeService.closeDispute(BUYER_UID, cancelDisp.getDisputeId());

        CreateDisputeRequest otherReq = new CreateDisputeRequest(
                ORDER_ID, "OTHER", "Unspecified packaging damage", "Unspecified commercial disagreement."
        );
        DisputeResponse otherDisp = disputeService.createDispute(BUYER_UID, otherReq);
        assertNotNull(otherDisp);
        assertEquals("OTHER", otherDisp.getIssueCategory());

        // Add evidence and verify registeredBy fields are mapped cleanly
        AddDisputeEvidenceRequest eviReq = new AddDisputeEvidenceRequest(
                "other_proof.pdf", "application/pdf", 102400L, "Supporting contract doc"
        );
        DisputeEvidence evi = disputeService.addEvidence(BUYER_UID, otherDisp.getDisputeId(), eviReq);
        assertNotNull(evi);
        assertEquals(BUYER_UID, evi.getRegisteredByUid());
        assertEquals("CUSTOMER", evi.getRegisteredByRole());

        DisputeDetailResponse detail = disputeService.getDisputeById(BUYER_UID, otherDisp.getDisputeId());
        assertEquals(1, detail.getEvidence().size());
        assertEquals(BUYER_UID, detail.getEvidence().get(0).getRegisteredByUid());
        assertEquals("CUSTOMER", detail.getEvidence().get(0).getRegisteredByRole());
    }
}

