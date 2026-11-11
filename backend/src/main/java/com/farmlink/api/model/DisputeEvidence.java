package com.farmlink.api.model;

/**
 * Domain entity representing supporting evidence metadata for a dispute, stored in subcollection 'disputes/{disputeId}/evidence'.
 * Evidence is strictly metadata-only. No raw file upload or unverified storage paths are accepted.
 */
public class DisputeEvidence {
    private String evidenceId;
    private String disputeId;
    private String registeredByUid;
    private String registeredByRole; // FARMER, MEDIATOR_BUYER, CUSTOMER
    private String fileName;
    private String storagePath; // Null unless verified by authorized server process
    private String contentType; // MIME type (e.g. image/jpeg, image/png, application/pdf)
    private Long fileSize; // in bytes
    private String description;
    private String createdAt;

    public DisputeEvidence() {}

    public DisputeEvidence(String evidenceId, String disputeId, String registeredByUid,
                           String registeredByRole, String fileName,
                           String contentType, Long fileSize, String description,
                           String createdAt) {
        this(evidenceId, disputeId, registeredByUid, registeredByRole, fileName, null, contentType, fileSize, description, createdAt);
    }

    public DisputeEvidence(String evidenceId, String disputeId, String registeredByUid,
                           String registeredByRole, String fileName, String storagePath,
                           String contentType, Long fileSize, String description,
                           String createdAt) {
        this.evidenceId = evidenceId;
        this.disputeId = disputeId;
        this.registeredByUid = registeredByUid;
        this.registeredByRole = registeredByRole;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }

    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }

    public String getRegisteredByUid() { return registeredByUid; }
    public void setRegisteredByUid(String registeredByUid) { this.registeredByUid = registeredByUid; }

    // Compatibility getter/setter for previous uploadedByUid
    public String getUploadedByUid() { return registeredByUid; }
    public void setUploadedByUid(String uploadedByUid) { this.registeredByUid = uploadedByUid; }

    public String getRegisteredByRole() { return registeredByRole; }
    public void setRegisteredByRole(String registeredByRole) { this.registeredByRole = registeredByRole; }

    // Compatibility getter/setter for previous uploadedByRole
    public String getUploadedByRole() { return registeredByRole; }
    public void setUploadedByRole(String uploadedByRole) { this.registeredByRole = uploadedByRole; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
