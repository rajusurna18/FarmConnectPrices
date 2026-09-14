package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.DeliveryService;
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

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryService deliveryService;

    private static final String BUYER_UID = "buyer_uid_100";
    private static final String FARMER_UID = "farmer_uid_200";
    private static final String DELIVERY_ID = "del_order_100";
    private static final String ORDER_ID = "order_100";

    private DeliveryResponse mockDeliveryResponse;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(BUYER_UID, "buyer@test.com", "Buyer User", true);
        SecurityContextHolder.getContext().setAuthentication(token);

        mockDeliveryResponse = new DeliveryResponse(
                DELIVERY_ID,
                ORDER_ID,
                FARMER_UID,
                BUYER_UID,
                "CUSTOMER",
                "CREATED",
                new LocationDto("Telangana", "Warangal", "Mandal", "Village", "506001"),
                null,
                null,
                new LocationDto("Telangana", "Hyderabad", "Mandal", "Village", "500001"),
                null,
                null,
                null,
                "TRK-TEST1234",
                null,
                null,
                "2026-09-14T10:00:00Z",
                "2026-09-14T10:00:00Z",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                true
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDelivery_Success_201() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest(ORDER_ID, null);
        when(deliveryService.createDelivery(eq(BUYER_UID), any(CreateDeliveryRequest.class))).thenReturn(mockDeliveryResponse);

        mockMvc.perform(post("/api/v1/marketplace/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryId").value(DELIVERY_ID))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createDelivery_Conflict_409() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest(ORDER_ID, null);
        when(deliveryService.createDelivery(eq(BUYER_UID), any(CreateDeliveryRequest.class)))
                .thenThrow(new IllegalStateException("A delivery request already exists for this order."));

        mockMvc.perform(post("/api/v1/marketplace/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getMyDeliveries_Success_200() throws Exception {
        DeliveryPageResponse pageResponse = new DeliveryPageResponse(List.of(mockDeliveryResponse), 0, 20, 1, 1, false);
        when(deliveryService.getMyDeliveries(eq(BUYER_UID), eq("ALL"), eq(0), eq(20))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/deliveries/mine?status=ALL&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].deliveryId").value(DELIVERY_ID));
    }

    @Test
    void getDeliveryById_Success_200() throws Exception {
        when(deliveryService.getDeliveryById(eq(DELIVERY_ID), eq(BUYER_UID))).thenReturn(mockDeliveryResponse);

        mockMvc.perform(get("/api/v1/marketplace/deliveries/" + DELIVERY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(DELIVERY_ID));
    }

    @Test
    void getDeliveryById_NotFound_404() throws Exception {
        when(deliveryService.getDeliveryById(eq("invalid_id"), eq(BUYER_UID)))
                .thenThrow(new NoSuchElementException("Delivery record not found"));

        mockMvc.perform(get("/api/v1/marketplace/deliveries/invalid_id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markDelivered_Success_200() throws Exception {
        mockDeliveryResponse.setStatus("DELIVERED");
        when(deliveryService.markDelivered(eq(BUYER_UID), eq(DELIVERY_ID))).thenReturn(mockDeliveryResponse);

        mockMvc.perform(post("/api/v1/marketplace/deliveries/" + DELIVERY_ID + "/delivered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void cancelDelivery_Forbidden_403() throws Exception {
        when(deliveryService.cancelDelivery(eq(BUYER_UID), eq(DELIVERY_ID), any()))
                .thenThrow(new AccessDeniedException("Unauthorized to cancel delivery."));

        mockMvc.perform(post("/api/v1/marketplace/deliveries/" + DELIVERY_ID + "/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCall_Returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/marketplace/deliveries/mine"))
                .andExpect(status().isUnauthorized());
    }
}
