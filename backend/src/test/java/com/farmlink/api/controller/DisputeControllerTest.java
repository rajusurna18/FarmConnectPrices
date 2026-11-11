package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.DisputeEvidence;
import com.farmlink.api.model.DisputeHistoryEntry;
import com.farmlink.api.model.DisputeResponseRecord;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.DisputeService;
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
public class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DisputeService disputeService;

    private static final String CALLER_UID = "user_cust_101";
    private static final String DISPUTE_ID = "disp_123456";
    private static final String ORDER_ID = "ord_tomato_001";

    private DisputeResponse mockDisputeResponse;

    @BeforeEach
    void setUp() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                CALLER_UID, "customer@example.com", "Test Customer", true
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        mockDisputeResponse = new DisputeResponse(
                DISPUTE_ID,
                ORDER_ID,
                "listing_001",
                "offer_001",
                "crop_tomato",
                "Tomato",
                CALLER_UID,
                "CUSTOMER",
                "farmer_001",
                CALLER_UID,
                "CUSTOMER",
                "DAMAGED_PRODUCT",
                "Crushed crates on arrival",
                "Two boxes arrived completely crushed.",
                "OPEN",
                null,
                null,
                null,
                null,
                null,
                "2026-10-10T10:00:00Z",
                "2026-10-10T10:00:00Z",
                "2026-10-10T10:00:00Z"
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateDispute_Success_Returns201() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest(
                ORDER_ID, "DAMAGED_PRODUCT", "Crushed crates on arrival", "Two boxes arrived completely crushed."
        );

        when(disputeService.createDispute(eq(CALLER_UID), any(CreateDisputeRequest.class)))
                .thenReturn(mockDisputeResponse);

        mockMvc.perform(post("/api/v1/marketplace/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.issueCategory").value("DAMAGED_PRODUCT"));
    }

    @Test
    void testCreateDispute_Unauthenticated_Returns401() throws Exception {
        SecurityContextHolder.clearContext();

        CreateDisputeRequest request = new CreateDisputeRequest(ORDER_ID, "DAMAGED_PRODUCT", "Subject", "Desc");

        mockMvc.perform(post("/api/v1/marketplace/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateDispute_Forbidden_Returns403() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest(ORDER_ID, "DAMAGED_PRODUCT", "Subject", "Desc");

        when(disputeService.createDispute(eq(CALLER_UID), any(CreateDisputeRequest.class)))
                .thenThrow(new AccessDeniedException("User is not a participant in order: " + ORDER_ID));

        mockMvc.perform(post("/api/v1/marketplace/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User is not a participant in order: " + ORDER_ID));
    }

    @Test
    void testCreateDispute_BadRequest_Returns400() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest(ORDER_ID, "INVALID_CAT", "Subject", "Desc");

        when(disputeService.createDispute(eq(CALLER_UID), any(CreateDisputeRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid dispute category: INVALID_CAT"));

        mockMvc.perform(post("/api/v1/marketplace/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid dispute category: INVALID_CAT"));
    }

    @Test
    void testCreateDispute_Conflict_Returns409() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest(ORDER_ID, "DAMAGED_PRODUCT", "Subject", "Desc");

        when(disputeService.createDispute(eq(CALLER_UID), any(CreateDisputeRequest.class)))
                .thenThrow(new IllegalStateException("An active dispute already exists for this order and category."));

        mockMvc.perform(post("/api/v1/marketplace/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("An active dispute already exists for this order and category."));
    }

    @Test
    void testGetMyDisputes_Success_Returns200() throws Exception {
        DisputePageResponse pageResponse = new DisputePageResponse(
                List.of(mockDisputeResponse), 0, 20, 1, 1, false
        );

        when(disputeService.getMyDisputes(eq(CALLER_UID), eq("OPEN"), eq(0), eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/disputes/mine?status=OPEN&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetMyDisputes_WithCursor_Success_Returns200() throws Exception {
        DisputePageResponse pageResponse = new DisputePageResponse(
                List.of(mockDisputeResponse), 0, 20, 1, 1, true, "sample_next_cursor"
        );

        when(disputeService.getMyDisputes(eq(CALLER_UID), eq("OPEN"), eq(0), eq(20), eq("sample_prev_cursor")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/disputes/mine?status=OPEN&page=0&size=20&cursor=sample_prev_cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value("sample_next_cursor"));
    }

    @Test
    void testGetRelatedDisputes_Success_Returns200() throws Exception {
        DisputePageResponse pageResponse = new DisputePageResponse(
                List.of(mockDisputeResponse), 0, 20, 1, 1, false
        );

        when(disputeService.getRelatedDisputes(eq(CALLER_UID), eq("ALL"), eq(0), eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/disputes/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetRelatedDisputes_WithCursor_Success_Returns200() throws Exception {
        DisputePageResponse pageResponse = new DisputePageResponse(
                List.of(mockDisputeResponse), 0, 20, 1, 1, true, "sample_related_cursor"
        );

        when(disputeService.getRelatedDisputes(eq(CALLER_UID), eq("ALL"), eq(0), eq(20), eq("sample_prev_rel_cursor")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/marketplace/disputes/related?cursor=sample_prev_rel_cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value("sample_related_cursor"));
    }

    @Test
    void testGetDisputesByOrderId_Success_Returns200() throws Exception {
        when(disputeService.getDisputesByOrderId(eq(CALLER_UID), eq(ORDER_ID)))
                .thenReturn(List.of(mockDisputeResponse));

        mockMvc.perform(get("/api/v1/marketplace/disputes/order/" + ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$[0].orderId").value(ORDER_ID));
    }

    @Test
    void testGetDisputesByOrderId_Forbidden_Returns403() throws Exception {
        when(disputeService.getDisputesByOrderId(eq(CALLER_UID), eq(ORDER_ID)))
                .thenThrow(new AccessDeniedException("User is not authorized to view disputes for this order"));

        mockMvc.perform(get("/api/v1/marketplace/disputes/order/" + ORDER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetDisputesByOrderId_NotFound_Returns404() throws Exception {
        when(disputeService.getDisputesByOrderId(eq(CALLER_UID), eq(ORDER_ID)))
                .thenThrow(new NoSuchElementException("Order not found: " + ORDER_ID));

        mockMvc.perform(get("/api/v1/marketplace/disputes/order/" + ORDER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDisputeById_Success_Returns200() throws Exception {
        DisputeResponseRecord record = new DisputeResponseRecord(
                "resp_1", DISPUTE_ID, "farmer_001", "FARMER", "We will replace it.", "2026-10-10T11:00:00Z"
        );
        DisputeEvidence evidence = new DisputeEvidence(
                "evi_1", DISPUTE_ID, CALLER_UID, "CUSTOMER", "photo.jpg", "disputes/1/photo.jpg", "image/jpeg", 2048L, "Photo", "2026-10-10T10:05:00Z"
        );
        DisputeHistoryEntry history = new DisputeHistoryEntry(
                "hist_1", DISPUTE_ID, CALLER_UID, "CUSTOMER", "DISPUTE_CREATED", null, "OPEN", "Dispute opened", "2026-10-10T10:00:00Z"
        );

        DisputeDetailResponse detailResponse = new DisputeDetailResponse(
                mockDisputeResponse, List.of(record), List.of(evidence), List.of(history)
        );

        when(disputeService.getDisputeById(eq(CALLER_UID), eq(DISPUTE_ID)))
                .thenReturn(detailResponse);

        mockMvc.perform(get("/api/v1/marketplace/disputes/" + DISPUTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispute.disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.responses[0].responseId").value("resp_1"))
                .andExpect(jsonPath("$.evidence[0].evidenceId").value("evi_1"))
                .andExpect(jsonPath("$.history[0].actionType").value("DISPUTE_CREATED"));
    }

    @Test
    void testGetDisputeById_NotFound_Returns404() throws Exception {
        when(disputeService.getDisputeById(eq(CALLER_UID), eq(DISPUTE_ID)))
                .thenThrow(new NoSuchElementException("Dispute not found: " + DISPUTE_ID));

        mockMvc.perform(get("/api/v1/marketplace/disputes/" + DISPUTE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDisputeById_Forbidden_Returns403() throws Exception {
        when(disputeService.getDisputeById(eq(CALLER_UID), eq(DISPUTE_ID)))
                .thenThrow(new AccessDeniedException("User is not authorized to view this dispute"));

        mockMvc.perform(get("/api/v1/marketplace/disputes/" + DISPUTE_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddResponse_Success_Returns201() throws Exception {
        AddDisputeResponseRequest request = new AddDisputeResponseRequest("Acknowledged, we will inspect.");
        DisputeResponseRecord record = new DisputeResponseRecord(
                "resp_1", DISPUTE_ID, CALLER_UID, "CUSTOMER", "Acknowledged, we will inspect.", "2026-10-10T11:00:00Z"
        );

        when(disputeService.addResponse(eq(CALLER_UID), eq(DISPUTE_ID), any(AddDisputeResponseRequest.class)))
                .thenReturn(record);

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseId").value("resp_1"))
                .andExpect(jsonPath("$.message").value("Acknowledged, we will inspect."));
    }

    @Test
    void testAddResponse_Conflict_WhenClosed_Returns409() throws Exception {
        AddDisputeResponseRequest request = new AddDisputeResponseRequest("Late message");

        when(disputeService.addResponse(eq(CALLER_UID), eq(DISPUTE_ID), any(AddDisputeResponseRequest.class)))
                .thenThrow(new IllegalStateException("Cannot add response to a closed dispute"));

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testAddEvidence_Success_Returns201() throws Exception {
        AddDisputeEvidenceRequest request = new AddDisputeEvidenceRequest(
                "photo.jpg", "image/jpeg", 2048L, "Damage photo"
        );
        DisputeEvidence evidence = new DisputeEvidence(
                "evi_1", DISPUTE_ID, CALLER_UID, "CUSTOMER", "photo.jpg", null, "image/jpeg", 2048L, "Damage photo", "2026-10-10T10:05:00Z"
        );

        when(disputeService.addEvidence(eq(CALLER_UID), eq(DISPUTE_ID), any(AddDisputeEvidenceRequest.class)))
                .thenReturn(evidence);

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceId").value("evi_1"))
                .andExpect(jsonPath("$.fileName").value("photo.jpg"));
    }

    @Test
    void testAddEvidence_BadRequest_Returns400() throws Exception {
        AddDisputeEvidenceRequest request = new AddDisputeEvidenceRequest(
                "bad.exe", "application/x-msdownload", 2048L, "Executable"
        );

        when(disputeService.addEvidence(eq(CALLER_UID), eq(DISPUTE_ID), any(AddDisputeEvidenceRequest.class)))
                .thenThrow(new IllegalArgumentException("Unsupported file type: application/x-msdownload"));

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Unsupported file type: application/x-msdownload"));
    }

    @Test
    void testCloseDispute_Success_Returns200() throws Exception {
        mockDisputeResponse.setStatus("CLOSED");
        mockDisputeResponse.setClosedAt("2026-10-10T12:00:00Z");

        when(disputeService.closeDispute(eq(CALLER_UID), eq(DISPUTE_ID)))
                .thenReturn(mockDisputeResponse);

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disputeId").value(DISPUTE_ID))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void testCloseDispute_Forbidden_WhenNotCreator_Returns403() throws Exception {
        when(disputeService.closeDispute(eq(CALLER_UID), eq(DISPUTE_ID)))
                .thenThrow(new AccessDeniedException("Only the dispute creator can close this dispute"));

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/close"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Only the dispute creator can close this dispute"));
    }

    @Test
    void testCloseDispute_Conflict_WhenAlreadyClosed_Returns409() throws Exception {
        when(disputeService.closeDispute(eq(CALLER_UID), eq(DISPUTE_ID)))
                .thenThrow(new IllegalStateException("Dispute is already closed"));

        mockMvc.perform(post("/api/v1/marketplace/disputes/" + DISPUTE_ID + "/close"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Dispute is already closed"));
    }
}
