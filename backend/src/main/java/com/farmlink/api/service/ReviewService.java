package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.model.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    public static final String COLLECTION_NAME = "reviews";
    public static final String RATING_SUMMARIES_COLLECTION = "ratingSummaries";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_COMMENT_LENGTH = 1000;

    private final Firestore firestore;
    private final OrderService orderService;
    private final ProfileService profileService;
    private final FirestoreQuotaGuard quotaGuard;

    // In-memory fallback ONLY for isolated unit tests (when firestore bean is null)
    private final Map<String, Review> inMemoryReviews = new ConcurrentHashMap<>();
    private final Map<String, RatingSummary> inMemoryRatingSummaries = new ConcurrentHashMap<>();

    @Autowired
    public ReviewService(
            @Autowired(required = false) Firestore firestore,
            OrderService orderService,
            ProfileService profileService,
            @Autowired(required = false) FirestoreQuotaGuard quotaGuard
    ) {
        this.firestore = firestore;
        this.orderService = orderService;
        this.profileService = profileService;
        this.quotaGuard = quotaGuard;
    }

    /**
     * Submit a Review for a COMPLETED Order with atomic rating summary aggregation.
     */
    public synchronized ReviewResponse createReview(String callerUid, CreateReviewRequest request) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (request == null || request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be an integer between 1 and 5 inclusive.");
        }

        String title = request.getTitle() != null ? sanitizeText(request.getTitle()) : "";
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Review title cannot exceed " + MAX_TITLE_LENGTH + " characters.");
        }

        String comment = request.getComment() != null ? sanitizeText(request.getComment()) : "";
        if (comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Review comment is required.");
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Review comment cannot exceed " + MAX_COMMENT_LENGTH + " characters.");
        }

        String orderId = request.getOrderId().trim();
        String reviewId = "review_" + orderId + "_" + callerUid.trim();

        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();

                DocumentReference orderRef = firestore.collection(OrderService.COLLECTION_NAME).document(orderId);
                DocumentReference reviewRef = firestore.collection(COLLECTION_NAME).document(reviewId);

                Review createdReview = firestore.runTransaction(transaction -> {
                    // 1. Fetch Order snapshot inside transaction
                    DocumentSnapshot orderSnap = transaction.get(orderRef).get();
                    if (!orderSnap.exists()) {
                        throw new NoSuchElementException("Order not found with ID: " + orderId);
                    }

                    String buyerUid = orderSnap.getString("buyerUid");
                    String farmerId = orderSnap.getString("farmerId");
                    String buyerRole = orderSnap.getString("buyerRole");
                    String orderStatus = orderSnap.getString("status");
                    String listingId = orderSnap.getString("listingId");
                    String cropId = orderSnap.getString("cropId");
                    String cropName = orderSnap.getString("cropName");

                    // 2. Verify participant status
                    if (!callerUid.equals(buyerUid) && !callerUid.equals(farmerId)) {
                        throw new AccessDeniedException("Unauthorized to review this order. Only order participants may submit reviews.");
                    }

                    // 3. Verify qualifying transaction status (COMPLETED only)
                    if (!OrderStatus.COMPLETED.name().equalsIgnoreCase(orderStatus)) {
                        throw new IllegalStateException("Reviews are only permitted for COMPLETED orders. Current order status: " + orderStatus);
                    }

                    // 4. Verify duplicate review inside transaction
                    DocumentSnapshot existingReviewSnap = transaction.get(reviewRef).get();
                    if (existingReviewSnap.exists()) {
                        throw new IllegalStateException("A review has already been submitted for this order by this user.");
                    }

                    // 5. Determine reviewer vs reviewee roles and UIDs
                    String reviewerRole;
                    String revieweeUid;
                    String revieweeRole;

                    if (callerUid.equals(buyerUid)) {
                        reviewerRole = buyerRole != null ? buyerRole : ProfileService.ROLE_CUSTOMER;
                        revieweeUid = farmerId;
                        revieweeRole = ProfileService.ROLE_FARMER;
                    } else {
                        reviewerRole = ProfileService.ROLE_FARMER;
                        revieweeUid = buyerUid;
                        revieweeRole = buyerRole != null ? buyerRole : ProfileService.ROLE_CUSTOMER;
                    }

                    if (callerUid.equals(revieweeUid)) {
                        throw new IllegalStateException("Self-review is forbidden.");
                    }

                    // Fetch display names safely
                    String reviewerDisplayName = fetchDisplayName(callerUid);
                    String revieweeDisplayName = fetchDisplayName(revieweeUid);

                    String nowIso = Instant.now().toString();
                    Review review = new Review(
                            reviewId,
                            orderId,
                            listingId,
                            cropId,
                            cropName,
                            callerUid,
                            reviewerRole,
                            reviewerDisplayName,
                            revieweeUid,
                            revieweeRole,
                            revieweeDisplayName,
                            rating,
                            title,
                            comment,
                            STATUS_PUBLISHED,
                            nowIso,
                            nowIso
                    );

                    // 6. Transactionally update ratingSummaries/{revieweeUid}
                    DocumentReference summaryRef = firestore.collection(RATING_SUMMARIES_COLLECTION).document(revieweeUid);
                    DocumentSnapshot summarySnap = transaction.get(summaryRef).get();

                    RatingSummary summary;
                    if (summarySnap.exists()) {
                        summary = mapDocToSummary(summarySnap);
                    } else {
                        summary = new RatingSummary(
                                revieweeUid,
                                revieweeRole,
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                0L, 0L, 0L, 0L, 0L, 0L, 0L, nowIso
                        );
                    }

                    long newTotalReviews = summary.getTotalReviews() + 1;
                    long newTotalPoints = summary.getTotalRatingPoints() + rating;
                    BigDecimal newAvg = BigDecimal.valueOf(newTotalPoints)
                            .divide(BigDecimal.valueOf(newTotalReviews), 2, RoundingMode.HALF_UP);

                    summary.setTotalReviews(newTotalReviews);
                    summary.setTotalRatingPoints(newTotalPoints);
                    summary.setAverageRating(newAvg);
                    summary.setUpdatedAt(nowIso);

                    switch (rating) {
                        case 1 -> summary.setOneStarCount(summary.getOneStarCount() + 1);
                        case 2 -> summary.setTwoStarCount(summary.getTwoStarCount() + 1);
                        case 3 -> summary.setThreeStarCount(summary.getThreeStarCount() + 1);
                        case 4 -> summary.setFourStarCount(summary.getFourStarCount() + 1);
                        case 5 -> summary.setFiveStarCount(summary.getFiveStarCount() + 1);
                    }

                    // Save transactional documents
                    transaction.set(reviewRef, mapReviewToDoc(review));
                    transaction.set(summaryRef, mapSummaryToDoc(summary), SetOptions.merge());

                    return review;
                }).get();

                if (quotaGuard != null) quotaGuard.recordSuccess();
                return toResponse(createdReview);

            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException("Failed to submit review transaction: " + e.getMessage(), e);
            }
        }

        // In-memory execution ONLY for unit tests (when firestore is null)
        OrderResponse order = orderService.getOrderById(orderId, callerUid);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        if (!callerUid.equals(order.getBuyerUid()) && !callerUid.equals(order.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized to review this order. Only order participants may submit reviews.");
        }

        if (!OrderStatus.COMPLETED.name().equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Reviews are only permitted for COMPLETED orders. Current order status: " + order.getStatus());
        }

        if (inMemoryReviews.containsKey(reviewId)) {
            throw new IllegalStateException("A review has already been submitted for this order by this user.");
        }

        String reviewerRole;
        String revieweeUid;
        String revieweeRole;

        if (callerUid.equals(order.getBuyerUid())) {
            reviewerRole = order.getBuyerRole() != null ? order.getBuyerRole() : ProfileService.ROLE_CUSTOMER;
            revieweeUid = order.getFarmerId();
            revieweeRole = ProfileService.ROLE_FARMER;
        } else {
            reviewerRole = ProfileService.ROLE_FARMER;
            revieweeUid = order.getBuyerUid();
            revieweeRole = order.getBuyerRole() != null ? order.getBuyerRole() : ProfileService.ROLE_CUSTOMER;
        }

        if (callerUid.equals(revieweeUid)) {
            throw new IllegalStateException("Self-review is forbidden.");
        }

        String reviewerDisplayName = fetchDisplayName(callerUid);
        String revieweeDisplayName = fetchDisplayName(revieweeUid);

        String nowIso = Instant.now().toString();
        Review review = new Review(
                reviewId,
                orderId,
                order.getListingId(),
                order.getCropId(),
                order.getCropName(),
                callerUid,
                reviewerRole,
                reviewerDisplayName,
                revieweeUid,
                revieweeRole,
                revieweeDisplayName,
                rating,
                title,
                comment,
                STATUS_PUBLISHED,
                nowIso,
                nowIso
        );

        RatingSummary summary = inMemoryRatingSummaries.getOrDefault(revieweeUid, new RatingSummary(
                revieweeUid, revieweeRole, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                0L, 0L, 0L, 0L, 0L, 0L, 0L, nowIso
        ));

        long newTotalReviews = summary.getTotalReviews() + 1;
        long newTotalPoints = summary.getTotalRatingPoints() + rating;
        BigDecimal newAvg = BigDecimal.valueOf(newTotalPoints)
                .divide(BigDecimal.valueOf(newTotalReviews), 2, RoundingMode.HALF_UP);

        summary.setTotalReviews(newTotalReviews);
        summary.setTotalRatingPoints(newTotalPoints);
        summary.setAverageRating(newAvg);
        summary.setUpdatedAt(nowIso);

        switch (rating) {
            case 1 -> summary.setOneStarCount(summary.getOneStarCount() + 1);
            case 2 -> summary.setTwoStarCount(summary.getTwoStarCount() + 1);
            case 3 -> summary.setThreeStarCount(summary.getThreeStarCount() + 1);
            case 4 -> summary.setFourStarCount(summary.getFourStarCount() + 1);
            case 5 -> summary.setFiveStarCount(summary.getFiveStarCount() + 1);
        }

        inMemoryReviews.put(reviewId, review);
        inMemoryRatingSummaries.put(revieweeUid, summary);
        return toResponse(review);
    }

    /**
     * Check review eligibility for an Order.
     */
    public ReviewEligibilityResponse getReviewEligibility(String callerUid, String orderId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        OrderResponse order;
        try {
            order = orderService.getOrderById(orderId, callerUid);
        } catch (NoSuchElementException e) {
            return new ReviewEligibilityResponse(orderId, false, "ORDER_NOT_FOUND", null, null, null, false);
        } catch (AccessDeniedException e) {
            return new ReviewEligibilityResponse(orderId, false, "NOT_ORDER_PARTICIPANT", null, null, null, false);
        }

        boolean isBuyer = callerUid.equals(order.getBuyerUid());
        boolean isFarmer = callerUid.equals(order.getFarmerId());

        if (!isBuyer && !isFarmer) {
            return new ReviewEligibilityResponse(orderId, false, "NOT_ORDER_PARTICIPANT", null, null, null, false);
        }

        String revieweeUid = isBuyer ? order.getFarmerId() : order.getBuyerUid();
        String revieweeRole = isBuyer ? ProfileService.ROLE_FARMER : order.getBuyerRole();
        String revieweeDisplayName = fetchDisplayName(revieweeUid);

        if (callerUid.equals(revieweeUid)) {
            return new ReviewEligibilityResponse(orderId, false, "SELF_REVIEW_FORBIDDEN", revieweeUid, revieweeDisplayName, revieweeRole, false);
        }

        if (!OrderStatus.COMPLETED.name().equalsIgnoreCase(order.getStatus())) {
            String reason = OrderStatus.CANCELLED.name().equalsIgnoreCase(order.getStatus()) ? "ORDER_CANCELLED" : "ORDER_NOT_COMPLETED";
            return new ReviewEligibilityResponse(orderId, false, reason, revieweeUid, revieweeDisplayName, revieweeRole, false);
        }

        String reviewId = "review_" + orderId + "_" + callerUid.trim();
        boolean alreadyReviewed = checkReviewExists(reviewId);

        if (alreadyReviewed) {
            return new ReviewEligibilityResponse(orderId, false, "ALREADY_REVIEWED", revieweeUid, revieweeDisplayName, revieweeRole, true);
        }

        return new ReviewEligibilityResponse(orderId, true, "ELIGIBLE", revieweeUid, revieweeDisplayName, revieweeRole, false);
    }

    /**
     * Get reviews submitted by caller.
     */
    public ReviewPageResponse getMyReviews(String callerUid, int page, int size) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Review> reviews = fetchMyReviewsFromFirestoreOrMemory(callerUid);
        reviews.sort(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = reviews.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<ReviewResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = reviews.subList(fromIndex, toIndex).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new ReviewPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Get public reviews received by a user.
     */
    public ReviewPageResponse getUserReviews(String userId, int page, int size) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required.");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        List<Review> reviews = fetchUserReviewsFromFirestoreOrMemory(userId);
        reviews = reviews.stream()
                .filter(r -> STATUS_PUBLISHED.equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());

        reviews.sort(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int totalElements = reviews.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<ReviewResponse> pageItems;
        if (fromIndex >= totalElements) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageItems = reviews.subList(fromIndex, toIndex).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        boolean hasNext = (safePage + 1) < totalPages;
        return new ReviewPageResponse(pageItems, safePage, safeSize, totalElements, totalPages, hasNext);
    }

    /**
     * Get reviews associated with an order (for order participants).
     */
    public List<ReviewResponse> getOrderReviews(String callerUid, String orderId) {
        if (callerUid == null || callerUid.trim().isEmpty()) {
            throw new AccessDeniedException("User must be authenticated.");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        OrderResponse order = orderService.getOrderById(orderId, callerUid);
        if (order == null) {
            throw new NoSuchElementException("Order not found with ID: " + orderId);
        }

        if (!callerUid.equals(order.getBuyerUid()) && !callerUid.equals(order.getFarmerId())) {
            throw new AccessDeniedException("Unauthorized to view reviews for this order.");
        }

        List<Review> reviews = fetchOrderReviewsFromFirestoreOrMemory(orderId);
        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get public Rating Summary for a user.
     */
    public RatingSummaryResponse getUserRatingSummary(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required.");
        }

        RatingSummary summary = fetchSummaryFromFirestoreOrMemory(userId);
        if (summary == null) {
            return new RatingSummaryResponse(
                    userId,
                    null,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0L, 0L, 0L, 0L, 0L, 0L, 0L,
                    Instant.now().toString()
            );
        }

        return new RatingSummaryResponse(
                summary.getUserId(),
                summary.getUserRole(),
                summary.getAverageRating(),
                summary.getTotalReviews(),
                summary.getTotalRatingPoints(),
                summary.getOneStarCount(),
                summary.getTwoStarCount(),
                summary.getThreeStarCount(),
                summary.getFourStarCount(),
                summary.getFiveStarCount(),
                summary.getUpdatedAt()
        );
    }

    // Helper methods
    private boolean checkReviewExists(String reviewId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot snap = firestore.collection(COLLECTION_NAME).document(reviewId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                return snap.exists();
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                logger.warn("Error checking review existence for {}: {}", reviewId, e.getMessage());
                throw new RuntimeException("Firestore query error checking review existence: " + e.getMessage(), e);
            }
        }
        return inMemoryReviews.containsKey(reviewId);
    }

    private List<Review> fetchMyReviewsFromFirestoreOrMemory(String callerUid) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("reviewerUid", callerUid)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                List<Review> results = new ArrayList<>();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    results.add(mapDocToReview(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                throw new RuntimeException("Failed to query user submitted reviews from Firestore: " + e.getMessage(), e);
            }
        }
        return inMemoryReviews.values().stream()
                .filter(r -> callerUid.equals(r.getReviewerUid()))
                .collect(Collectors.toList());
    }

    private List<Review> fetchUserReviewsFromFirestoreOrMemory(String userId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("revieweeUid", userId)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                List<Review> results = new ArrayList<>();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    results.add(mapDocToReview(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                throw new RuntimeException("Failed to query user received reviews from Firestore: " + e.getMessage(), e);
            }
        }
        return inMemoryReviews.values().stream()
                .filter(r -> userId.equals(r.getRevieweeUid()))
                .collect(Collectors.toList());
    }

    private List<Review> fetchOrderReviewsFromFirestoreOrMemory(String orderId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                QuerySnapshot query = firestore.collection(COLLECTION_NAME)
                        .whereEqualTo("orderId", orderId)
                        .get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                List<Review> results = new ArrayList<>();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    results.add(mapDocToReview(doc));
                }
                return results;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                throw new RuntimeException("Failed to query order reviews from Firestore: " + e.getMessage(), e);
            }
        }
        return inMemoryReviews.values().stream()
                .filter(r -> orderId.equals(r.getOrderId()))
                .collect(Collectors.toList());
    }

    private RatingSummary fetchSummaryFromFirestoreOrMemory(String userId) {
        if (firestore != null) {
            try {
                if (quotaGuard != null) quotaGuard.checkQuotaAvailability();
                DocumentSnapshot doc = firestore.collection(RATING_SUMMARIES_COLLECTION).document(userId).get().get();
                if (quotaGuard != null) quotaGuard.recordSuccess();
                if (doc.exists()) {
                    return mapDocToSummary(doc);
                }
                return null;
            } catch (Exception e) {
                if (quotaGuard != null) quotaGuard.recordQuotaExhaustion(e);
                throw new RuntimeException("Failed to fetch rating summary from Firestore: " + e.getMessage(), e);
            }
        }
        return inMemoryRatingSummaries.get(userId);
    }

    private String fetchDisplayName(String uid) {
        if (uid == null) return "User";
        try {
            ProfileResponse profile = profileService.getProfile(uid, null, false, null);
            if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().trim().isEmpty()) {
                return profile.getDisplayName().trim();
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve display name for UID {}: {}", uid, e.getMessage());
        }
        return "User";
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        // Strip HTML/script tags and trim whitespace
        return text.replaceAll("<[^>]*>", "").trim();
    }

    public ReviewResponse toResponse(Review review) {
        if (review == null) return null;
        return new ReviewResponse(
                review.getReviewId(),
                review.getOrderId(),
                review.getListingId(),
                review.getCropId(),
                review.getCropName(),
                review.getReviewerUid(),
                review.getReviewerRole(),
                review.getReviewerDisplayName(),
                review.getRevieweeUid(),
                review.getRevieweeRole(),
                review.getRevieweeDisplayName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getStatus(),
                true, // verifiedTransaction is derived constant for completed order reviews
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private Map<String, Object> mapReviewToDoc(Review review) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("reviewId", review.getReviewId());
        doc.put("orderId", review.getOrderId());
        doc.put("listingId", review.getListingId());
        doc.put("cropId", review.getCropId());
        doc.put("cropName", review.getCropName());
        doc.put("reviewerUid", review.getReviewerUid());
        doc.put("reviewerRole", review.getReviewerRole());
        doc.put("reviewerDisplayName", review.getReviewerDisplayName());
        doc.put("revieweeUid", review.getRevieweeUid());
        doc.put("revieweeRole", review.getRevieweeRole());
        doc.put("revieweeDisplayName", review.getRevieweeDisplayName());
        doc.put("rating", review.getRating());
        doc.put("title", review.getTitle());
        doc.put("comment", review.getComment());
        doc.put("status", review.getStatus());
        doc.put("createdAt", review.getCreatedAt());
        doc.put("updatedAt", review.getUpdatedAt());
        return doc;
    }

    private Review mapDocToReview(DocumentSnapshot doc) {
        Review review = new Review();
        review.setReviewId(doc.getString("reviewId"));
        review.setOrderId(doc.getString("orderId"));
        review.setListingId(doc.getString("listingId"));
        review.setCropId(doc.getString("cropId"));
        review.setCropName(doc.getString("cropName"));
        review.setReviewerUid(doc.getString("reviewerUid"));
        review.setReviewerRole(doc.getString("reviewerRole"));
        review.setReviewerDisplayName(doc.getString("reviewerDisplayName"));
        review.setRevieweeUid(doc.getString("revieweeUid"));
        review.setRevieweeRole(doc.getString("revieweeRole"));
        review.setRevieweeDisplayName(doc.getString("revieweeDisplayName"));
        if (doc.get("rating") != null) {
            review.setRating(((Number) doc.get("rating")).intValue());
        }
        review.setTitle(doc.getString("title"));
        review.setComment(doc.getString("comment"));
        review.setStatus(doc.getString("status"));
        review.setCreatedAt(doc.getString("createdAt"));
        review.setUpdatedAt(doc.getString("updatedAt"));
        return review;
    }

    private Map<String, Object> mapSummaryToDoc(RatingSummary summary) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("userId", summary.getUserId());
        doc.put("userRole", summary.getUserRole());
        doc.put("averageRating", summary.getAverageRating() != null ? summary.getAverageRating().toString() : "0.00");
        doc.put("totalReviews", summary.getTotalReviews());
        doc.put("totalRatingPoints", summary.getTotalRatingPoints());
        doc.put("oneStarCount", summary.getOneStarCount());
        doc.put("twoStarCount", summary.getTwoStarCount());
        doc.put("threeStarCount", summary.getThreeStarCount());
        doc.put("fourStarCount", summary.getFourStarCount());
        doc.put("fiveStarCount", summary.getFiveStarCount());
        doc.put("updatedAt", summary.getUpdatedAt());
        return doc;
    }

    private RatingSummary mapDocToSummary(DocumentSnapshot doc) {
        RatingSummary summary = new RatingSummary();
        summary.setUserId(doc.getString("userId"));
        summary.setUserRole(doc.getString("userRole"));
        if (doc.get("averageRating") != null) {
            summary.setAverageRating(new BigDecimal(doc.get("averageRating").toString()));
        } else {
            summary.setAverageRating(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        summary.setTotalReviews(doc.getLong("totalReviews") != null ? doc.getLong("totalReviews") : 0L);
        summary.setTotalRatingPoints(doc.getLong("totalRatingPoints") != null ? doc.getLong("totalRatingPoints") : 0L);
        summary.setOneStarCount(doc.getLong("oneStarCount") != null ? doc.getLong("oneStarCount") : 0L);
        summary.setTwoStarCount(doc.getLong("twoStarCount") != null ? doc.getLong("twoStarCount") : 0L);
        summary.setThreeStarCount(doc.getLong("threeStarCount") != null ? doc.getLong("threeStarCount") : 0L);
        summary.setFourStarCount(doc.getLong("fourStarCount") != null ? doc.getLong("fourStarCount") : 0L);
        summary.setFiveStarCount(doc.getLong("fiveStarCount") != null ? doc.getLong("fiveStarCount") : 0L);
        summary.setUpdatedAt(doc.getString("updatedAt"));
        return summary;
    }
}
