package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.exception.DuplicateRecordException;
import com.farmlink.api.exception.FirestoreQuotaExhaustedException;
import com.farmlink.api.exception.InvalidImageSignatureException;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.CropDiagnosticService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.firebase.use-emulator=true",
        "app.firebase.storage-bucket=farmconnectprices.appspot.com"
})
public class CropDiagnosticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CropDiagnosticService diagnosticService;

    private static final String TEST_UID = "farmer-uid-123";
    private static final String DIAGNOSTIC_ID = "diag-abc-123";
    private static final String CROP_ID = "crop-tomato-01";

    private CreateDiagnosticRequest sampleRequest;
    private DiagnosticRecordResponse sampleResponse;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                TEST_UID, "farmer@test.com", "Test Farmer", true
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        DiagnosticImagesDto images = new DiagnosticImagesDto(
                new DiagnosticImagesDto.ImageMetadataDto("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/original.jpg", "original.jpg", "image/jpeg", 1024L, 800, 600),
                new DiagnosticImagesDto.ImageMetadataDto("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/diagnostic.webp", "diagnostic.webp", "image/webp", 512L, 512, 512),
                new DiagnosticImagesDto.ImageMetadataDto("users/" + TEST_UID + "/diagnostics/" + DIAGNOSTIC_ID + "/display.webp", "display.webp", "image/webp", 256L, 256, 256)
        );

        sampleRequest = new CreateDiagnosticRequest();
        sampleRequest.setDiagnosticId(DIAGNOSTIC_ID);
        sampleRequest.setFarmId("farm-1");
        sampleRequest.setCropId(CROP_ID);
        sampleRequest.setSymptoms("Yellowing of lower leaves and spots");
        sampleRequest.setAffectedPart("LEAF");
        sampleRequest.setImages(images);

        sampleResponse = new DiagnosticRecordResponse(
                DIAGNOSTIC_ID,
                TEST_UID,
                "farm-1",
                "farmcrop-1",
                CROP_ID,
                "Tomato",
                "Yellowing of lower leaves and spots",
                "LEAF",
                images,
                "PENDING_ANALYSIS",
                null,
                "2026-11-13T20:00:00Z",
                "2026-11-13T20:00:00Z"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // POST /api/v1/diagnostics
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnostics - 201 Created on valid request")
    void createDiagnostic_Success_Returns201() throws Exception {
        when(diagnosticService.createDiagnostic(eq(TEST_UID), any(CreateDiagnosticRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DIAGNOSTIC_ID))
                .andExpect(jsonPath("$.farmerUid").value(TEST_UID))
                .andExpect(jsonPath("$.cropName").value("Tomato"))
                .andExpect(jsonPath("$.status").value("PENDING_ANALYSIS"));
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 401 Unauthorized when unauthenticated")
    void createDiagnostic_Unauthenticated_Returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 400 Bad Request on invalid ID format")
    void createDiagnostic_InvalidIdFormat_Returns400() throws Exception {
        CreateDiagnosticRequest invalidRequest = new CreateDiagnosticRequest();
        invalidRequest.setDiagnosticId("invalid id with spaces!");
        invalidRequest.setFarmId("farm-1");
        invalidRequest.setCropId(CROP_ID);
        invalidRequest.setSymptoms("Valid symptoms description here");
        invalidRequest.setAffectedPart("LEAF");
        invalidRequest.setImages(sampleRequest.getImages());

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 400 Bad Request on InvalidImageSignatureException")
    void createDiagnostic_CorruptedBytes_Returns400() throws Exception {
        when(diagnosticService.createDiagnostic(eq(TEST_UID), any(CreateDiagnosticRequest.class)))
                .thenThrow(new InvalidImageSignatureException("Byte signature verification failed for original image: Not a valid JPEG file."));

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Byte signature verification failed for original image: Not a valid JPEG file."));
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 403 Forbidden when user is not a Farmer")
    void createDiagnostic_NonFarmerRole_Returns403() throws Exception {
        when(diagnosticService.createDiagnostic(eq(TEST_UID), any(CreateDiagnosticRequest.class)))
                .thenThrow(new AccessDeniedException("Only farmers can create diagnostic records."));

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only farmers can create diagnostic records."));
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 404 Not Found when farm does not exist")
    void createDiagnostic_FarmNotFound_Returns404() throws Exception {
        when(diagnosticService.createDiagnostic(eq(TEST_UID), any(CreateDiagnosticRequest.class)))
                .thenThrow(new NoSuchElementException("Farm with ID 'farm-1' not found."));

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Farm with ID 'farm-1' not found."));
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 409 Conflict when record already exists")
    void createDiagnostic_DuplicateRecord_Returns409() throws Exception {
        when(diagnosticService.createDiagnostic(eq(TEST_UID), any(CreateDiagnosticRequest.class)))
                .thenThrow(new DuplicateRecordException("Diagnostic record with ID '" + DIAGNOSTIC_ID + "' already exists."));

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Diagnostic record with ID '" + DIAGNOSTIC_ID + "' already exists."));
    }

    @Test
    @DisplayName("POST /api/v1/diagnostics - 503 Service Unavailable when quota exhausted")
    void createDiagnostic_QuotaExhausted_Returns503() throws Exception {
        when(diagnosticService.createDiagnostic(eq(TEST_UID), any(CreateDiagnosticRequest.class)))
                .thenThrow(new FirestoreQuotaExhaustedException("Firestore quota exceeded during diagnostic creation"));

        mockMvc.perform(post("/api/v1/diagnostics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Firestore quota exceeded during diagnostic creation"));
    }

    // =========================================================================
    // GET /api/v1/diagnostics/{diagnosticId}
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/diagnostics/{id} - 200 OK for owner")
    void getDiagnosticById_Success_Returns200() throws Exception {
        when(diagnosticService.getDiagnosticById(eq(TEST_UID), eq(DIAGNOSTIC_ID)))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/diagnostics/" + DIAGNOSTIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DIAGNOSTIC_ID))
                .andExpect(jsonPath("$.farmerUid").value(TEST_UID));
    }

    @Test
    @DisplayName("GET /api/v1/diagnostics/{id} - 401 Unauthorized when unauthenticated")
    void getDiagnosticById_Unauthenticated_Returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/diagnostics/" + DIAGNOSTIC_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/diagnostics/{id} - 403 Forbidden when caller does not own record")
    void getDiagnosticById_Forbidden_Returns403() throws Exception {
        when(diagnosticService.getDiagnosticById(eq(TEST_UID), eq(DIAGNOSTIC_ID)))
                .thenThrow(new AccessDeniedException("Access denied. You do not own this diagnostic record."));

        mockMvc.perform(get("/api/v1/diagnostics/" + DIAGNOSTIC_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied. You do not own this diagnostic record."));
    }

    @Test
    @DisplayName("GET /api/v1/diagnostics/{id} - 404 Not Found when ID does not exist")
    void getDiagnosticById_NotFound_Returns404() throws Exception {
        when(diagnosticService.getDiagnosticById(eq(TEST_UID), eq("non-existent-id")))
                .thenThrow(new NoSuchElementException("Diagnostic record not found with ID: non-existent-id"));

        mockMvc.perform(get("/api/v1/diagnostics/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Diagnostic record not found with ID: non-existent-id"));
    }

    // =========================================================================
    // GET /api/v1/diagnostics (Pagination & Listing)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/diagnostics - 200 OK with paginated results")
    void listDiagnostics_Success_Returns200() throws Exception {
        DiagnosticPageResponse pageResponse = new DiagnosticPageResponse(
                List.of(sampleResponse), 20, false, null
        );

        when(diagnosticService.listFarmerDiagnostics(eq(TEST_UID), eq("ALL"), isNull(), eq(20), isNull()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/diagnostics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(DIAGNOSTIC_ID))
                .andExpect(jsonPath("$.limit").value(20))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/diagnostics - 400 Bad Request on invalid filter")
    void listDiagnostics_InvalidFilter_Returns400() throws Exception {
        when(diagnosticService.listFarmerDiagnostics(eq(TEST_UID), eq("INVALID_STATUS"), isNull(), eq(20), isNull()))
                .thenThrow(new IllegalArgumentException("Unsupported status filter: 'INVALID_STATUS'."));

        mockMvc.perform(get("/api/v1/diagnostics").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported status filter: 'INVALID_STATUS'."));
    }

    @Test
    @DisplayName("GET /api/v1/diagnostics - 503 Service Unavailable when quota exhausted")
    void listDiagnostics_QuotaExhausted_Returns503() throws Exception {
        when(diagnosticService.listFarmerDiagnostics(anyString(), anyString(), any(), anyInt(), any()))
                .thenThrow(new FirestoreQuotaExhaustedException("Firestore quota exceeded during diagnostic history query"));

        mockMvc.perform(get("/api/v1/diagnostics"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Firestore quota exceeded during diagnostic history query"));
    }
}
