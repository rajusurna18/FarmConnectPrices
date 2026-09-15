package com.farmlink.api.model;

import java.math.BigDecimal;

/**
 * Domain entity representing Rating & Trust Aggregation for a User in Firestore collection 'ratingSummaries'.
 */
public class RatingSummary {
    private String userId;                // Document ID: userId
    private String userRole;              // FARMER, MEDIATOR_BUYER, CUSTOMER
    private BigDecimal averageRating;     // Scale 2, HALF_UP (e.g., 4.75)
    private Long totalReviews;            // Count of received reviews
    private Long totalRatingPoints;       // Sum of rating scores received
    private Long oneStarCount;
    private Long twoStarCount;
    private Long threeStarCount;
    private Long fourStarCount;
    private Long fiveStarCount;
    private String updatedAt;             // ISO 8601 timestamp

    public RatingSummary() {}

    public RatingSummary(String userId, String userRole, BigDecimal averageRating, Long totalReviews,
                         Long totalRatingPoints, Long oneStarCount, Long twoStarCount,
                         Long threeStarCount, Long fourStarCount, Long fiveStarCount, String updatedAt) {
        this.userId = userId;
        this.userRole = userRole;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.totalRatingPoints = totalRatingPoints;
        this.oneStarCount = oneStarCount;
        this.twoStarCount = twoStarCount;
        this.threeStarCount = threeStarCount;
        this.fourStarCount = fourStarCount;
        this.fiveStarCount = fiveStarCount;
        this.updatedAt = updatedAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public Long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }

    public Long getTotalRatingPoints() { return totalRatingPoints; }
    public void setTotalRatingPoints(Long totalRatingPoints) { this.totalRatingPoints = totalRatingPoints; }

    public Long getOneStarCount() { return oneStarCount; }
    public void setOneStarCount(Long oneStarCount) { this.oneStarCount = oneStarCount; }

    public Long getTwoStarCount() { return twoStarCount; }
    public void setTwoStarCount(Long twoStarCount) { this.twoStarCount = twoStarCount; }

    public Long getThreeStarCount() { return threeStarCount; }
    public void setThreeStarCount(Long threeStarCount) { this.threeStarCount = threeStarCount; }

    public Long getFourStarCount() { return fourStarCount; }
    public void setFourStarCount(Long fourStarCount) { this.fourStarCount = fourStarCount; }

    public Long getFiveStarCount() { return fiveStarCount; }
    public void setFiveStarCount(Long fiveStarCount) { this.fiveStarCount = fiveStarCount; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
