package com.farmlink.api.controller;

import com.farmlink.api.dto.BuyerOpportunityDto;
import com.farmlink.api.dto.BuyerOpportunityPageResponse;
import com.farmlink.api.model.ConfidenceLevel;
import com.farmlink.api.model.MatchReasonCode;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.BuyerOpportunityService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class BuyerOpportunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuyerOpportunityService opportunityService;

    private static final String FARMER_UID = "farmer-uid-123";
    private static final String BUYER_UID = "buyer-uid-456";

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(FARMER_UID, "farmer@test.com", "Farmer User", true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getBuyerOpportunities_Returns200_WhenFarmerAuthenticated() throws Exception {
        BuyerOpportunityDto dto = new BuyerOpportunityDto(
                "opp_101",
                "MEDIATOR_BUYER",
                "Mediator / Buyer",
                "Rangareddy, Telangana",
                "crop_tomato",
                "Tomato",
                500.0,
                "KG",
                new BigDecimal("90.00"),
                ConfidenceLevel.HIGH,
                List.of(MatchReasonCode.CROP_MATCH, MatchReasonCode.SAME_DISTRICT),
                "2026-09-18T10:00:00Z"
        );
        BuyerOpportunityPageResponse pageResponse = new BuyerOpportunityPageResponse(List.of(dto), 0, 10, 1, 1, false, "SUCCESS");

        when(opportunityService.getBuyerOpportunities(eq(FARMER_UID), eq("listing-1"), eq(0), eq(10)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/buyer-opportunities")
                        .param("listingId", "listing-1")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].opportunityId").value("opp_101"))
                .andExpect(jsonPath("$.items[0].buyerRoleDisplay").value("Mediator / Buyer"))
                .andExpect(jsonPath("$.items[0].relevanceScore").value(90.00))
                .andExpect(jsonPath("$.statusState").value("SUCCESS"));
    }

    @Test
    void getBuyerOpportunities_Returns403_WhenAccessDeniedFromService() throws Exception {
        when(opportunityService.getBuyerOpportunities(eq(FARMER_UID), eq("listing-other"), anyInt(), anyInt()))
                .thenThrow(new AccessDeniedException("Unauthorized access to listing opportunities."));

        mockMvc.perform(get("/api/v1/marketplace/buyer-opportunities")
                        .param("listingId", "listing-other")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBuyerOpportunities_Returns400_WhenListingIdMissing() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/buyer-opportunities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
