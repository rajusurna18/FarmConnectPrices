package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.ProductListingService;
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
public class ProductListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductListingService listingService;

    private static final String FARMER_UID = "farmer-uid-123";

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
    void createListing_Returns201_WhenFarmerAuthenticated() throws Exception {
        ProductListingResponse response = new ProductListingResponse(
                "listing-1", FARMER_UID, "crop_tomato", "Tomato",
                500.0, 500.0, "KG", 2400.0, "QUINTAL",
                new LocationDto("Telangana", "Warangal", "Enumamula", "Village"),
                "Fresh harvest", "GRADE_A", "2026-09-01", "2026-09-02", "ACTIVE",
                "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", null
        );

        when(listingService.createListing(eq(FARMER_UID), any(CreateListingRequest.class))).thenReturn(response);

        String payload = """
                {
                    "cropId": "crop_tomato",
                    "quantity": 500,
                    "unit": "KG",
                    "askingPrice": 2400,
                    "priceUnit": "QUINTAL",
                    "description": "Fresh harvest"
                }
                """;

        mockMvc.perform(post("/api/v1/marketplace/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value("listing-1"))
                .andExpect(jsonPath("$.cropName").value("Tomato"))
                .andExpect(jsonPath("$.askingPrice").value(2400.0));
    }

    @Test
    void browsePublicListings_Returns200() throws Exception {
        ProductListingResponse listing = new ProductListingResponse(
                "listing-1", FARMER_UID, "crop_tomato", "Tomato",
                500.0, 500.0, "KG", 2400.0, "QUINTAL",
                new LocationDto("Telangana", "Warangal", "Enumamula", "Village"),
                "Fresh harvest", "GRADE_A", "2026-09-01", "2026-09-02", "ACTIVE",
                "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", null
        );
        ListingPageResponse pageResponse = new ListingPageResponse(List.of(listing), 0, 20, 1, 1, false);

        when(listingService.browsePublicListings(any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].listingId").value("listing-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createListing_Returns403_WhenAccessDenied() throws Exception {
        when(listingService.createListing(eq(FARMER_UID), any()))
                .thenThrow(new AccessDeniedException("Marketplace listing creation restricted to Farmers."));

        String payload = """
                {
                    "cropId": "crop_tomato",
                    "quantity": 500,
                    "askingPrice": 2400
                }
                """;

        mockMvc.perform(post("/api/v1/marketplace/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateListing_Returns200_WhenOwnerUpdates() throws Exception {
        ProductListingResponse response = new ProductListingResponse(
                "listing-1", FARMER_UID, "crop_tomato", "Tomato",
                500.0, 400.0, "KG", 2600.0, "QUINTAL",
                new LocationDto("Telangana", "Warangal", "Enumamula", "Village"),
                "Fresh harvest updated", "GRADE_A", "2026-09-01", "2026-09-02", "ACTIVE",
                "2026-09-08T00:00:00Z", "2026-09-08T00:00:00Z", null
        );

        when(listingService.updateListing(eq(FARMER_UID), eq("listing-1"), any(UpdateListingRequest.class)))
                .thenReturn(response);

        String payload = """
                {
                    "askingPrice": 2600,
                    "availableQuantity": 400
                }
                """;

        mockMvc.perform(put("/api/v1/marketplace/listings/listing-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.askingPrice").value(2600.0))
                .andExpect(jsonPath("$.availableQuantity").value(400.0));
    }
}
