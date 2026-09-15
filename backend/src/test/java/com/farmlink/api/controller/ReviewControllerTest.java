package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private ReviewController reviewController;

    private static final String BUYER_UID = "buyer_uid_200";

    @BeforeEach
    void setUp() {
        reviewController = new ReviewController(reviewService);
    }

    private void setSecurityContext(String uid) {
        FirebaseAuthenticationToken token = new FirebaseAuthenticationToken(uid, "buyer@test.com", "Buyer Name", true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    void createReview_Success() {
        setSecurityContext(BUYER_UID);
        CreateReviewRequest req = new CreateReviewRequest("order_1", 5, "Great", "Comment");
        ReviewResponse expected = new ReviewResponse(
                "rev_1", "order_1", "list_1", "crop_1", "Crop", BUYER_UID, "CUSTOMER", "Buyer",
                "farmer_1", "FARMER", "Farmer", 5, "Great", "Comment", "PUBLISHED", true, "2026-09-10", "2026-09-10"
        );

        when(reviewService.createReview(eq(BUYER_UID), any(CreateReviewRequest.class))).thenReturn(expected);

        ResponseEntity<?> response = reviewController.createReview(req);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void createReview_Unauthenticated_Returns401() {
        clearSecurityContext();
        CreateReviewRequest req = new CreateReviewRequest("order_1", 5, "Great", "Comment");

        ResponseEntity<?> response = reviewController.createReview(req);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getReviewEligibility_Success() {
        setSecurityContext(BUYER_UID);
        ReviewEligibilityResponse expected = new ReviewEligibilityResponse("order_1", true, "ELIGIBLE", "farmer_1", "Farmer", "FARMER", false);

        when(reviewService.getReviewEligibility(BUYER_UID, "order_1")).thenReturn(expected);

        ResponseEntity<?> response = reviewController.getReviewEligibility("order_1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getUserRatingSummary_PublicAccess() {
        clearSecurityContext(); // Public endpoint, no auth required
        RatingSummaryResponse expected = new RatingSummaryResponse(
                "user_1", "FARMER", new BigDecimal("4.80"), 10L, 48L, 0L, 0L, 1L, 1L, 8L, "2026-09-10"
        );

        when(reviewService.getUserRatingSummary("user_1")).thenReturn(expected);

        ResponseEntity<?> response = reviewController.getUserRatingSummary("user_1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }
}
