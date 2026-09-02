package com.farmlink.api.controller;

import com.farmlink.api.dto.ai.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
import com.farmlink.api.service.ai.AiDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiDecisionControllerTest {

    @Mock
    private AiDecisionService aiDecisionService;

    @Mock
    private FarmService farmService;

    private AiDecisionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AiDecisionController(aiDecisionService, farmService);
    }

    @Test
    void testProcessDecision_Unauthenticated_Returns401() {
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        ResponseEntity<AiDecisionResponse> response = controller.processDecision(null, request);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(aiDecisionService);
    }

    @Test
    void testProcessDecision_NonFarmerRole_Throws403() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("buyer-uid-1", "buyer@example.com", "Buyer User");
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        doThrow(new AccessDeniedException("Access denied: Only FARMER profile users are authorized to perform this operation."))
                .when(farmService).verifyFarmerRole("buyer-uid-1");

        assertThrows(AccessDeniedException.class, () -> controller.processDecision(token, request));
        verifyNoInteractions(aiDecisionService);
    }

    @Test
    void testProcessDecision_FarmerRole_Returns200WithResponse() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("farmer-uid-1", "farmer@example.com", "Farmer User");
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.MARKET_SELECTION);

        AiDecisionResponse expected = new AiDecisionResponse();
        expected.setDecisionType(AiDecisionType.MARKET_SELECTION);
        expected.setSummary("Market A recommended");
        expected.setConfidence(AiConfidenceLevel.HIGH);

        when(aiDecisionService.processDecisionRequest(eq("farmer-uid-1"), any())).thenReturn(expected);

        ResponseEntity<AiDecisionResponse> response = controller.processDecision(token, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Market A recommended", response.getBody().getSummary());
        verify(farmService).verifyFarmerRole("farmer-uid-1");
    }

    @Test
    void testProcessDecision_CrossFarmerAccess_Throws403() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("farmer-A", "farmerA@example.com", "Farmer A");
        AiDecisionRequest request = new AiDecisionRequest();
        request.setDecisionType(AiDecisionType.PROFITABILITY_EXPLANATION);
        request.setEconomicRecordId("record-owned-by-farmer-B");

        when(aiDecisionService.processDecisionRequest(eq("farmer-A"), any()))
                .thenThrow(new AccessDeniedException("Access denied: You do not own this economic record."));

        assertThrows(AccessDeniedException.class, () -> controller.processDecision(token, request));
    }
}
