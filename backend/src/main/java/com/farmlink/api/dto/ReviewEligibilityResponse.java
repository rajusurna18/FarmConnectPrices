package com.farmlink.api.dto;

public class ReviewEligibilityResponse {
    private String orderId;
    private boolean eligible;
    private String reason; // ELIGIBLE, ORDER_NOT_COMPLETED, ALREADY_REVIEWED, NOT_ORDER_PARTICIPANT, ORDER_NOT_FOUND, etc.
    private String revieweeUid;
    private String revieweeDisplayName;
    private String revieweeRole;
    private boolean alreadyReviewed;

    public ReviewEligibilityResponse() {}

    public ReviewEligibilityResponse(String orderId, boolean eligible, String reason,
                                     String revieweeUid, String revieweeDisplayName,
                                     String revieweeRole, boolean alreadyReviewed) {
        this.orderId = orderId;
        this.eligible = eligible;
        this.reason = reason;
        this.revieweeUid = revieweeUid;
        this.revieweeDisplayName = revieweeDisplayName;
        this.revieweeRole = revieweeRole;
        this.alreadyReviewed = alreadyReviewed;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRevieweeUid() { return revieweeUid; }
    public void setRevieweeUid(String revieweeUid) { this.revieweeUid = revieweeUid; }

    public String getRevieweeDisplayName() { return revieweeDisplayName; }
    public void setRevieweeDisplayName(String revieweeDisplayName) { this.revieweeDisplayName = revieweeDisplayName; }

    public String getRevieweeRole() { return revieweeRole; }
    public void setRevieweeRole(String revieweeRole) { this.revieweeRole = revieweeRole; }

    public boolean isAlreadyReviewed() { return alreadyReviewed; }
    public void setAlreadyReviewed(boolean alreadyReviewed) { this.alreadyReviewed = alreadyReviewed; }
}
