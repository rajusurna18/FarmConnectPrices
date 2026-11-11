package com.farmlink.api.dto;

import com.farmlink.api.model.DisputeEvidence;
import com.farmlink.api.model.DisputeHistoryEntry;
import com.farmlink.api.model.DisputeResponseRecord;

import java.util.ArrayList;
import java.util.List;

public class DisputeDetailResponse {
    private DisputeResponse dispute;
    private List<DisputeResponseRecord> responses = new ArrayList<>();
    private List<DisputeEvidence> evidence = new ArrayList<>();
    private List<DisputeHistoryEntry> history = new ArrayList<>();

    public DisputeDetailResponse() {}

    public DisputeDetailResponse(DisputeResponse dispute,
                                 List<DisputeResponseRecord> responses,
                                 List<DisputeEvidence> evidence,
                                 List<DisputeHistoryEntry> history) {
        this.dispute = dispute;
        this.responses = responses != null ? responses : new ArrayList<>();
        this.evidence = evidence != null ? evidence : new ArrayList<>();
        this.history = history != null ? history : new ArrayList<>();
    }

    public DisputeResponse getDispute() { return dispute; }
    public void setDispute(DisputeResponse dispute) { this.dispute = dispute; }

    public List<DisputeResponseRecord> getResponses() { return responses; }
    public void setResponses(List<DisputeResponseRecord> responses) { this.responses = responses; }

    public List<DisputeEvidence> getEvidence() { return evidence; }
    public void setEvidence(List<DisputeEvidence> evidence) { this.evidence = evidence; }

    public List<DisputeHistoryEntry> getHistory() { return history; }
    public void setHistory(List<DisputeHistoryEntry> history) { this.history = history; }
}

