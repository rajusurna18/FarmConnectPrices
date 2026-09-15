package com.farmlink.api.dto;

public class ReviewResponse {
    private String reviewId;
    private String orderId;
    private String listingId;
    private String cropId;
    private String cropName;

    private String reviewerUid;
    private String reviewerRole;
    private String reviewerDisplayName;

    private String revieweeUid;
    private String revieweeRole;
    private String revieweeDisplayName;

    private Integer rating;
    private String title;
    private String comment;

    private String status;
    private boolean verifiedTransaction;
    private String createdAt;
    private String updatedAt;

    public ReviewResponse() {}

    public ReviewResponse(String reviewId, String orderId, String listingId, String cropId, String cropName,
                          String reviewerUid, String reviewerRole, String reviewerDisplayName,
                          String revieweeUid, String revieweeRole, String revieweeDisplayName,
                          Integer rating, String title, String comment, String status,
                          boolean verifiedTransaction, String createdAt, String updatedAt) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.listingId = listingId;
        this.cropId = cropId;
        this.cropName = cropName;
        this.reviewerUid = reviewerUid;
        this.reviewerRole = reviewerRole;
        this.reviewerDisplayName = reviewerDisplayName;
        this.revieweeUid = revieweeUid;
        this.revieweeRole = revieweeRole;
        this.revieweeDisplayName = revieweeDisplayName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.status = status;
        this.verifiedTransaction = verifiedTransaction;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getReviewerUid() { return reviewerUid; }
    public void setReviewerUid(String reviewerUid) { this.reviewerUid = reviewerUid; }

    public String getReviewerRole() { return reviewerRole; }
    public void setReviewerRole(String reviewerRole) { this.reviewerRole = reviewerRole; }

    public String getReviewerDisplayName() { return reviewerDisplayName; }
    public void setReviewerDisplayName(String reviewerDisplayName) { this.reviewerDisplayName = reviewerDisplayName; }

    public String getRevieweeUid() { return revieweeUid; }
    public void setRevieweeUid(String revieweeUid) { this.revieweeUid = revieweeUid; }

    public String getRevieweeRole() { return revieweeRole; }
    public void setRevieweeRole(String revieweeRole) { this.revieweeRole = revieweeRole; }

    public String getRevieweeDisplayName() { return revieweeDisplayName; }
    public void setRevieweeDisplayName(String revieweeDisplayName) { this.revieweeDisplayName = revieweeDisplayName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isVerifiedTransaction() { return verifiedTransaction; }
    public void setVerifiedTransaction(boolean verifiedTransaction) { this.verifiedTransaction = verifiedTransaction; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
