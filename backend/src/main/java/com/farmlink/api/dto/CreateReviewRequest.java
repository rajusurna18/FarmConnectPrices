package com.farmlink.api.dto;

public class CreateReviewRequest {
    private String orderId;
    private Integer rating;
    private String title;
    private String comment;

    public CreateReviewRequest() {}

    public CreateReviewRequest(String orderId, Integer rating, String title, String comment) {
        this.orderId = orderId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
