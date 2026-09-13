package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.PaymentStatus;
import com.farmlink.api.provider.MockPaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private OrderService orderService;

    private MockPaymentProvider paymentProvider;
    private PaymentService paymentService;

    private static final String BUYER_UID = "buyer_uid_100";
    private static final String FARMER_UID = "farmer_uid_200";
    private static final String OTHER_UID = "other_uid_300";

    private static final String ORDER_ID_CONFIRMED = "order_confirmed_1";
    private static final String ORDER_ID_PENDING = "order_pending_1";

    private OrderResponse confirmedOrder;
    private OrderResponse pendingOrder;

    @BeforeEach
    void setUp() {
        paymentProvider = new MockPaymentProvider();
        paymentService = new PaymentService(null, orderService, paymentProvider, null, null);

        OrderItemResponse item = new OrderItemResponse(
                "item_1", ORDER_ID_CONFIRMED, "listing_1", "crop_paddy", "Paddy",
                10.0, "QUINTAL", new BigDecimal("2500.00"), "QUINTAL", new BigDecimal("25000.00")
        );

        confirmedOrder = new OrderResponse(
                ORDER_ID_CONFIRMED, "offer_1", "listing_1", FARMER_UID, BUYER_UID, "MEDIATOR_BUYER",
                "crop_paddy", "Paddy", "CONFIRMED", 10.0, "QUINTAL", new BigDecimal("2500.00"), "QUINTAL",
                new BigDecimal("25000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000.00"), "INR",
                item, null, null, "2026-09-13T10:00:00Z", "2026-09-13T10:05:00Z", "2026-09-13T10:05:00Z",
                null, null, null, false, true, false, true
        );

        pendingOrder = new OrderResponse(
                ORDER_ID_PENDING, "offer_2", "listing_1", FARMER_UID, BUYER_UID, "MEDIATOR_BUYER",
                "crop_paddy", "Paddy", "PENDING", 10.0, "QUINTAL", new BigDecimal("2500.00"), "QUINTAL",
                new BigDecimal("25000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25000.00"), "INR",
                item, null, null, "2026-09-13T10:00:00Z", "2026-09-13T10:00:00Z", null,
                null, null, null, true, false, false, true
        );
    }

    @Test
    void testCreatePaymentIntentSuccess() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);

        PaymentIntentResponse response = paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        assertNotNull(response);
        assertEquals(ORDER_ID_CONFIRMED, response.getOrderId());
        assertEquals("pay_" + ORDER_ID_CONFIRMED, response.getPaymentId());
        assertEquals(new BigDecimal("25000.00"), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertEquals(PaymentStatus.PENDING.name(), response.getStatus());
        assertTrue(response.getPaymentIntentId().startsWith("mock_pi_"));
    }

    @Test
    void testCreatePaymentIntentIdempotency() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);

        PaymentIntentResponse response1 = paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);
        PaymentIntentResponse response2 = paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals(response1.getPaymentIntentId(), response2.getPaymentIntentId());
        assertEquals(response1.getPaymentId(), response2.getPaymentId());
    }

    @Test
    void testCreatePaymentIntentPendingOrderRejected() {
        when(orderService.getOrderById(eq(ORDER_ID_PENDING), eq(BUYER_UID))).thenReturn(pendingOrder);

        assertThrows(IllegalStateException.class, () -> paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_PENDING));
    }

    @Test
    void testCreatePaymentIntentFarmerRejected() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(FARMER_UID))).thenReturn(confirmedOrder);

        assertThrows(AccessDeniedException.class, () -> paymentService.createPaymentIntent(FARMER_UID, ORDER_ID_CONFIRMED));
    }

    @Test
    void testCreatePaymentIntentOtherUserRejected() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(OTHER_UID))).thenReturn(confirmedOrder);

        assertThrows(AccessDeniedException.class, () -> paymentService.createPaymentIntent(OTHER_UID, ORDER_ID_CONFIRMED));
    }

    @Test
    void testVerifyPaymentSuccess() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);

        PaymentIntentResponse intent = paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest();
        verifyReq.setProviderPaymentId(intent.getPaymentIntentId());
        verifyReq.setPaymentMethod("UPI");
        verifyReq.setSimulatedStatus("SUCCESS");

        PaymentResponse response = paymentService.verifyPayment(BUYER_UID, intent.getPaymentId(), verifyReq);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS.name(), response.getStatus());
        assertEquals("pay_" + ORDER_ID_CONFIRMED, response.getPaymentId());
        assertEquals(new BigDecimal("25000.00"), response.getAmount());
        assertNotNull(response.getVerifiedAt());
        assertNotNull(response.getTransactionReference());
        assertEquals("UPI", response.getPaymentMethod());
    }

    @Test
    void testVerifyPaymentSimulatedFailure() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);

        PaymentIntentResponse intent = paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest();
        verifyReq.setProviderPaymentId(intent.getPaymentIntentId());
        verifyReq.setSimulatedStatus("FAILED");
        verifyReq.setFailureReason("Declined by issuing bank");

        PaymentResponse response = paymentService.verifyPayment(BUYER_UID, intent.getPaymentId(), verifyReq);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED.name(), response.getStatus());
        assertEquals("SIMULATED_MOCK_FAILURE", response.getFailureCode());
        assertEquals("Declined by issuing bank", response.getFailureMessage());
        assertNotNull(response.getFailedAt());
    }

    @Test
    void testVerifyPaymentDuplicateSuccessIsIdempotent() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);

        PaymentIntentResponse intent = paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest();
        verifyReq.setProviderPaymentId(intent.getPaymentIntentId());
        verifyReq.setSimulatedStatus("SUCCESS");

        PaymentResponse resp1 = paymentService.verifyPayment(BUYER_UID, intent.getPaymentId(), verifyReq);
        PaymentResponse resp2 = paymentService.verifyPayment(BUYER_UID, intent.getPaymentId(), verifyReq);

        assertEquals(PaymentStatus.SUCCESS.name(), resp1.getStatus());
        assertEquals(PaymentStatus.SUCCESS.name(), resp2.getStatus());
        assertEquals(resp1.getVerifiedAt(), resp2.getVerifiedAt());
    }

    @Test
    void testGetMyPayments() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);
        paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        PaymentPageResponse page = paymentService.getMyPayments(BUYER_UID, "ALL", 0, 20);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getItems().size());
        assertEquals(BUYER_UID, page.getItems().get(0).getBuyerUid());
    }

    @Test
    void testGetReceivedPayments() {
        when(orderService.getOrderById(eq(ORDER_ID_CONFIRMED), eq(BUYER_UID))).thenReturn(confirmedOrder);
        paymentService.createPaymentIntent(BUYER_UID, ORDER_ID_CONFIRMED);

        PaymentPageResponse page = paymentService.getReceivedPayments(FARMER_UID, "ALL", 0, 20);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getItems().size());
        assertEquals(FARMER_UID, page.getItems().get(0).getFarmerId());
    }
}
