package com.farmlink.api.dto;

import java.util.ArrayList;
import java.util.List;

public class IngestionResultDto {

    private boolean success;
    private String message;
    private int recordsFetched;
    private int recordsProcessed;
    private int recordsAccepted;
    private int recordsImported; // alias for recordsAccepted / written
    private int recordsSkipped;
    private int recordsRejected;
    private int recordsUnmappedMarkets;
    private int recordsUnmappedCrops;
    private int recordsDuplicated;
    private int recordsUpserted;
    private int recordsUnchanged;
    private int recordsFailed;
    private int pagesFetched;
    private int lastOffset;
    private long durationMs;
    private List<String> errors = new ArrayList<>();
    private String timestamp;

    public IngestionResultDto() {
    }

    public IngestionResultDto(boolean success, String message, int recordsFetched, int recordsImported,
                              int recordsSkipped, int recordsRejected, int recordsUnmappedMarkets,
                              int recordsUnmappedCrops, int recordsDuplicated, int recordsFailed,
                              String timestamp) {
        this.success = success;
        this.message = message;
        this.recordsFetched = recordsFetched;
        this.recordsProcessed = recordsFetched;
        this.recordsAccepted = recordsImported;
        this.recordsImported = recordsImported;
        this.recordsSkipped = recordsSkipped;
        this.recordsRejected = recordsRejected;
        this.recordsUnmappedMarkets = recordsUnmappedMarkets;
        this.recordsUnmappedCrops = recordsUnmappedCrops;
        this.recordsDuplicated = recordsDuplicated;
        this.recordsUpserted = recordsImported;
        this.recordsFailed = recordsFailed;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRecordsFetched() {
        return recordsFetched;
    }

    public void setRecordsFetched(int recordsFetched) {
        this.recordsFetched = recordsFetched;
    }

    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public int getRecordsAccepted() {
        return recordsAccepted;
    }

    public void setRecordsAccepted(int recordsAccepted) {
        this.recordsAccepted = recordsAccepted;
    }

    public int getRecordsImported() {
        return recordsImported;
    }

    public void setRecordsImported(int recordsImported) {
        this.recordsImported = recordsImported;
    }

    public int getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(int recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public int getRecordsRejected() {
        return recordsRejected;
    }

    public void setRecordsRejected(int recordsRejected) {
        this.recordsRejected = recordsRejected;
    }

    public int getRecordsUnmappedMarkets() {
        return recordsUnmappedMarkets;
    }

    public void setRecordsUnmappedMarkets(int recordsUnmappedMarkets) {
        this.recordsUnmappedMarkets = recordsUnmappedMarkets;
    }

    public int getRecordsUnmappedCrops() {
        return recordsUnmappedCrops;
    }

    public void setRecordsUnmappedCrops(int recordsUnmappedCrops) {
        this.recordsUnmappedCrops = recordsUnmappedCrops;
    }

    public int getRecordsDuplicated() {
        return recordsDuplicated;
    }

    public void setRecordsDuplicated(int recordsDuplicated) {
        this.recordsDuplicated = recordsDuplicated;
    }

    public int getRecordsUpserted() {
        return recordsUpserted;
    }

    public void setRecordsUpserted(int recordsUpserted) {
        this.recordsUpserted = recordsUpserted;
    }

    public int getRecordsUnchanged() {
        return recordsUnchanged;
    }

    public void setRecordsUnchanged(int recordsUnchanged) {
        this.recordsUnchanged = recordsUnchanged;
    }

    public int getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(int recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public int getPagesFetched() {
        return pagesFetched;
    }

    public void setPagesFetched(int pagesFetched) {
        this.pagesFetched = pagesFetched;
    }

    public int getLastOffset() {
        return lastOffset;
    }

    public void setLastOffset(int lastOffset) {
        this.lastOffset = lastOffset;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

