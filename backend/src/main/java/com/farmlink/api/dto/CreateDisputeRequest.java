package com.farmlink.api.dto;

public class CreateDisputeRequest {
    private String orderId;
    private String issueCategory;
    private String subject;
    private String description;

    public CreateDisputeRequest() {}

    public CreateDisputeRequest(String orderId, String issueCategory, String subject, String description) {
        this.orderId = orderId;
        this.issueCategory = issueCategory;
        this.subject = subject;
        this.description = description;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getIssueCategory() { return issueCategory; }
    public void setIssueCategory(String issueCategory) { this.issueCategory = issueCategory; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

