package com.farmlink.api.dto;

import java.util.List;

public class OfferPageResponse {
    private List<OfferResponse> items;
    private int page;
    private int size;
    private int totalElements;
    private int totalPages;
    private boolean hasNext;

    public OfferPageResponse() {}

    public OfferPageResponse(List<OfferResponse> items, int page, int size, int totalElements, int totalPages, boolean hasNext) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    public List<OfferResponse> getItems() {
        return items;
    }

    public void setItems(List<OfferResponse> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
