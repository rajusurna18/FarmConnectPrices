package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class NotificationListResponse {
    private List<NotificationResponse> notifications = new ArrayList<>();
    private long totalCount;
    private long unreadCount;
    private int page;
    private int pageSize;
    private boolean hasMore;

    public NotificationListResponse() {}

    public NotificationListResponse(List<NotificationResponse> notifications, long totalCount, long unreadCount, int page, int pageSize, boolean hasMore) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        this.totalCount = totalCount;
        this.unreadCount = unreadCount;
        this.page = page;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
    }

    public List<NotificationResponse> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationResponse> notifications) {
        this.notifications = notifications;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
