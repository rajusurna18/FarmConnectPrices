package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProfileService profileService;

    private ReviewService reviewService;

    private static final String FARMER_UID = "farmer_uid_100";
    private static final String BUYER_UID = "buyer_uid_200";
    private static final String OTHER_UID = "other_uid_300";
    private static final String ORDER_ID = "order_completed_1";
    private static final String LISTING_ID = "listing_test_1";

    private OrderResponse completedOrder;
    private OrderResponse pendingOrder;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(null, orderService, profileService, null);

        OrderItemResponse item = new OrderItemResponse(
                "item_1", ORDER_ID, LISTING_ID, "crop_paddy", "Paddy", 10.0, "QUINTAL",
                BigDecimal.valueOf(2500), "QUINTAL", BigDecimal.valueOf(25000)
        );

        completedOrder = new OrderResponse(
                ORDER_ID, "offer_1", LISTING_ID, FARMER_UID, BUYER_UID, ProfileService.ROLE_CUSTOMER,
                "crop_paddy", "Paddy", OrderStatus.COMPLETED.name(), 10.0, "QUINTAL",
                BigDecimal.valueOf(2500), "QUINTAL", BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(25000), "INR", item, null, null,
                "2026-09-10T10:00:00Z", "2026-09-10T10:00:00Z", "2026-09-10T10:05:00Z",
                "2026-09-10T10:10:00Z", "2026-09-10T10:30:00Z", null,
                false, false, false, false
        );

        pendingOrder = new OrderResponse(
                "order_pending_1", "offer_2", LISTING_ID, FARMER_UID, BUYER_UID, ProfileService.ROLE_CUSTOMER,
                "crop_paddy", "Paddy", OrderStatus.PENDING.name(), 10.0, "QUINTAL",
                BigDecimal.valueOf(2500), "QUINTAL", BigDecimal.valueOf(25000), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(25000), "INR", item, null, null,
                "2026-09-10T10:00:00Z", "2026-09-10T10:00:00Z", null, null, null, null,
                true, false, false, true
        );
    }

    @Test
    void createReview_Success_BuyerReviewsFarmer() {
        when(orderService.getOrderById(ORDER_ID, BUYER_UID)).thenReturn(completedOrder);
        when(profileService.getProfile(eq(BUYER_UID), any(), any(Boolean.class), any())).thenReturn(
                new ProfileResponse(BUYER_UID, "Buyer Ramesh", "buyer@test.com", true, "CUSTOMER", "Customer", "ACTIVE", null, null, null, null, true)
        );
        when(profileService.getProfile(eq(FARMER_UID), any(), any(Boolean.class), any())).thenReturn(
                new ProfileResponse(FARMER_UID, "Farmer Suresh", "farmer@test.com", true, "FARMER", "Farmer", "ACTIVE", null, null, null, null, true)
        );

        CreateReviewRequest request = new CreateReviewRequest(ORDER_ID, 5, "Great Quality", "The paddy was top quality and fresh.");
        ReviewResponse review = reviewService.createReview(BUYER_UID, request);

        assertNotNull(review);
        assertEquals(ORDER_ID, review.getOrderId());
        assertEquals(BUYER_UID, review.getReviewerUid());
        assertEquals("CUSTOMER", review.getReviewerRole());
        assertEquals("Buyer Ramesh", review.getReviewerDisplayName());
        assertEquals(FARMER_UID, review.getRevieweeUid());
        assertEquals("FARMER", review.getRevieweeRole());
        assertEquals("Farmer Suresh", review.getRevieweeDisplayName());
        assertEquals(5, review.getRating());
        assertEquals("Great Quality", review.getTitle());
        assertEquals("The paddy was top quality and fresh.", review.getComment());
        assertTrue(review.isVerifiedTransaction());

        // Verify Rating Summary
        RatingSummaryResponse summary = reviewService.getUserRatingSummary(FARMER_UID);
        assertNotNull(summary);
        assertEquals(1L, summary.getTotalReviews());
        assertEquals(5L, summary.getTotalRatingPoints());
        assertEquals(new BigDecimal("5.00"), summary.getAverageRating());
        assertEquals(1L, summary.getFiveStarCount());
    }

    @Test
    void createReview_Success_FarmerReviewsBuyer() {
        when(orderService.getOrderById(ORDER_ID, FARMER_UID)).thenReturn(completedOrder);

        CreateReviewRequest request = new CreateReviewRequest(ORDER_ID, 4, "Prompt Payment", "Punctual buyer, smooth transaction.");
        ReviewResponse review = reviewService.createReview(FARMER_UID, request);

        assertNotNull(review);
        assertEquals(ORDER_ID, review.getOrderId());
        assertEquals(FARMER_UID, review.getReviewerUid());
        assertEquals("FARMER", review.getReviewerRole());
        assertEquals(BUYER_UID, review.getRevieweeUid());
        assertEquals("CUSTOMER", review.getRevieweeRole());
        assertEquals(4, review.getRating());
    }

    @Test
    void createReview_RejectsNonCompletedOrder() {
        when(orderService.getOrderById("order_pending_1", BUYER_UID)).thenReturn(pendingOrder);

        CreateReviewRequest request = new CreateReviewRequest("order_pending_1", 5, "Good", "Comment");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.createReview(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("COMPLETED"));
    }

    @Test
    void createReview_RejectsDuplicateReview() {
        when(orderService.getOrderById(ORDER_ID, BUYER_UID)).thenReturn(completedOrder);

        CreateReviewRequest request = new CreateReviewRequest(ORDER_ID, 5, "Great Quality", "Fresh produce.");
        reviewService.createReview(BUYER_UID, request);

        // Second attempt by same user for same order
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.createReview(BUYER_UID, request)
        );
        assertTrue(ex.getMessage().contains("already been submitted"));
    }

    @Test
    void createReview_RejectsInvalidRatings() {
        CreateReviewRequest invalidZero = new CreateReviewRequest(ORDER_ID, 0, "Title", "Comment");
        assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(BUYER_UID, invalidZero));

        CreateReviewRequest invalidSix = new CreateReviewRequest(ORDER_ID, 6, "Title", "Comment");
        assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(BUYER_UID, invalidSix));
    }

    @Test
    void createReview_RejectsUnrelatedUser() {
        when(orderService.getOrderById(ORDER_ID, OTHER_UID)).thenThrow(new AccessDeniedException("Unauthorized"));

        CreateReviewRequest request = new CreateReviewRequest(ORDER_ID, 5, "Title", "Comment");
        assertThrows(AccessDeniedException.class, () -> reviewService.createReview(OTHER_UID, request));
    }

    @Test
    void getReviewEligibility_ReturnsCorrectStatus() {
        when(orderService.getOrderById(ORDER_ID, BUYER_UID)).thenReturn(completedOrder);

        ReviewEligibilityResponse resp = reviewService.getReviewEligibility(BUYER_UID, ORDER_ID);
        assertTrue(resp.isEligible());
        assertEquals("ELIGIBLE", resp.getReason());
        assertEquals(FARMER_UID, resp.getRevieweeUid());
        assertFalse(resp.isAlreadyReviewed());

        // Submit review
        reviewService.createReview(BUYER_UID, new CreateReviewRequest(ORDER_ID, 5, "Title", "Comment text"));

        // Check eligibility again
        ReviewEligibilityResponse respAfter = reviewService.getReviewEligibility(BUYER_UID, ORDER_ID);
        assertFalse(respAfter.isEligible());
        assertEquals("ALREADY_REVIEWED", respAfter.getReason());
        assertTrue(respAfter.isAlreadyReviewed());
    }

    @Test
    void getUserRatingSummary_CalculatesAverageCorrectly() {
        when(orderService.getOrderById(ORDER_ID, BUYER_UID)).thenReturn(completedOrder);

        // Submit 5 star review
        reviewService.createReview(BUYER_UID, new CreateReviewRequest(ORDER_ID, 5, "T1", "Comment 1"));

        RatingSummaryResponse summary = reviewService.getUserRatingSummary(FARMER_UID);
        assertEquals(new BigDecimal("5.00"), summary.getAverageRating());
        assertEquals(1L, summary.getTotalReviews());

        // Reset and test multiple reviews for a user in rating aggregation
        RatingSummaryResponse summaryEmpty = reviewService.getUserRatingSummary("non_existent_user");
        assertEquals(new BigDecimal("0.00"), summaryEmpty.getAverageRating());
        assertEquals(0L, summaryEmpty.getTotalReviews());
    }
}
