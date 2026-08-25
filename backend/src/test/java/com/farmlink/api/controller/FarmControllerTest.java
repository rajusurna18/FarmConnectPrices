package com.farmlink.api.controller;

import com.farmlink.api.dto.FarmRequest;
import com.farmlink.api.dto.FarmResponse;
import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
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
public class FarmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FarmService farmService;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("farmer-uid-123", "farmer@test.com", "Test Farmer", true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getFarms_Returns200_WhenFarmerAuthenticated() throws Exception {
        LocationDto loc = new LocationDto("Telangana", "Warangal", "Enumamula", "Village 1", "506002");
        FarmResponse farm = new FarmResponse("farm-1", "farmer-uid-123", "Green Acres", loc, 5.5, "ACRE", "ACTIVE", "2026-08-25T00:00:00Z", "2026-08-25T00:00:00Z");

        when(farmService.getFarmsForOwner("farmer-uid-123")).thenReturn(List.of(farm));

        mockMvc.perform(get("/api/v1/farms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("farm-1"))
                .andExpect(jsonPath("$[0].name").value("Green Acres"))
                .andExpect(jsonPath("$[0].landAreaUnit").value("ACRE"));
    }

    @Test
    void getFarms_Returns403_WhenUserRoleForbidden() throws Exception {
        when(farmService.getFarmsForOwner("farmer-uid-123"))
                .thenThrow(new AccessDeniedException("Farm management access is strictly restricted to Farmers."));

        mockMvc.perform(get("/api/v1/farms"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFarm_Returns201_WhenValidRequest() throws Exception {
        LocationDto loc = new LocationDto("Telangana", "Warangal", "Enumamula", "Village 1", "506002");
        FarmResponse farm = new FarmResponse("farm-new", "farmer-uid-123", "Sunshine Farm", loc, 10.0, "ACRE", "ACTIVE", "2026-08-25T00:00:00Z", "2026-08-25T00:00:00Z");

        when(farmService.createFarm(eq("farmer-uid-123"), any(FarmRequest.class))).thenReturn(farm);

        String jsonBody = """
            {
              "name": "Sunshine Farm",
              "landArea": 10.0,
              "landAreaUnit": "ACRE",
              "status": "ACTIVE",
              "location": {
                "state": "Telangana",
                "district": "Warangal",
                "mandal": "Enumamula",
                "village": "Village 1",
                "pincode": "506002"
              }
            }
            """;

        mockMvc.perform(post("/api/v1/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sunshine Farm"))
                .andExpect(jsonPath("$.landArea").value(10.0));
    }

    @Test
    void getFarmById_Returns403_WhenAccessingAnotherFarmersFarm() throws Exception {
        when(farmService.getFarmById("farm-other", "farmer-uid-123"))
                .thenThrow(new AccessDeniedException("Access denied. You do not own this farm."));

        mockMvc.perform(get("/api/v1/farms/farm-other"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFarmById_Returns404_WhenFarmDoesNotExist() throws Exception {
        when(farmService.getFarmById("farm-missing", "farmer-uid-123"))
                .thenThrow(new NoSuchElementException("Farm not found with ID: farm-missing"));

        mockMvc.perform(get("/api/v1/farms/farm-missing"))
                .andExpect(status().isNotFound());
    }
}
