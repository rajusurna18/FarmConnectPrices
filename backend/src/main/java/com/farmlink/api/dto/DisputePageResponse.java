package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class DisputePageResponse {
    private List<DisputeResponse> items = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private String nextCursor;

    public DisputePageResponse() {}

    public DisputePageResponse(List<DisputeResponse> items, int page, int size, long totalElements, int totalPages, boolean hasNext) {
        this(items, page, size, totalElements, totalPages, hasNext, null);
    }

    public DisputePageResponse(List<DisputeResponse> items, int page, int size, long totalElements, int totalPages, boolean hasNext, String nextCursor) {
        this.items = items != null ? items : new ArrayList<>();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.nextCursor = nextCursor;
    }

    // Supported property aliases for cursor pagination contract
    public List<DisputeResponse> getContent() {
        return items;
    }

    public void setContent(List<DisputeResponse> content) {
        this.items = content != null ? content : new ArrayList<>();
    }

    public int getPageSize() {
        return size;
    }

    public void setPageSize(int pageSize) {
        this.size = pageSize;
    }

    public List<DisputeResponse> getItems() { return items; }
    public void setItems(List<DisputeResponse> items) { this.items = items != null ? items : new ArrayList<>(); }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
}
