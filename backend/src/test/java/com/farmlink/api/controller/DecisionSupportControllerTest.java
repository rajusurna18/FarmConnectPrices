package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.DecisionSupportService;
import com.farmlink.api.service.FarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionSupportControllerTest {

    private DecisionSupportService decisionSupportService;
    private FarmService farmService;
    private DecisionSupportController controller;

    @BeforeEach
    void setUp() {
        decisionSupportService = Mockito.mock(DecisionSupportService.class);
        farmService = Mockito.mock(FarmService.class);
        controller = new DecisionSupportController(decisionSupportService, farmService);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccess() {
        MarketEvaluationRequest req = new MarketEvaluationRequest();
        ResponseEntity<?> response = controller.evaluateMarket(req);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Non-farmer user (e.g. MEDIATOR_BUYER or CUSTOMER) returns 403 Forbidden")
    void testNonFarmerAccessDenied() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("uid123", "buyer@example.com", "Buyer User");
        SecurityContextHolder.getContext().setAuthentication(token);

        doThrow(new AccessDeniedException("Farm management access is strictly restricted to Farmers."))
                .when(farmService).verifyFarmerRole("uid123");

        MarketEvaluationRequest req = new MarketEvaluationRequest();
        ResponseEntity<?> response = controller.evaluateMarket(req);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Authenticated FARMER user evaluation returns 200 OK")
    void testFarmerAccessGranted() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("farmer123", "farmer@example.com", "Farmer User");
        SecurityContextHolder.getContext().setAuthentication(token);

        doNothing().when(farmService).verifyFarmerRole("farmer123");

        MarketEvaluationResponse mockRes = new MarketEvaluationResponse();
        mockRes.setStatus("SUCCESS");
        mockRes.setGrossRevenue(new BigDecimal("28000.00"));
        when(decisionSupportService.evaluateMarket(any())).thenReturn(mockRes);

        MarketEvaluationRequest req = new MarketEvaluationRequest("CROP1", "MKT1", 10.0, "QUINTAL", "MODAL", "LATEST_AVAILABLE", null, 300.0, 100.0);
        ResponseEntity<?> response = controller.evaluateMarket(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockRes, response.getBody());
    }

    @Test
    @DisplayName("Authenticated FARMER user comparison returns 200 OK")
    void testFarmerComparisonGranted() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken("farmer123", "farmer@example.com", "Farmer User");
        SecurityContextHolder.getContext().setAuthentication(token);

        doNothing().when(farmService).verifyFarmerRole("farmer123");

        MarketProfitabilityComparisonResponse mockRes = new MarketProfitabilityComparisonResponse();
        when(decisionSupportService.compareMarkets(any())).thenReturn(mockRes);

        MarketComparisonRequest req = new MarketComparisonRequest();
        ResponseEntity<?> response = controller.compareMarkets(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockRes, response.getBody());
    }
}
