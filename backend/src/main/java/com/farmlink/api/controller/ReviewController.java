package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/marketplace")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Submit a Review for a COMPLETED order.
     */
    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(@RequestBody CreateReviewRequest request) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ReviewResponse response = reviewService.createReview(token.getUid(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit review: " + e.getMessage());
        }
    }

    /**
     * Check review eligibility for an order.
     */
    @GetMapping("/reviews/eligibility/{orderId}")
    public ResponseEntity<?> getReviewEligibility(@PathVariable String orderId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ReviewEligibilityResponse response = reviewService.getReviewEligibility(token.getUid(), orderId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to check review eligibility: " + e.getMessage());
        }
    }

    /**
     * Get reviews submitted by the authenticated user.
     */
    @GetMapping("/reviews/mine")
    public ResponseEntity<?> getMyReviews(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            ReviewPageResponse response = reviewService.getMyReviews(token.getUid(), page, size);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch my reviews: " + e.getMessage());
        }
    }

    /**
     * Get reviews associated with an order (Order participants only).
     */
    @GetMapping("/orders/{orderId}/reviews")
    public ResponseEntity<?> getOrderReviews(@PathVariable String orderId) {
        FirebaseAuthenticationToken token = getAuthenticatedToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication token is required.");
        }

        try {
            List<ReviewResponse> response = reviewService.getOrderReviews(token.getUid(), orderId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch order reviews: " + e.getMessage());
        }
    }

    /**
     * Get public reviews received by user {userId}.
     */
    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<?> getUserReviews(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        try {
            ReviewPageResponse response = reviewService.getUserReviews(userId, page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch user reviews: " + e.getMessage());
        }
    }

    /**
     * Get public Rating Summary for user {userId}.
     */
    @GetMapping("/users/{userId}/rating-summary")
    public ResponseEntity<?> getUserRatingSummary(@PathVariable String userId) {
        try {
            RatingSummaryResponse response = reviewService.getUserRatingSummary(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch rating summary: " + e.getMessage());
        }
    }

    private FirebaseAuthenticationToken getAuthenticatedToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof FirebaseAuthenticationToken token && token.isAuthenticated()) {
            return token;
        }
        return null;
    }
}
