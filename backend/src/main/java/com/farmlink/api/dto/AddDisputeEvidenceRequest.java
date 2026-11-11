package com.farmlink.api.dto;

public class AddDisputeEvidenceRequest {
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String description;

    public AddDisputeEvidenceRequest() {}

    public AddDisputeEvidenceRequest(String fileName, String contentType, Long fileSize, String description) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.description = description;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
