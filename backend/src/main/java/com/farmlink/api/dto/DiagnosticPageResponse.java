package com.farmlink.api.dto;

import java.util.List;

public class DiagnosticPageResponse {

    private List<DiagnosticRecordResponse> items;
    private int limit;
    private boolean hasNext;
    private String nextCursor;

    public DiagnosticPageResponse() {
    }

    public DiagnosticPageResponse(List<DiagnosticRecordResponse> items, int limit, boolean hasNext, String nextCursor) {
        this.items = items;
        this.limit = limit;
        this.hasNext = hasNext;
        this.nextCursor = nextCursor;
    }

    public List<DiagnosticRecordResponse> getItems() {
        return items;
    }

    public void setItems(List<DiagnosticRecordResponse> items) {
        this.items = items;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}

