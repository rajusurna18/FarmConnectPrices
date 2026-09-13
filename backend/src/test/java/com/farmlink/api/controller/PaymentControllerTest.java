package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private static final String BUYER_UID = "buyer-uid-123";
    private static final String FARMER_UID = "farmer-uid-456";
    private static final String ORDER_ID = "order-123";
    private static final String PAYMENT_ID = "pay_order-123";

    private PaymentIntentResponse mockIntentResponse;
    private PaymentResponse mockPaymentResponse;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(BUYER_UID, "buyer@test.com", "Buyer User", true);
        SecurityContextHolder.getContext().setAuthentication(token);

        mockIntentResponse = new PaymentIntentResponse(
                "mock_pi_123", PAYMENT_ID, ORDER_ID, new BigDecimal("15000.00"), "INR",
                "MOCK", "mock_order_123", "PENDING", "2026-09-13T10:00:00Z", "2026-09-13T10:15:00Z"
        );

        mockPaymentResponse = new PaymentResponse(
                PAYMENT_ID, ORDER_ID, BUYER_UID, FARMER_UID, "MEDIATOR_BUYER",
                new BigDecimal("15000.00"), "INR", "SUCCESS", "MOCK", "mock_pi_123",
                "mock_order_123", "tx_mock_123", "MOCK_CARD", null, null,
                "2026-09-13T10:00:00Z", "2026-09-13T10:01:00Z", "2026-09-13T10:00:05Z",
                "2026-09-13T10:01:00Z", null, "2026-09-13T10:15:00Z"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreatePaymentIntentSuccess() throws Exception {
        when(paymentService.createPaymentIntent(eq(BUYER_UID), eq(ORDER_ID))).thenReturn(mockIntentResponse);

        CreatePaymentIntentRequest req = new CreatePaymentIntentRequest(ORDER_ID);

        mockMvc.perform(post("/api/v1/marketplace/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testCreatePaymentIntentUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        CreatePaymentIntentRequest req = new CreatePaymentIntentRequest(ORDER_ID);

        mockMvc.perform(post("/api/v1/marketplace/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreatePaymentIntentForbidden() throws Exception {
        when(paymentService.createPaymentIntent(eq(BUYER_UID), eq(ORDER_ID)))
                .thenThrow(new AccessDeniedException("Unauthorized to initiate payment for this order."));

        CreatePaymentIntentRequest req = new CreatePaymentIntentRequest(ORDER_ID);

        mockMvc.perform(post("/api/v1/marketplace/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testVerifyPaymentSuccess() throws Exception {
        when(paymentService.verifyPayment(eq(BUYER_UID), eq(PAYMENT_ID), any())).thenReturn(mockPaymentResponse);

        VerifyPaymentRequest req = new VerifyPaymentRequest("mock_pi_123", "sig", "MOCK_CARD", "SUCCESS", null);

        mockMvc.perform(post("/api/v1/marketplace/payments/" + PAYMENT_ID + "/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID));
    }

    @Test
    void testGetMyPayments() throws Exception {
        PaymentPageResponse pageResponse = new PaymentPageResponse(
                Collections.singletonList(mockPaymentResponse), 0, 20, 1, 1, false
        );

        when(paymentService.getMyPayments(eq(BUYER_UID), eq("ALL"), eq(0), eq(20))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/payments/mine?status=ALL&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetPaymentByIdNotFound() throws Exception {
        when(paymentService.getPaymentById(eq("pay_nonexistent"), eq(BUYER_UID)))
                .thenThrow(new NoSuchElementException("Payment record not found"));

        mockMvc.perform(get("/api/v1/marketplace/payments/pay_nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testProcessWebhookSuccess() throws Exception {
        when(paymentService.processWebhook(eq("MOCK"), any(), any())).thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/v1/marketplace/payments/webhook/MOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Mock-Signature", "mock_sig_val")
                        .content("{\"providerPaymentId\":\"pay_order-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
