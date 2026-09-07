package com.farmlink.api.controller;

import com.farmlink.api.dto.SmartSellingDecisionRequest;
import com.farmlink.api.dto.SmartSellingDecisionResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
import com.farmlink.api.service.SmartSellingDecisionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/smart-selling")
public class SmartSellingDecisionController {

    private final SmartSellingDecisionService smartSellingDecisionService;
    private final FarmService farmService;

    public SmartSellingDecisionController(SmartSellingDecisionService smartSellingDecisionService, FarmService farmService) {
        this.smartSellingDecisionService = smartSellingDecisionService;
        this.farmService = farmService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateSmartSelling(@RequestBody SmartSellingDecisionRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized authentication token missing or invalid.");
        }

        try {
            farmService.verifyFarmerRole(token.getUid());
            SmartSellingDecisionResponse response = smartSellingDecisionService.evaluateSmartSelling(token.getUid(), request);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error executing Smart Selling evaluation: " + e.getMessage());
        }
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof FirebaseAuthenticationToken && auth.isAuthenticated()) {
            return (FirebaseAuthenticationToken) auth;
        }
        return null;
    }
}
