package com.farmlink.api.service;

import com.farmlink.api.client.DataGovMandiClient;
import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.CoverageAuditResultDto;
import com.farmlink.api.dto.external.DataGovMandiRecordDto;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgmarknetCoverageAuditService {

    private static final Logger logger = LoggerFactory.getLogger(AgmarknetCoverageAuditService.class);

    private final DataGovMandiClient mandiClient;
    private final DataGovMandiProperties properties;

    public AgmarknetCoverageAuditService(DataGovMandiClient mandiClient, DataGovMandiProperties properties) {
        this.mandiClient = mandiClient;
        this.properties = properties;
    }

    public CoverageAuditResultDto performCoverageAudit(
            String targetResourceId,
            Integer requestedPageSize,
            Integer requestedMaxPages,
            Integer startingOffset
    ) {
        long startTime = System.currentTimeMillis();
        CoverageAuditResultDto result = new CoverageAuditResultDto();

        String resourceId = (targetResourceId != null && !targetResourceId.trim().isEmpty())
                ? targetResourceId.trim()
                : properties.getResourceId();

        // Enforce strict upper bounds: pageSize capped at 10,000, maxPages capped at 100
        int pageSize = Math.min((requestedPageSize != null && requestedPageSize > 0) ? requestedPageSize : 1000, 10000);
        int maxPages = Math.min((requestedMaxPages != null && requestedMaxPages > 0) ? requestedMaxPages : 10, 100);
        int startOffset = (startingOffset != null && startingOffset >= 0) ? startingOffset : 0;

        result.setResourceId(resourceId);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("resourceId", resourceId);
        details.put("pageSize", pageSize);
        details.put("maxPages", maxPages);
        details.put("startOffset", startOffset);
        List<Integer> offsetsSampled = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Aggregation data structures for discovery
        Set<String> discoveredStates = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Set<String>> stateToDistricts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Set<String>> stateDistrictToMarkets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Set<String>> stateDistrictMarketToCommodities = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> globalMarkets = new HashSet<>();
        Set<String> globalCommodities = new HashSet<>();
        Set<String> globalVarieties = new HashSet<>();

        Map<String, Integer> stateRecordCounts = new HashMap<>();

        int totalPagesSampled = 0;
        int totalRecordsSampled = 0;

        // PHASE 1: ORDINARY PAGINATION SAMPLING
        logger.info("Starting AGMARKNET Coverage Audit Phase 1 (Pagination Sampling): resourceId={}, pageSize={}, maxPages={}, offset={}",
                resourceId, pageSize, maxPages, startOffset);

        int currentOffset = startOffset;
        for (int page = 0; page < maxPages; page++) {
            offsetsSampled.add(currentOffset);
            Optional<DataGovMandiResponseDto> responseOpt = mandiClient.fetchMandiPrices(resourceId, pageSize, currentOffset);

            if (responseOpt.isEmpty()) {
                errors.add("Failed to fetch response at offset " + currentOffset);
                break;
            }

            DataGovMandiResponseDto response = responseOpt.get();
            List<DataGovMandiRecordDto> records = response.getRecords();
            if (records == null || records.isEmpty()) {
                logger.info("Pagination sampling reached empty page at offset {}", currentOffset);
                break;
            }

            totalPagesSampled++;
            totalRecordsSampled += records.size();

            for (DataGovMandiRecordDto rec : records) {
                processRecordMetadata(rec, discoveredStates, stateToDistricts, stateDistrictToMarkets,
                        stateDistrictMarketToCommodities, globalMarkets, globalCommodities, globalVarieties, stateRecordCounts);
            }

            if (records.size() < pageSize) {
                logger.info("Fetched last page with {} records at offset {}", records.size(), currentOffset);
                break;
            }

            currentOffset += records.size();

            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }


        // PHASE 2: STATE FILTER COVERAGE TEST
        logger.info("Starting AGMARKNET Coverage Audit Phase 2 (State Filter Test) for {} discovered states...", discoveredStates.size());

        Map<String, Boolean> stateFilterConfirmed = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> stateFilterRecordCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String state : discoveredStates) {
            Map<String, String> stateFilter = Map.of("state", state);
            Optional<DataGovMandiResponseDto> filterRespOpt = mandiClient.fetchMandiPrices(resourceId, 100, 0, stateFilter);

            if (filterRespOpt.isPresent() && filterRespOpt.get().getRecords() != null && !filterRespOpt.get().getRecords().isEmpty()) {
                stateFilterConfirmed.put(state, true);
                stateFilterRecordCounts.put(state, filterRespOpt.get().getRecords().size());

                for (DataGovMandiRecordDto rec : filterRespOpt.get().getRecords()) {
                    processRecordMetadata(rec, discoveredStates, stateToDistricts, stateDistrictToMarkets,
                            stateDistrictMarketToCommodities, globalMarkets, globalCommodities, globalVarieties, stateRecordCounts);
                }
            } else {
                stateFilterConfirmed.put(state, false);
                stateFilterRecordCounts.put(state, 0);
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }


        // PHASE 3: STATE RECONCILIATION & REPORT BUILDING
        List<CoverageAuditResultDto.StateCoverageDetailDto> stateSummaries = new ArrayList<>();
        for (String state : discoveredStates) {
            boolean isSampled = stateRecordCounts.getOrDefault(state, 0) > 0;
            boolean isFilterConfirmed = stateFilterConfirmed.getOrDefault(state, false);

            String status = "DISCOVERED";
            if (isFilterConfirmed) {
                status = "FILTER_CONFIRMED";
            }

            int districtCount = stateToDistricts.getOrDefault(state, Collections.emptySet()).size();

            int marketCount = 0;
            for (String sdKey : stateDistrictToMarkets.keySet()) {
                if (sdKey.toLowerCase(Locale.ROOT).startsWith(state.toLowerCase(Locale.ROOT) + "||")) {
                    marketCount += stateDistrictToMarkets.get(sdKey).size();
                }
            }

            int commodityCount = 0;
            Set<String> stateCommodities = new HashSet<>();
            for (String sdmKey : stateDistrictMarketToCommodities.keySet()) {
                if (sdmKey.toLowerCase(Locale.ROOT).startsWith(state.toLowerCase(Locale.ROOT) + "||")) {
                    stateCommodities.addAll(stateDistrictMarketToCommodities.get(sdmKey));
                }
            }
            commodityCount = stateCommodities.size();

            stateSummaries.add(new CoverageAuditResultDto.StateCoverageDetailDto(
                    state, isSampled, isFilterConfirmed, stateRecordCounts.getOrDefault(state, 0),
                    districtCount, marketCount, commodityCount, status
            ));
        }

        // Build District Summaries
        List<CoverageAuditResultDto.DistrictCoverageDetailDto> districtSummaries = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : stateDistrictToMarkets.entrySet()) {
            String[] parts = entry.getKey().split("\\|\\|");
            if (parts.length == 2) {
                String st = parts[0];
                String dt = parts[1];
                int mktCount = entry.getValue().size();

                Set<String> distCommodities = new HashSet<>();
                for (Map.Entry<String, Set<String>> sdmEntry : stateDistrictMarketToCommodities.entrySet()) {
                    if (sdmEntry.getKey().toLowerCase(Locale.ROOT).startsWith((st + "||" + dt).toLowerCase(Locale.ROOT) + "||")) {
                        distCommodities.addAll(sdmEntry.getValue());
                    }
                }
                districtSummaries.add(new CoverageAuditResultDto.DistrictCoverageDetailDto(st, dt, mktCount, distCommodities.size()));
            }
        }

        // Build Commodity Summaries
        Map<String, Set<String>> commodityToStates = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Set<String>> commodityToDistricts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Set<String>> commodityToMarkets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, Set<String>> sdmEntry : stateDistrictMarketToCommodities.entrySet()) {
            String[] parts = sdmEntry.getKey().split("\\|\\|");
            if (parts.length == 3) {
                String st = parts[0];
                String dt = parts[1];
                String mk = parts[2];
                for (String cmd : sdmEntry.getValue()) {
                    commodityToStates.computeIfAbsent(cmd, k -> new HashSet<>()).add(st);
                    commodityToDistricts.computeIfAbsent(cmd, k -> new HashSet<>()).add(st + "||" + dt);
                    commodityToMarkets.computeIfAbsent(cmd, k -> new HashSet<>()).add(st + "||" + dt + "||" + mk);
                }
            }
        }

        List<CoverageAuditResultDto.CommodityCoverageDetailDto> commoditySummaries = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : commodityToStates.entrySet()) {
            String cmd = entry.getKey();
            int stCount = entry.getValue().size();
            int dtCount = commodityToDistricts.getOrDefault(cmd, Collections.emptySet()).size();
            int mkCount = commodityToMarkets.getOrDefault(cmd, Collections.emptySet()).size();
            commoditySummaries.add(new CoverageAuditResultDto.CommodityCoverageDetailDto(cmd, stCount, dtCount, mkCount));
        }

        long duration = System.currentTimeMillis() - startTime;

        details.put("offsetsSampled", offsetsSampled);
        details.put("errors", errors);

        result.setSuccess(true);
        result.setPagesSampled(totalPagesSampled);
        result.setRecordsSampled(totalRecordsSampled);
        result.setUniqueStatesCount(discoveredStates.size());
        result.setUniqueDistrictsCount(stateToDistricts.values().stream().mapToInt(Set::size).sum());
        result.setUniqueMarketsCount(globalMarkets.size());
        result.setUniqueCommoditiesCount(globalCommodities.size());
        result.setUniqueVarietiesCount(globalVarieties.size());
        result.setDurationMs(duration);

        result.setStateSummaries(stateSummaries);
        result.setDistrictSummaries(districtSummaries);
        result.setCommoditySummaries(commoditySummaries);
        result.setSamplingDetails(details);

        logger.info("AGMARKNET Coverage Audit complete in {} ms. States: {}, Districts: {}, Markets: {}, Commodities: {}",
                duration, discoveredStates.size(), result.getUniqueDistrictsCount(), globalMarkets.size(), globalCommodities.size());

        return result;
    }

    private void processRecordMetadata(
            DataGovMandiRecordDto rec,
            Set<String> states,
            Map<String, Set<String>> stateToDistricts,
            Map<String, Set<String>> stateDistrictToMarkets,
            Map<String, Set<String>> stateDistrictMarketToCommodities,
            Set<String> globalMarkets,
            Set<String> globalCommodities,
            Set<String> globalVarieties,
            Map<String, Integer> stateRecordCounts
    ) {
        if (rec == null) return;
        String st = rec.getState();
        String dt = rec.getDistrict();
        String mk = rec.getMarket();
        String cm = rec.getCommodity();
        String vr = rec.getVariety();

        if (st != null && !st.trim().isEmpty()) {
            String cleanState = st.trim();
            states.add(cleanState);
            stateRecordCounts.put(cleanState, stateRecordCounts.getOrDefault(cleanState, 0) + 1);

            if (dt != null && !dt.trim().isEmpty()) {
                String cleanDistrict = dt.trim();
                stateToDistricts.computeIfAbsent(cleanState, k -> new HashSet<>()).add(cleanDistrict);

                if (mk != null && !mk.trim().isEmpty()) {
                    String cleanMarket = mk.trim();
                    String sdKey = cleanState + "||" + cleanDistrict;
                    stateDistrictToMarkets.computeIfAbsent(sdKey, k -> new HashSet<>()).add(cleanMarket);
                    globalMarkets.add(cleanState + "||" + cleanDistrict + "||" + cleanMarket);

                    if (cm != null && !cm.trim().isEmpty()) {
                        String cleanCommodity = cm.trim();
                        String sdmKey = cleanState + "||" + cleanDistrict + "||" + cleanMarket;
                        stateDistrictMarketToCommodities.computeIfAbsent(sdmKey, k -> new HashSet<>()).add(cleanCommodity);
                        globalCommodities.add(cleanCommodity);
                    }
                }
            }
        }

        if (vr != null && !vr.trim().isEmpty()) {
            globalVarieties.add(vr.trim());
        }
    }
}
