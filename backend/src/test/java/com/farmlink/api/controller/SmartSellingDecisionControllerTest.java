package com.farmlink.api.controller;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.SmartSellingDecisionRequest;
import com.farmlink.api.dto.SmartSellingDecisionResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
import com.farmlink.api.service.SmartSellingDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SmartSellingDecisionControllerTest {

    private SmartSellingDecisionService smartSellingDecisionService;
    private FarmService farmService;
    private SmartSellingDecisionController controller;

    @BeforeEach
    public void setUp() {
        smartSellingDecisionService = Mockito.mock(SmartSellingDecisionService.class);
        farmService = Mockito.mock(FarmService.class);
        controller = new SmartSellingDecisionController(smartSellingDecisionService, farmService);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testEvaluateSmartSelling_Unauthenticated_Returns401() {
        SmartSellingDecisionRequest req = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1"));
        ResponseEntity<?> response = controller.evaluateSmartSelling(req);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testEvaluateSmartSelling_Authenticated_ReturnsSuccess() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "farmer-uid", "farmer@example.com", "Farmer", true, List.of(new SimpleGrantedAuthority("ROLE_FARMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        SmartSellingDecisionRequest req = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1"));

        SmartSellingDecisionResponse successResp = new SmartSellingDecisionResponse();
        successResp.setStatus("SUCCESS");
        successResp.setCrop(new CropResponse("crop-1", "Paddy", "GRAIN", "Oryza sativa", "ACTIVE"));

        when(smartSellingDecisionService.evaluateSmartSelling(eq("farmer-uid"), any(SmartSellingDecisionRequest.class)))
                .thenReturn(successResp);

        ResponseEntity<?> response = controller.evaluateSmartSelling(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testEvaluateSmartSelling_Forbidden_Returns403() {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(
                "trader-uid", "trader@example.com", "Trader", true, List.of(new SimpleGrantedAuthority("ROLE_TRADER"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        Mockito.doThrow(new AccessDeniedException("Access denied. Only farmers can access decision support."))
                .when(farmService).verifyFarmerRole("trader-uid");

        SmartSellingDecisionRequest req = new SmartSellingDecisionRequest("crop-1", 10.0, "QUINTAL", List.of("market-1"));

        ResponseEntity<?> response = controller.evaluateSmartSelling(req);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
