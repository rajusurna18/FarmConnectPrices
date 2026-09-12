package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.OrderService;
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
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private static final String BUYER_UID = "buyer-uid-123";
    private static final String FARMER_UID = "farmer-uid-456";
    private static final String OFFER_ID = "offer-123";
    private static final String ORDER_ID = "order-123";

    private OrderResponse mockOrderResponse;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(BUYER_UID, "buyer@test.com", "Buyer User", true);
        SecurityContextHolder.getContext().setAuthentication(token);

        mockOrderResponse = new OrderResponse(
                ORDER_ID,
                OFFER_ID,
                "listing-1",
                FARMER_UID,
                BUYER_UID,
                "MEDIATOR_BUYER",
                "crop-paddy",
                "Paddy",
                "PENDING",
                50.0,
                "QUINTAL",
                new BigDecimal("2400.00"),
                "QUINTAL",
                new BigDecimal("120000.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("120000.00"),
                "INR",
                new OrderItemResponse("item-1", ORDER_ID, "listing-1", "crop-paddy", "Paddy", 50.0, "QUINTAL", new BigDecimal("2400.00"), "QUINTAL", new BigDecimal("120000.00")),
                null,
                null,
                "2026-09-02T10:00:00Z",
                "2026-09-02T10:00:00Z",
                null, null, null, null,
                false, false, false, true
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateOrderFromOffer_Success() throws Exception {
        when(orderService.createOrderFromOffer(eq(BUYER_UID), eq(OFFER_ID))).thenReturn(mockOrderResponse);

        mockMvc.perform(post("/api/v1/marketplace/orders/from-offer/" + OFFER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.agreedPrice").value(2400.00))
                .andExpect(jsonPath("$.totalAmount").value(120000.00));
    }

    @Test
    void testCreateOrderFromOffer_DuplicateOrder_ReturnsConflict409() throws Exception {
        when(orderService.createOrderFromOffer(eq(BUYER_UID), eq(OFFER_ID)))
                .thenThrow(new IllegalStateException("An order has already been created from this accepted offer."));

        mockMvc.perform(post("/api/v1/marketplace/orders/from-offer/" + OFFER_ID))
                .andExpect(status().isConflict())
                .andExpect(content().string("An order has already been created from this accepted offer."));
    }

    @Test
    void testCreateOrderFromOffer_Unauthorized_ReturnsForbidden403() throws Exception {
        when(orderService.createOrderFromOffer(eq(BUYER_UID), eq(OFFER_ID)))
                .thenThrow(new AccessDeniedException("Unauthorized caller."));

        mockMvc.perform(post("/api/v1/marketplace/orders/from-offer/" + OFFER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetMyOrders_Success() throws Exception {
        OrderPageResponse pageResponse = new OrderPageResponse(List.of(mockOrderResponse), 0, 20, 1, 1, false);
        when(orderService.getMyOrders(eq(BUYER_UID), eq("ALL"), eq(0), eq(20))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/orders/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetOrderById_Success() throws Exception {
        when(orderService.getOrderById(eq(ORDER_ID), eq(BUYER_UID))).thenReturn(mockOrderResponse);

        mockMvc.perform(get("/api/v1/marketplace/orders/" + ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID));
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        mockOrderResponse.setStatus("CANCELLED");
        when(orderService.cancelOrder(eq(BUYER_UID), eq(ORDER_ID), any())).thenReturn(mockOrderResponse);

        CancelOrderRequest req = new CancelOrderRequest("Price changed");

        mockMvc.perform(post("/api/v1/marketplace/orders/" + ORDER_ID + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testUnauthenticatedRequest_ReturnsUnauthorized401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/v1/marketplace/orders/from-offer/" + OFFER_ID))
                .andExpect(status().isUnauthorized());
    }
}
