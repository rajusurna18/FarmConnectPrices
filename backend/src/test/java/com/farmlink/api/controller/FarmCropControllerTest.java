package com.farmlink.api.controller;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.FarmCropRequest;
import com.farmlink.api.dto.FarmCropResponse;
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
public class FarmCropControllerTest {

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
    void getFarmCrops_Returns200_WhenFarmOwnedByFarmer() throws Exception {
        CropResponse crop = new CropResponse("crop-paddy", "Rice / Paddy", "CEREAL", "Oryza sativa", "ACTIVE");
        FarmCropResponse fc = new FarmCropResponse("farm-1_crop-paddy_KHARIF", "farm-1", "farmer-uid-123", "crop-paddy", crop, "KHARIF", "ACTIVE", "2026-08-25T00:00:00Z", "2026-08-25T00:00:00Z");

        when(farmService.getFarmCrops("farm-1", "farmer-uid-123")).thenReturn(List.of(fc));

        mockMvc.perform(get("/api/v1/farms/farm-1/crops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cropId").value("crop-paddy"))
                .andExpect(jsonPath("$[0].season").value("KHARIF"));
    }

    @Test
    void addFarmCrop_Returns201_WhenValidRequest() throws Exception {
        CropResponse crop = new CropResponse("crop-wheat", "Wheat", "CEREAL", "Triticum aestivum", "ACTIVE");
        FarmCropResponse fc = new FarmCropResponse("farm-1_crop-wheat_RABI", "farm-1", "farmer-uid-123", "crop-wheat", crop, "RABI", "ACTIVE", "2026-08-25T00:00:00Z", "2026-08-25T00:00:00Z");

        when(farmService.addFarmCrop(eq("farm-1"), eq("farmer-uid-123"), any(FarmCropRequest.class))).thenReturn(fc);

        String jsonBody = """
            {
              "cropId": "crop-wheat",
              "season": "RABI",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/v1/farms/farm-1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cropId").value("crop-wheat"))
                .andExpect(jsonPath("$.season").value("RABI"));
    }

    @Test
    void addFarmCrop_Returns400_WhenDuplicateSeasonRelationship() throws Exception {
        when(farmService.addFarmCrop(eq("farm-1"), eq("farmer-uid-123"), any(FarmCropRequest.class)))
                .thenThrow(new IllegalArgumentException("Crop relationship already exists for this farm and season (KHARIF)."));

        String jsonBody = """
            {
              "cropId": "crop-paddy",
              "season": "KHARIF",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/v1/farms/farm-1/crops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }
}
