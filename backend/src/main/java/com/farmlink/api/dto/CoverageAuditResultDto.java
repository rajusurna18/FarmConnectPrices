package com.farmlink.api.dto;

import java.util.*;

public class CoverageAuditResultDto {

    private boolean success;
    private String resourceId;
    private int pagesSampled;
    private int recordsSampled;
    private int uniqueStatesCount;
    private int uniqueDistrictsCount;
    private int uniqueMarketsCount;
    private int uniqueCommoditiesCount;
    private int uniqueVarietiesCount;
    private long durationMs;

    private List<StateCoverageDetailDto> stateSummaries = new ArrayList<>();
    private List<DistrictCoverageDetailDto> districtSummaries = new ArrayList<>();
    private List<CommodityCoverageDetailDto> commoditySummaries = new ArrayList<>();
    private Map<String, Object> samplingDetails = new LinkedHashMap<>();

    public CoverageAuditResultDto() {
    }

    public static class StateCoverageDetailDto {
        private String state;
        private boolean sampled;
        private boolean filterConfirmed;
        private int recordsSampled;
        private int districtCount;
        private int marketCount;
        private int commodityCount;
        private String status; // DISCOVERED, FILTER_CONFIRMED, NOT_YET_VERIFIED

        public StateCoverageDetailDto() {}

        public StateCoverageDetailDto(String state, boolean sampled, boolean filterConfirmed,
                                      int recordsSampled, int districtCount, int marketCount,
                                      int commodityCount, String status) {
            this.state = state;
            this.sampled = sampled;
            this.filterConfirmed = filterConfirmed;
            this.recordsSampled = recordsSampled;
            this.districtCount = districtCount;
            this.marketCount = marketCount;
            this.commodityCount = commodityCount;
            this.status = status;
        }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public boolean isSampled() { return sampled; }
        public void setSampled(boolean sampled) { this.sampled = sampled; }
        public boolean isFilterConfirmed() { return filterConfirmed; }
        public void setFilterConfirmed(boolean filterConfirmed) { this.filterConfirmed = filterConfirmed; }
        public int getRecordsSampled() { return recordsSampled; }
        public void setRecordsSampled(int recordsSampled) { this.recordsSampled = recordsSampled; }
        public int getDistrictCount() { return districtCount; }
        public void setDistrictCount(int districtCount) { this.districtCount = districtCount; }
        public int getMarketCount() { return marketCount; }
        public void setMarketCount(int marketCount) { this.marketCount = marketCount; }
        public int getCommodityCount() { return commodityCount; }
        public void setCommodityCount(int commodityCount) { this.commodityCount = commodityCount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class DistrictCoverageDetailDto {
        private String state;
        private String district;
        private int marketsObserved;
        private int commoditiesObserved;

        public DistrictCoverageDetailDto() {}

        public DistrictCoverageDetailDto(String state, String district, int marketsObserved, int commoditiesObserved) {
            this.state = state;
            this.district = district;
            this.marketsObserved = marketsObserved;
            this.commoditiesObserved = commoditiesObserved;
        }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public int getMarketsObserved() { return marketsObserved; }
        public void setMarketsObserved(int marketsObserved) { this.marketsObserved = marketsObserved; }
        public int getCommoditiesObserved() { return commoditiesObserved; }
        public void setCommoditiesObserved(int commoditiesObserved) { this.commoditiesObserved = commoditiesObserved; }
    }

    public static class CommodityCoverageDetailDto {
        private String commodity;
        private int statesObserved;
        private int districtsObserved;
        private int marketsObserved;

        public CommodityCoverageDetailDto() {}

        public CommodityCoverageDetailDto(String commodity, int statesObserved, int districtsObserved, int marketsObserved) {
            this.commodity = commodity;
            this.statesObserved = statesObserved;
            this.districtsObserved = districtsObserved;
            this.marketsObserved = marketsObserved;
        }

        public String getCommodity() { return commodity; }
        public void setCommodity(String commodity) { this.commodity = commodity; }
        public int getStatesObserved() { return statesObserved; }
        public void setStatesObserved(int statesObserved) { this.statesObserved = statesObserved; }
        public int getDistrictsObserved() { return districtsObserved; }
        public void setDistrictsObserved(int districtsObserved) { this.districtsObserved = districtsObserved; }
        public int getMarketsObserved() { return marketsObserved; }
        public void setMarketsObserved(int marketsObserved) { this.marketsObserved = marketsObserved; }
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public int getPagesSampled() { return pagesSampled; }
    public void setPagesSampled(int pagesSampled) { this.pagesSampled = pagesSampled; }
    public int getRecordsSampled() { return recordsSampled; }
    public void setRecordsSampled(int recordsSampled) { this.recordsSampled = recordsSampled; }
    public int getUniqueStatesCount() { return uniqueStatesCount; }
    public void setUniqueStatesCount(int uniqueStatesCount) { this.uniqueStatesCount = uniqueStatesCount; }
    public int getUniqueDistrictsCount() { return uniqueDistrictsCount; }
    public void setUniqueDistrictsCount(int uniqueDistrictsCount) { this.uniqueDistrictsCount = uniqueDistrictsCount; }
    public int getUniqueMarketsCount() { return uniqueMarketsCount; }
    public void setUniqueMarketsCount(int uniqueMarketsCount) { this.uniqueMarketsCount = uniqueMarketsCount; }
    public int getUniqueCommoditiesCount() { return uniqueCommoditiesCount; }
    public void setUniqueCommoditiesCount(int uniqueCommoditiesCount) { this.uniqueCommoditiesCount = uniqueCommoditiesCount; }
    public int getUniqueVarietiesCount() { return uniqueVarietiesCount; }
    public void setUniqueVarietiesCount(int uniqueVarietiesCount) { this.uniqueVarietiesCount = uniqueVarietiesCount; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public List<StateCoverageDetailDto> getStateSummaries() { return stateSummaries; }
    public void setStateSummaries(List<StateCoverageDetailDto> stateSummaries) { this.stateSummaries = stateSummaries; }
    public List<DistrictCoverageDetailDto> getDistrictSummaries() { return districtSummaries; }
    public void setDistrictSummaries(List<DistrictCoverageDetailDto> districtSummaries) { this.districtSummaries = districtSummaries; }
    public List<CommodityCoverageDetailDto> getCommoditySummaries() { return commoditySummaries; }
    public void setCommoditySummaries(List<CommodityCoverageDetailDto> commoditySummaries) { this.commoditySummaries = commoditySummaries; }
    public Map<String, Object> getSamplingDetails() { return samplingDetails; }
    public void setSamplingDetails(Map<String, Object> samplingDetails) { this.samplingDetails = samplingDetails; }
}
