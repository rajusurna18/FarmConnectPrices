package com.farmlink.api.dto;

import java.util.List;

public class BuyerOpportunityPageResponse {
    private List<BuyerOpportunityDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private String statusState; // SUCCESS, NO_MATCHES, INSUFFICIENT_DATA

    public BuyerOpportunityPageResponse() {}

    public BuyerOpportunityPageResponse(List<BuyerOpportunityDto> items, int page, int size,
                                        long totalElements, int totalPages, boolean hasNext,
                                        String statusState) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.statusState = statusState;
    }

    public List<BuyerOpportunityDto> getItems() { return items; }
    public void setItems(List<BuyerOpportunityDto> items) { this.items = items; }

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

    public String getStatusState() { return statusState; }
    public void setStatusState(String statusState) { this.statusState = statusState; }
}
