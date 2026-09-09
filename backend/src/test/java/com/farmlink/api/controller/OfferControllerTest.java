package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.OfferService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OfferService offerService;

    private static final String BUYER_UID = "buyer-uid-123";
    private static final String FARMER_UID = "farmer-uid-456";

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(BUYER_UID, "buyer@test.com", "Buyer User", true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOffer_Returns201_WhenBuyerAuthenticated() throws Exception {
        OfferResponse response = new OfferResponse(
                "offer-1", "listing-1", FARMER_UID, BUYER_UID, "CUSTOMER",
                "crop_tomato", "Tomato", 300.0, "KG", 2200.0, "QUINTAL", 2500.0,
                "Offer message", "PENDING", 1, FARMER_UID, null, null, null,
                List.of(), "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", "2026-09-15T00:00:00Z", null,
                false, false, false, true
        );

        when(offerService.createOffer(eq(BUYER_UID), any(CreateOfferRequest.class))).thenReturn(response);

        String payload = """
                {
                    "listingId": "listing-1",
                    "offeredQuantity": 300,
                    "quantityUnit": "KG",
                    "offeredPrice": 2200,
                    "priceUnit": "QUINTAL",
                    "message": "Offer message"
                }
                """;

        mockMvc.perform(post("/api/v1/marketplace/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").value("offer-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.offeredPrice").value(2200.0));
    }

    @Test
    void getMyOffers_Returns200() throws Exception {
        OfferResponse response = new OfferResponse(
                "offer-1", "listing-1", FARMER_UID, BUYER_UID, "CUSTOMER",
                "crop_tomato", "Tomato", 300.0, "KG", 2200.0, "QUINTAL", 2500.0,
                "Offer message", "PENDING", 1, FARMER_UID, null, null, null,
                List.of(), "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", "2026-09-15T00:00:00Z", null,
                false, false, false, true
        );
        OfferPageResponse pageResponse = new OfferPageResponse(List.of(response), 0, 20, 1, 1, false);

        when(offerService.getMyOffers(eq(BUYER_UID), any(), eq(0), eq(20))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/offers/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].offerId").value("offer-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void acceptOffer_Returns200() throws Exception {
        OfferResponse response = new OfferResponse(
                "offer-1", "listing-1", FARMER_UID, BUYER_UID, "CUSTOMER",
                "crop_tomato", "Tomato", 300.0, "KG", 2200.0, "QUINTAL", 2500.0,
                "Accepted", "ACCEPTED", 2, FARMER_UID, 300.0, 2200.0, "QUINTAL",
                List.of(), "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", "2026-09-15T00:00:00Z", "2026-09-08T00:00:00Z",
                false, false, false, false
        );

        when(offerService.acceptOffer(eq(BUYER_UID), eq("offer-1"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/marketplace/offers/offer-1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.agreedPrice").value(2200.0));
    }

    @Test
    void createOffer_Returns403_WhenAccessDenied() throws Exception {
        when(offerService.createOffer(eq(BUYER_UID), any()))
                .thenThrow(new AccessDeniedException("Unauthorized user role"));

        String payload = """
                {
                    "listingId": "listing-1",
                    "offeredQuantity": 300,
                    "offeredPrice": 2200
                }
                """;

        mockMvc.perform(post("/api/v1/marketplace/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }
}
