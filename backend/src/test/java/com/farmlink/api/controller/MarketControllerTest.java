package com.farmlink.api.controller;

import com.farmlink.api.config.SecurityConfig;
import com.farmlink.api.dto.MarketCropResponse;
import com.farmlink.api.dto.MarketResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.MarketService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketController.class)
@Import(SecurityConfig.class)
class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketService marketService;

    @MockBean
    private FirebaseApp firebaseApp;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("test-uid", "user@example.com", "Test User", true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void getMarkets_returns200_whenAuthenticated() throws Exception {
        MarketSummaryResponse summary = new MarketSummaryResponse(
                "mkt-1", "Guntur Mandi", "GNT-001", "MANDI", "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4
        );
        Mockito.when(marketService.getMarkets(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/markets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mkt-1"))
                .andExpect(jsonPath("$[0].code").value("GNT-001"));
    }

    @Test
    void getMarketById_returns200_whenFound() throws Exception {
        MarketResponse response = new MarketResponse(
                "mkt-1", "Guntur Mandi", "GNT-001", "MANDI",
                new LocationDto("Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
                16.29, 80.43, "ACTIVE", "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
        );
        Mockito.when(marketService.getMarketById("mkt-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/markets/mkt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mkt-1"))
                .andExpect(jsonPath("$.name").value("Guntur Mandi"));
    }

    @Test
    void getMarketById_returns404_whenNotFound() throws Exception {
        Mockito.when(marketService.getMarketById("mkt-invalid")).thenThrow(new NoSuchElementException("Market not found"));

        mockMvc.perform(get("/api/v1/markets/mkt-invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMarketCrops_returns200_whenValid() throws Exception {
        MarketCropResponse mc = new MarketCropResponse("mc-1", "mkt-1", "crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE", "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z");
        Mockito.when(marketService.getMarketCrops("mkt-1")).thenReturn(List.of(mc));

        mockMvc.perform(get("/api/v1/markets/mkt-1/crops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cropName").value("Red Chilli"));
    }

    @Test
    void writeRequests_areNotSupportedOrAllowed() throws Exception {
        // Normal users cannot POST or create markets (HTTP 405 Method Not Allowed)
        mockMvc.perform(post("/api/v1/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fake Mandi\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
