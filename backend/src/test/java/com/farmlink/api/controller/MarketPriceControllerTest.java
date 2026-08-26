package com.farmlink.api.controller;

import com.farmlink.api.config.SecurityConfig;
import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.MarketPriceService;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketPriceController.class)
@Import(SecurityConfig.class)
class MarketPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketPriceService marketPriceService;

    @MockBean
    private FirebaseApp firebaseApp;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("test-uid", "user@example.com", "Test User", true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void getMarketPrices_returns200_whenAuthenticated() throws Exception {
        MarketPriceSummaryResponse summary = new MarketPriceSummaryResponse(
                "prc-1", "mkt-1", "Guntur Mandi", "crop-chilli", "Red Chilli",
                "2026-08-26", 18500.0, 22500.0, 20500.0, "INR", "QUINTAL",
                "IMPORTED_DATA", "Development Reference Seed Data", "VERIFIED", "ACTIVE"
        );
        Mockito.when(marketPriceService.getMarketPrices(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/market-prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prc-1"))
                .andExpect(jsonPath("$[0].marketName").value("Guntur Mandi"))
                .andExpect(jsonPath("$[0].modalPrice").value(20500.0));
    }

    @Test
    void getLatestMarketPrice_returns200_whenValidParams() throws Exception {
        MarketSummaryResponse mSummary = new MarketSummaryResponse("mkt-1", "Guntur Mandi", "GNT-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4);
        CropResponse crop = new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE");
        MarketPriceSourceDto source = new MarketPriceSourceDto("IMPORTED_DATA", "Development Reference Seed Data", "ref-1");

        MarketPriceResponse response = new MarketPriceResponse(
                "prc-1", mSummary, crop, "2026-08-26", "2026-08-26T06:00:00Z",
                18500.0, 22500.0, 20500.0, "INR", "QUINTAL", source, "VERIFIED", "ACTIVE",
                "2026-08-26T06:00:00Z", "2026-08-26T06:00:00Z"
        );

        Mockito.when(marketPriceService.getLatestMarketPrice("mkt-1", "crop-chilli")).thenReturn(response);

        mockMvc.perform(get("/api/v1/market-prices/latest?marketId=mkt-1&cropId=crop-chilli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prc-1"))
                .andExpect(jsonPath("$.market.id").value("mkt-1"))
                .andExpect(jsonPath("$.crop.id").value("crop-chilli"));
    }

    @Test
    void getMarketPriceHistory_returns200_whenValidParams() throws Exception {
        MarketSummaryResponse mSummary = new MarketSummaryResponse("mkt-1", "Guntur Mandi", "GNT-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4);
        CropResponse crop = new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE");
        MarketPriceSourceDto source = new MarketPriceSourceDto("IMPORTED_DATA", "Development Reference Seed Data", "ref-1");

        MarketPriceResponse p1 = new MarketPriceResponse(
                "prc-1", mSummary, crop, "2026-08-26", "2026-08-26T06:00:00Z",
                18500.0, 22500.0, 20500.0, "INR", "QUINTAL", source, "VERIFIED", "ACTIVE",
                "2026-08-26T06:00:00Z", "2026-08-26T06:00:00Z"
        );

        Mockito.when(marketPriceService.getMarketPriceHistory("mkt-1", "crop-chilli", null, null))
                .thenReturn(List.of(p1));

        mockMvc.perform(get("/api/v1/market-prices/history?marketId=mkt-1&cropId=crop-chilli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prc-1"));
    }

    @Test
    void getMarketPriceById_returns200_whenFound() throws Exception {
        MarketSummaryResponse mSummary = new MarketSummaryResponse("mkt-1", "Guntur Mandi", "GNT-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4);
        CropResponse crop = new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE");
        MarketPriceSourceDto source = new MarketPriceSourceDto("IMPORTED_DATA", "Development Reference Seed Data", "ref-1");

        MarketPriceResponse response = new MarketPriceResponse(
                "prc-1", mSummary, crop, "2026-08-26", "2026-08-26T06:00:00Z",
                18500.0, 22500.0, 20500.0, "INR", "QUINTAL", source, "VERIFIED", "ACTIVE",
                "2026-08-26T06:00:00Z", "2026-08-26T06:00:00Z"
        );

        Mockito.when(marketPriceService.getMarketPriceById("prc-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/market-prices/prc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prc-1"));
    }

    @Test
    void getMarketPriceById_returns404_whenNotFound() throws Exception {
        Mockito.when(marketPriceService.getMarketPriceById("prc-invalid"))
                .thenThrow(new NoSuchElementException("Price record not found"));

        mockMvc.perform(get("/api/v1/market-prices/prc-invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientWriteRequests_areForbidden_andReturn405() throws Exception {
        mockMvc.perform(post("/api/v1/market-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minPrice\":100}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/v1/market-prices/prc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minPrice\":100}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/v1/market-prices/prc-1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
