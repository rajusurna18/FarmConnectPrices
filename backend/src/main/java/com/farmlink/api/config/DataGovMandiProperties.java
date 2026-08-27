package com.farmlink.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "data-gov-in")
public class DataGovMandiProperties {

    private String baseUrl = "https://api.data.gov.in/resource";
    private String resourceId = "35985678-0d79-46b4-9ed6-6f13308a1d24"; // Historical/Backfill Resource
    private String dailyResourceId = "9ef84268-d588-465a-a308-a864a43d0070"; // Daily Bulletin Resource
    private String apiKey = "";

    private boolean enabled = false;
    private String mode = "INCREMENTAL"; // INCREMENTAL or BACKFILL
    private int pageSize = 1000;
    private int maxPages = 10;
    private int lookbackDays = 30;
    private int initialOffset = 0;
    private int retryAttempts = 3;
    private long requestTimeoutMs = 30000;


    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getDailyResourceId() {
        return dailyResourceId;
    }

    public void setDailyResourceId(String dailyResourceId) {
        this.dailyResourceId = dailyResourceId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public int getInitialOffset() {
        return initialOffset;
    }

    public void setInitialOffset(int initialOffset) {
        this.initialOffset = initialOffset;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }
}

