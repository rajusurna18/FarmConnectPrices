package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.OfferStatus;
import com.farmlink.api.model.OrderStatus;
import com.farmlink.api.model.ProductListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OfferService offerService;

    @Mock
    private ProductListingService listingService;

    @Mock
    private ProfileService profileService;

    private OrderService orderService;

    private static final String FARMER_UID = "farmer_uid_100";
    private static final String BUYER_UID = "buyer_uid_200";
    private static final String OTHER_UID = "other_uid_300";
    private static final String OFFER_ID = "offer_accepted_1";
    private static final String LISTING_ID = "listing_test_1";

    private OfferResponse acceptedOffer;
    private ProductListingResponse activeListing;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(null, offerService, listingService, profileService, null);

        activeListing = new ProductListingResponse(
                LISTING_ID,
                FARMER_UID,
                "crop_paddy",
                "Paddy / Rice",
                100.0,
                100.0,
                "QUINTAL",
                2500.0,
                "QUINTAL",
                new LocationDto("Telangana", "Warangal", "Enumamula", "Village"),
                "Fresh Paddy",
                "GRADE_A",
                "2026-09-01",
                "2026-09-02",
                ProductListingStatus.ACTIVE.name(),
                "2026-09-01T10:00:00Z",
                "2026-09-01T10:00:00Z",
                null
        );

        acceptedOffer = new OfferResponse(
                OFFER_ID,
                LISTING_ID,
                FARMER_UID,
                BUYER_UID,
                ProfileService.ROLE_MEDIATOR_BUYER,
                "crop_paddy",
                "Paddy / Rice",
                40.0,
                "QUINTAL",
                new BigDecimal("2300.00"),
                "QUINTAL",
                new BigDecimal("2500.00"),
                "Proposal Accepted",
                OfferStatus.ACCEPTED.name(),
                2,
                FARMER_UID,
                40.0,
                new BigDecimal("2300.00"),
                "QUINTAL",
                null,
                "2026-09-02T10:00:00Z",
                "2026-09-02T10:05:00Z",
                null,
                "2026-09-02T10:05:00Z",
                false,
                false,
                false,
                false
        );
    }

    @Test
    void testCreateOrderFromAcceptedOffer_Success() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(activeListing);

        OrderResponse response = orderService.createOrderFromOffer(BUYER_UID, OFFER_ID);

        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(OFFER_ID, response.getOfferId());
        assertEquals(LISTING_ID, response.getListingId());
        assertEquals(FARMER_UID, response.getFarmerId());
        assertEquals(BUYER_UID, response.getBuyerUid());
        assertEquals(OrderStatus.PENDING.name(), response.getStatus());
        assertEquals(40.0, response.getTotalQuantity());
        assertEquals(new BigDecimal("2300.00"), response.getAgreedPrice());

        BigDecimal expectedTotal = new BigDecimal("2300.00").multiply(BigDecimal.valueOf(40.0)).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedTotal, response.getTotalAmount());
        assertEquals(expectedTotal, response.getSubtotal());

        assertNotNull(response.getItem());
        assertEquals(40.0, response.getItem().getQuantity());
        assertEquals(expectedTotal, response.getItem().getLineTotal());
    }

    @Test
    void testCreateOrderFromNonAcceptedOffer_ThrowsIllegalStateException() {
        OfferResponse pendingOffer = new OfferResponse(
                "offer_pending_1", LISTING_ID, FARMER_UID, BUYER_UID, ProfileService.ROLE_MEDIATOR_BUYER,
                "crop_paddy", "Paddy", 40.0, "QUINTAL", new BigDecimal("2300.00"), "QUINTAL",
                new BigDecimal("2500.00"), "Pending", OfferStatus.PENDING.name(), 1, FARMER_UID,
                null, null, null, null, "2026-09-02T10:00:00Z", "2026-09-02T10:00:00Z", null, null,
                true, true, true, true
        );

        when(offerService.getOfferById(eq("offer_pending_1"), eq(BUYER_UID))).thenReturn(pendingOffer);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderService.createOrderFromOffer(BUYER_UID, "offer_pending_1")
        );
        assertTrue(ex.getMessage().contains("ACCEPTED"));
    }

    @Test
    void testCreateOrderFromOffer_UnauthorizedCaller_ThrowsAccessDenied() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(OTHER_UID))).thenReturn(acceptedOffer);

        assertThrows(AccessDeniedException.class, () ->
                orderService.createOrderFromOffer(OTHER_UID, OFFER_ID)
        );
    }

    @Test
    void testCreateOrderFromOffer_InsufficientQuantity_ThrowsIllegalArgumentException() {
        ProductListingResponse lowStockListing = new ProductListingResponse(
                LISTING_ID, FARMER_UID, "crop_paddy", "Paddy", 100.0, 10.0, "QUINTAL",
                2500.0, "QUINTAL", new LocationDto(), "Low stock", "GRADE_A",
                "2026-09-01", "2026-09-02", ProductListingStatus.ACTIVE.name(),
                "2026-09-01T10:00:00Z", "2026-09-01T10:00:00Z", null
        );

        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(lowStockListing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrderFromOffer(BUYER_UID, OFFER_ID)
        );
        assertTrue(ex.getMessage().contains("exceeds available listing quantity"));
    }

    @Test
    void testDuplicateOrderCreationFromSameOffer_ThrowsIllegalStateException() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(activeListing);

        // First creation succeeds
        orderService.createOrderFromOffer(BUYER_UID, OFFER_ID);

        // Second creation attempt for same offerId fails with 409 conflict error
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderService.createOrderFromOffer(BUYER_UID, OFFER_ID)
        );
        assertTrue(ex.getMessage().contains("already been created"));
    }

    @Test
    void testOrderLifecycleStateTransitions() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(activeListing);

        OrderResponse created = orderService.createOrderFromOffer(BUYER_UID, OFFER_ID);
        String orderId = created.getOrderId();

        // Farmer confirms PENDING -> CONFIRMED
        OrderResponse confirmed = orderService.confirmOrder(FARMER_UID, orderId);
        assertEquals(OrderStatus.CONFIRMED.name(), confirmed.getStatus());
        assertNotNull(confirmed.getConfirmedAt());

        // Farmer processes CONFIRMED -> PROCESSING
        OrderResponse processing = orderService.processOrder(FARMER_UID, orderId);
        assertEquals(OrderStatus.PROCESSING.name(), processing.getStatus());
        assertNotNull(processing.getProcessedAt());

        // Farmer completes PROCESSING -> COMPLETED
        OrderResponse completed = orderService.completeOrder(FARMER_UID, orderId);
        assertEquals(OrderStatus.COMPLETED.name(), completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void testInvalidStateTransition_ThrowsIllegalStateException() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(activeListing);

        OrderResponse created = orderService.createOrderFromOffer(BUYER_UID, OFFER_ID);
        String orderId = created.getOrderId();

        // Cannot move directly from PENDING to PROCESSING or COMPLETED
        assertThrows(IllegalStateException.class, () -> orderService.processOrder(FARMER_UID, orderId));
        assertThrows(IllegalStateException.class, () -> orderService.completeOrder(FARMER_UID, orderId));
    }

    @Test
    void testCancelOrder_RestoresInventory() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(activeListing);

        OrderResponse created = orderService.createOrderFromOffer(BUYER_UID, OFFER_ID);
        String orderId = created.getOrderId();

        OrderResponse cancelled = orderService.cancelOrder(BUYER_UID, orderId, "Buyer changed mind");
        assertEquals(OrderStatus.CANCELLED.name(), cancelled.getStatus());
        assertEquals("Buyer changed mind", cancelled.getCancellationReason());
        assertEquals(BUYER_UID, cancelled.getCancelledByUid());
        assertNotNull(cancelled.getCancelledAt());

        // Attempting to cancel an already CANCELLED order fails
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(BUYER_UID, orderId, "Cancel again"));
    }

    @Test
    void testGetMyOrdersAndReceivedOrders() {
        when(offerService.getOfferById(eq(OFFER_ID), eq(BUYER_UID))).thenReturn(acceptedOffer);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(activeListing);

        orderService.createOrderFromOffer(BUYER_UID, OFFER_ID);

        OrderPageResponse buyerPage = orderService.getMyOrders(BUYER_UID, "ALL", 0, 10);
        assertEquals(1, buyerPage.getTotalElements());
        assertEquals(BUYER_UID, buyerPage.getItems().get(0).getBuyerUid());

        OrderPageResponse farmerPage = orderService.getReceivedOrders(FARMER_UID, "ALL", 0, 10);
        assertEquals(1, farmerPage.getTotalElements());
        assertEquals(FARMER_UID, farmerPage.getItems().get(0).getFarmerId());
    }
}
