package com.farmlink.api.controller;

import com.farmlink.api.dto.MarketComparisonRequest;
import com.farmlink.api.dto.MarketEvaluationRequest;
import com.farmlink.api.dto.MarketEvaluationResponse;
import com.farmlink.api.dto.MarketProfitabilityComparisonResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.DecisionSupportService;
import com.farmlink.api.service.FarmService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/decision-support")
public class DecisionSupportController {

    private final DecisionSupportService decisionSupportService;
    private final FarmService farmService;

    public DecisionSupportController(DecisionSupportService decisionSupportService, FarmService farmService) {
        this.decisionSupportService = decisionSupportService;
        this.farmService = farmService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateMarket(@RequestBody MarketEvaluationRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            farmService.verifyFarmerRole(token.getUid());
            MarketEvaluationResponse response = decisionSupportService.evaluateMarket(request);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/compare-markets")
    public ResponseEntity<?> compareMarkets(@RequestBody MarketComparisonRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            farmService.verifyFarmerRole(token.getUid());
            MarketProfitabilityComparisonResponse response = decisionSupportService.compareMarkets(request);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof FirebaseAuthenticationToken token) {
            return token;
        }
        return null;
    }
}
