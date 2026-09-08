package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.ProductListingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/marketplace/listings")
public class ProductListingController {

    private final ProductListingService listingService;

    public ProductListingController(ProductListingService listingService) {
        this.listingService = listingService;
    }

    /**
     * Create a new marketplace product listing.
     * Restricted to authenticated FARMER users.
     */
    @PostMapping
    public ResponseEntity<?> createListing(@RequestBody CreateListingRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ProductListingResponse response = listingService.createListing(token.getUid(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create listing: " + e.getMessage());
        }
    }

    /**
     * Public browse active marketplace listings with search, filtering, sorting, and pagination.
     */
    @GetMapping
    public ResponseEntity<?> browsePublicListings(
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ListingPageResponse pageResponse = listingService.browsePublicListings(
                    cropId, state, district, minPrice, maxPrice, sortBy, page, size
            );
            return ResponseEntity.ok(pageResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch marketplace listings: " + e.getMessage());
        }
    }

    /**
     * Get authenticated farmer's own listings.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> getMyListings(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ListingPageResponse pageResponse = listingService.getFarmerListings(token.getUid(), status, page, size);
            return ResponseEntity.ok(pageResponse);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch farmer listings: " + e.getMessage());
        }
    }

    /**
     * Get detailed view of a specific listing.
     */
    @GetMapping("/{listingId}")
    public ResponseEntity<?> getListingById(@PathVariable String listingId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        String callerUid = token != null ? token.getUid() : null;

        try {
            ProductListingResponse response = listingService.getListingById(listingId, callerUid);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch listing: " + e.getMessage());
        }
    }

    /**
     * Update an existing listing. Ownership enforced server-side.
     */
    @PutMapping("/{listingId}")
    public ResponseEntity<?> updateListing(
            @PathVariable String listingId,
            @RequestBody UpdateListingRequest request
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ProductListingResponse response = listingService.updateListing(token.getUid(), listingId, request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update listing: " + e.getMessage());
        }
    }

    /**
     * Update status lifecycle state of an owned listing.
     */
    @PatchMapping("/{listingId}/status")
    public ResponseEntity<?> updateListingStatus(
            @PathVariable String listingId,
            @RequestBody UpdateListingStatusRequest request
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ProductListingResponse response = listingService.updateListingStatus(token.getUid(), listingId, request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update listing status: " + e.getMessage());
        }
    }

    /**
     * Delete an owned draft/unpublished listing.
     */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<?> deleteListing(@PathVariable String listingId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            listingService.deleteListing(token.getUid(), listingId);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete listing: " + e.getMessage());
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
