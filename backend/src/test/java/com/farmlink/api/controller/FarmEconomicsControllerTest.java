package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmEconomicsService;
import com.farmlink.api.service.FarmService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FarmEconomicsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FarmEconomicsService farmEconomicsService;

    @MockBean
    private FarmService farmService;

    private static final String FARMER_UID = "farmer-uid-100";
    private static final String OTHER_ROLE_UID = "buyer-uid-200";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String uid) {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(uid, uid + "@example.com", uid);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void testGetEconomicRecordsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/farm-economics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetEconomicRecordsForbiddenRole() throws Exception {
        authenticate(OTHER_ROLE_UID);
        doThrow(new AccessDeniedException("Access strictly restricted to Farmers"))
                .when(farmService).verifyFarmerRole(OTHER_ROLE_UID);

        mockMvc.perform(get("/api/v1/farm-economics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetEconomicRecordsSuccess() throws Exception {
        authenticate(FARMER_UID);
        doNothing().when(farmService).verifyFarmerRole(FARMER_UID);

        FarmEconomicRecordResponse rec = new FarmEconomicRecordResponse();
        rec.setId("rec-1");
        rec.setOwnerUid(FARMER_UID);
        rec.setFarmId("farm-1");
        rec.setCropId("crop-1");
        rec.setSeason("KHARIF");
        rec.setCultivatedArea(BigDecimal.valueOf(5));
        rec.setCultivatedAreaUnit("ACRE");
        rec.setExpectedYield(BigDecimal.valueOf(10));
        rec.setYieldUnit("QUINTAL");

        when(farmEconomicsService.getEconomicRecordsForOwner(FARMER_UID)).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/farm-economics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rec-1"))
                .andExpect(jsonPath("$[0].farmId").value("farm-1"));
    }

    @Test
    void testCreateEconomicRecordSuccess() throws Exception {
        authenticate(FARMER_UID);
        doNothing().when(farmService).verifyFarmerRole(FARMER_UID);

        FarmEconomicRecordResponse response = new FarmEconomicRecordResponse();
        response.setId("rec-new");
        response.setOwnerUid(FARMER_UID);
        response.setTotalCost(BigDecimal.valueOf(11900));

        when(farmEconomicsService.createEconomicRecord(eq(FARMER_UID), any())).thenReturn(response);

        String jsonPayload = """
            {
              "farmId": "farm-1",
              "cropId": "crop-1",
              "season": "KHARIF",
              "cultivatedArea": 5,
              "cultivatedAreaUnit": "ACRE",
              "expectedYield": 10,
              "yieldUnit": "QUINTAL",
              "productionCosts": [
                { "category": "SEEDS", "amount": 2500 },
                { "category": "FERTILIZER", "amount": 4000 }
              ],
              "sellingCosts": {
                "transportationCost": 300,
                "otherSellingCosts": 100
              }
            }
            """;

        mockMvc.perform(post("/api/v1/farm-economics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("rec-new"))
                .andExpect(jsonPath("$.totalCost").value(11900));
    }

    @Test
    void testEvaluateProfitabilitySuccess() throws Exception {
        authenticate(FARMER_UID);
        doNothing().when(farmService).verifyFarmerRole(FARMER_UID);

        FarmProfitabilityEvaluationResponse response = new FarmProfitabilityEvaluationResponse();
        response.setStatus("SUCCESS");
        response.setProfitabilityStatus("PROFITABLE");
        response.setEstimatedProfit(BigDecimal.valueOf(16100));

        when(farmEconomicsService.evaluateProfitability(eq(FARMER_UID), any())).thenReturn(response);

        String jsonPayload = """
            {
              "farmId": "farm-1",
              "cropId": "crop-1",
              "marketId": "market-1",
              "expectedYield": 10,
              "yieldUnit": "QUINTAL",
              "productionCosts": [
                { "category": "SEEDS", "amount": 2500 }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/farm-economics/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.profitabilityStatus").value("PROFITABLE"))
                .andExpect(jsonPath("$.estimatedProfit").value(16100));
    }
}
