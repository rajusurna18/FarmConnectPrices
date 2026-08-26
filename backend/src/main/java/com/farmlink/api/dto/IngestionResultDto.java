package com.farmlink.api.dto;

public class IngestionResultDto {

    private boolean success;
    private String message;
    private int recordsFetched;
    private int recordsImported;
    private int recordsSkipped;
    private int recordsRejected;
    private int recordsUnmappedMarkets;
    private int recordsUnmappedCrops;
    private int recordsDuplicated;
    private int recordsFailed;
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
        this.recordsImported = recordsImported;
        this.recordsSkipped = recordsSkipped;
        this.recordsRejected = recordsRejected;
        this.recordsUnmappedMarkets = recordsUnmappedMarkets;
        this.recordsUnmappedCrops = recordsUnmappedCrops;
        this.recordsDuplicated = recordsDuplicated;
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

    public int getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(int recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
