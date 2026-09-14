package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeliveryServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ProductListingService listingService;

    @Mock
    private ProfileService profileService;

    private DeliveryService deliveryService;

    private static final String FARMER_UID = "farmer_uid_100";
    private static final String BUYER_UID = "buyer_uid_200";
    private static final String OTHER_UID = "other_uid_300";
    private static final String ORDER_ID = "order_test_100";
    private static final String PAYMENT_ID = "pay_order_test_100";
    private static final String LISTING_ID = "listing_test_1";

    private OrderResponse confirmedOrder;
    private PaymentResponse successPayment;
    private ProductListingResponse listingResponse;
    private ProfileResponse buyerProfile;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(null, orderService, paymentService, listingService, profileService, null);

        confirmedOrder = new OrderResponse(
                ORDER_ID,
                "offer_1",
                LISTING_ID,
                FARMER_UID,
                BUYER_UID,
                "CUSTOMER",
                "crop_paddy",
                "Paddy / Rice",
                OrderStatus.CONFIRMED.name(),
                10.0,
                "QUINTAL",
                new BigDecimal("2000.00"),
                "QUINTAL",
                new BigDecimal("20000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20000.00"),
                "INR",
                null,
                null,
                null,
                "2026-09-10T10:00:00Z",
                "2026-09-10T10:05:00Z",
                "2026-09-10T10:05:00Z",
                null,
                null,
                null,
                false,
                true,
                false,
                true
        );

        successPayment = new PaymentResponse(
                PAYMENT_ID,
                ORDER_ID,
                BUYER_UID,
                FARMER_UID,
                "CUSTOMER",
                new BigDecimal("20000.00"),
                "INR",
                PaymentStatus.SUCCESS.name(),
                "RAZORPAY_MOCK",
                "pay_mock_123",
                "order_mock_123",
                "txn_123456",
                "UPI",
                null,
                null,
                "2026-09-10T10:10:00Z",
                "2026-09-10T10:11:00Z",
                "2026-09-10T10:10:00Z",
                "2026-09-10T10:11:00Z",
                null,
                "2026-09-10T10:25:00Z"
        );

        listingResponse = new ProductListingResponse(
                LISTING_ID,
                FARMER_UID,
                "crop_paddy",
                "Paddy / Rice",
                100.0,
                90.0,
                "QUINTAL",
                2000.0,
                "QUINTAL",
                new LocationDto("Telangana", "Warangal", "Enumamula", "Farm Village"),
                "Fresh Paddy",
                "GRADE_A",
                "2026-09-01",
                "2026-09-02",
                ProductListingStatus.ACTIVE.name(),
                "2026-09-01T10:00:00Z",
                "2026-09-01T10:00:00Z",
                null
        );

        buyerProfile = new ProfileResponse(
                BUYER_UID,
                "Buyer Name",
                "buyer@test.com",
                true,
                "CUSTOMER",
                "Customer",
                "ACTIVE",
                "9876543210",
                new LocationDto("Telangana", "Hyderabad", "Kukatpally", "Destination Village", "500072"),
                "Buyer Business",
                "123 Main St",
                true
        );
    }

    @Test
    void createDelivery_Success() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(listingResponse);
        when(profileService.getProfile(eq(BUYER_UID), any(), eq(true), any())).thenReturn(buyerProfile);

        CreateDeliveryRequest req = new CreateDeliveryRequest(ORDER_ID, null);
        DeliveryResponse resp = deliveryService.createDelivery(BUYER_UID, req);

        assertNotNull(resp);
        assertEquals("del_" + ORDER_ID, resp.getDeliveryId());
        assertEquals(ORDER_ID, resp.getOrderId());
        assertEquals(DeliveryStatus.CREATED.name(), resp.getStatus());
        assertNotNull(resp.getPickupAddress());
        assertNotNull(resp.getDeliveryAddress());
    }

    @Test
    void createDelivery_FailsWhenPaymentNotSuccess() {
        PaymentResponse failedPayment = new PaymentResponse(
                PAYMENT_ID, ORDER_ID, BUYER_UID, FARMER_UID, "CUSTOMER",
                new BigDecimal("20000.00"), "INR", PaymentStatus.PENDING.name(),
                "RAZORPAY_MOCK", null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(failedPayment);

        CreateDeliveryRequest req = new CreateDeliveryRequest(ORDER_ID, null);
        assertThrows(IllegalStateException.class, () -> deliveryService.createDelivery(BUYER_UID, req));
    }

    @Test
    void createDelivery_FailsOnDuplicateRequest() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);
        when(listingService.getListingById(eq(LISTING_ID), eq(BUYER_UID))).thenReturn(listingResponse);
        when(profileService.getProfile(eq(BUYER_UID), any(), eq(true), any())).thenReturn(buyerProfile);

        CreateDeliveryRequest req = new CreateDeliveryRequest(ORDER_ID, null);
        deliveryService.createDelivery(BUYER_UID, req);

        assertThrows(IllegalStateException.class, () -> deliveryService.createDelivery(BUYER_UID, req));
    }

    @Test
    void fullDeliveryLifecycle_StateTransitionsSuccess() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);

        // 1. Create
        CreateDeliveryRequest req = new CreateDeliveryRequest(ORDER_ID, null);
        DeliveryResponse created = deliveryService.createDelivery(BUYER_UID, req);
        assertEquals(DeliveryStatus.CREATED.name(), created.getStatus());

        // 2. Assign Partner
        AssignDeliveryPartnerRequest assignReq = new AssignDeliveryPartnerRequest("Ramesh Transport", "9988776655", "TRUCK", "TS08AB1234");
        DeliveryResponse assigned = deliveryService.assignPartner(FARMER_UID, created.getDeliveryId(), assignReq);
        assertEquals(DeliveryStatus.ASSIGNED.name(), assigned.getStatus());
        assertNotNull(assigned.getPartner());

        // 3. Mark Ready
        DeliveryResponse ready = deliveryService.markReadyForPickup(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.READY_FOR_PICKUP.name(), ready.getStatus());

        // 4. Mark Picked Up
        DeliveryResponse pickedUp = deliveryService.markPickedUp(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.PICKED_UP.name(), pickedUp.getStatus());

        // 5. Mark In Transit
        DeliveryResponse inTransit = deliveryService.markInTransit(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.IN_TRANSIT.name(), inTransit.getStatus());

        // 6. Mark Out For Delivery
        DeliveryResponse outForDelivery = deliveryService.markOutForDelivery(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.OUT_FOR_DELIVERY.name(), outForDelivery.getStatus());

        // 7. Mark Delivered & Order Completion Integration
        DeliveryResponse delivered = deliveryService.markDelivered(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.DELIVERED.name(), delivered.getStatus());
        assertNotNull(delivered.getDeliveredAt());
    }

    @Test
    void markDelivered_IdempotencyTest() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);

        DeliveryResponse created = deliveryService.createDelivery(BUYER_UID, new CreateDeliveryRequest(ORDER_ID, null));
        deliveryService.assignPartner(FARMER_UID, created.getDeliveryId(), new AssignDeliveryPartnerRequest("Driver", "9900990099", "VAN", "TS01AA1111"));
        deliveryService.markReadyForPickup(FARMER_UID, created.getDeliveryId());
        deliveryService.markPickedUp(FARMER_UID, created.getDeliveryId());
        deliveryService.markInTransit(FARMER_UID, created.getDeliveryId());
        deliveryService.markOutForDelivery(FARMER_UID, created.getDeliveryId());

        DeliveryResponse d1 = deliveryService.markDelivered(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.DELIVERED.name(), d1.getStatus());

        // Repeat call must succeed idempotently
        DeliveryResponse d2 = deliveryService.markDelivered(FARMER_UID, created.getDeliveryId());
        assertEquals(DeliveryStatus.DELIVERED.name(), d2.getStatus());
    }

    @Test
    void cancelDelivery_SuccessPriorToPickup() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);

        DeliveryResponse created = deliveryService.createDelivery(BUYER_UID, new CreateDeliveryRequest(ORDER_ID, null));

        DeliveryResponse cancelled = deliveryService.cancelDelivery(BUYER_UID, created.getDeliveryId(), new CancelDeliveryRequest("Changed mind"));
        assertEquals(DeliveryStatus.CANCELLED.name(), cancelled.getStatus());
        assertEquals("Changed mind", cancelled.getCancellationReason());
    }

    @Test
    void cancelDelivery_FailsAfterPickup() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);

        DeliveryResponse created = deliveryService.createDelivery(BUYER_UID, new CreateDeliveryRequest(ORDER_ID, null));
        deliveryService.assignPartner(FARMER_UID, created.getDeliveryId(), new AssignDeliveryPartnerRequest("Driver", "9900990099", "VAN", "TS01AA1111"));
        deliveryService.markReadyForPickup(FARMER_UID, created.getDeliveryId());
        deliveryService.markPickedUp(FARMER_UID, created.getDeliveryId());

        assertThrows(IllegalStateException.class, () -> deliveryService.cancelDelivery(BUYER_UID, created.getDeliveryId(), new CancelDeliveryRequest("Cancel attempt")));
    }

    @Test
    void unauthorizedUser_CannotAccessOrModify() {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(confirmedOrder);
        when(paymentService.getPaymentById(eq(PAYMENT_ID), eq(BUYER_UID))).thenReturn(successPayment);

        DeliveryResponse created = deliveryService.createDelivery(BUYER_UID, new CreateDeliveryRequest(ORDER_ID, null));

        assertThrows(AccessDeniedException.class, () -> deliveryService.getDeliveryById(created.getDeliveryId(), OTHER_UID));
        assertThrows(AccessDeniedException.class, () -> deliveryService.assignPartner(OTHER_UID, created.getDeliveryId(), new AssignDeliveryPartnerRequest("Hack", "00", "TRUCK", "00")));
    }
}
