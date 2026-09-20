package com.farmlink.api.controller;

import com.farmlink.api.dto.BuyerOpportunityPageResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.BuyerOpportunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/marketplace/buyer-opportunities")
public class BuyerOpportunityController {

    private final BuyerOpportunityService buyerOpportunityService;

    public BuyerOpportunityController(BuyerOpportunityService buyerOpportunityService) {
        this.buyerOpportunityService = buyerOpportunityService;
    }

    /**
     * Get deterministic buyer opportunities for an active listing owned by the authenticated farmer.
     */
    @GetMapping
    public ResponseEntity<?> getBuyerOpportunities(
            @RequestParam(required = false) String listingId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        if (listingId == null || listingId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Listing ID is required.");
        }

        try {
            BuyerOpportunityPageResponse response = buyerOpportunityService.getBuyerOpportunities(
                    token.getUid(),
                    listingId.trim(),
                    page,
                    size
            );
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to calculate buyer opportunities: " + e.getMessage());
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
