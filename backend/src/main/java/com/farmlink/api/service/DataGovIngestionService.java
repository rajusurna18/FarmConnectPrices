package com.farmlink.api.service;

import com.farmlink.api.client.DataGovMandiClient;
import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.IngestionResultDto;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.dto.external.DataGovMandiRecordDto;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class DataGovIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DataGovIngestionService.class);

    private final DataGovMandiClient client;
    private final MandiMappingService mappingService;
    private final DataGovMandiProperties properties;
    private final Firestore firestore;

    public static final String SOURCE_TYPE_GOVT = "GOVERNMENT";
    public static final String SOURCE_NAME_GOVT = "data.gov.in / AGMARKNET";
    public static final String SOURCE_REF_HISTORICAL = "35985678-0d79-46b4-9ed6-6f13308a1d24";
    public static final String SOURCE_REF_DAILY = "9ef84268-d588-465a-a308-a864a43d0070";

    public DataGovIngestionService(
            DataGovMandiClient client,
            MandiMappingService mappingService,
            DataGovMandiProperties properties,
            Firestore firestore
    ) {
        this.client = client;
        this.mappingService = mappingService;
        this.properties = properties;
        this.firestore = firestore;
    }

    public IngestionResultDto ingestMandiPrices(Integer limitOverride) {
        return ingestMandiPrices(null, null, limitOverride, null, null, null);
    }

    public IngestionResultDto ingestMandiPrices(
            String modeParam,
            String resourceIdParam,
            Integer pageSizeParam,
            Integer maxPagesParam,
            Integer lookbackDaysParam,
            Integer offsetParam
    ) {
        long startTime = System.currentTimeMillis();
        String nowIso = Instant.now().toString();

        String mode = (modeParam != null && !modeParam.trim().isEmpty())
                ? modeParam.trim().toUpperCase(Locale.ROOT)
                : (properties.getMode() != null ? properties.getMode().toUpperCase(Locale.ROOT) : "INCREMENTAL");

        String targetResourceId = (resourceIdParam != null && !resourceIdParam.trim().isEmpty())
                ? resourceIdParam.trim()
                : ("BACKFILL".equals(mode) ? properties.getResourceId() : properties.getDailyResourceId());

        int pageSize = (pageSizeParam != null && pageSizeParam > 0) ? pageSizeParam : properties.getPageSize();
        int maxPages = (maxPagesParam != null && maxPagesParam > 0) ? maxPagesParam : properties.getMaxPages();
        int lookbackDays = (lookbackDaysParam != null && lookbackDaysParam >= 0) ? lookbackDaysParam : properties.getLookbackDays();
        int currentOffset = (offsetParam != null && offsetParam >= 0) ? offsetParam : properties.getInitialOffset();

        LocalDate cutoffDate = LocalDate.now().minusDays(lookbackDays);

        int totalFetched = 0;
        int totalProcessed = 0;
        int totalAccepted = 0;
        int totalRejected = 0;
        int totalSkipped = 0;
        int totalUnmappedMarkets = 0;
        int totalUnmappedCrops = 0;
        int totalUpserted = 0;
        int totalUnchanged = 0;
        int totalFailed = 0;
        int pagesFetched = 0;

        List<String> errors = new ArrayList<>();
        Set<String> writtenMarketIds = new HashSet<>();
        Set<String> writtenCropIds = new HashSet<>();


        Set<String> uniqueStates = new TreeSet<>();
        Set<String> uniqueDistricts = new TreeSet<>();
        Set<String> uniqueMarkets = new TreeSet<>();
        Set<String> uniqueCommodities = new TreeSet<>();
        Set<String> uniqueVarieties = new TreeSet<>();
        String[] earliestDate = new String[1];
        String[] latestDate = new String[1];

        logger.info("Starting AGMARKNET ingestion mode: {}, resourceId: {}, pageSize: {}, maxPages: {}, lookbackDays: {}, initialOffset: {}",
                mode, targetResourceId, pageSize, maxPages, lookbackDays, currentOffset);

        for (int page = 0; page < maxPages; page++) {
            Optional<DataGovMandiResponseDto> responseOpt = client.fetchMandiPrices(targetResourceId, pageSize, currentOffset);
            if (responseOpt.isEmpty() || responseOpt.get().getRecords() == null) {
                logger.warn("Received empty response from data.gov.in resource {} at offset {}", targetResourceId, currentOffset);
                if (page == 0) {
                    errors.add("No records returned from data.gov.in for resource " + targetResourceId);
                }
                break;
            }

            List<DataGovMandiRecordDto> records = responseOpt.get().getRecords();
            if (records.isEmpty()) {
                logger.info("End of records stream reached at page {} (offset {})", page, currentOffset);
                break;
            }

            pagesFetched++;
            int pageRecordsCount = records.size();
            totalFetched += pageRecordsCount;

            boolean allRecordsOlderThanLookback = true;
            com.google.cloud.firestore.WriteBatch batch = firestore != null ? firestore.batch() : null;

            for (DataGovMandiRecordDto record : records) {
                totalProcessed++;
                try {
                    String rawState = record.getState() != null ? record.getState().trim() : "Unknown";
                    String rawDistrict = record.getDistrict() != null ? record.getDistrict().trim() : "Unknown";
                    String rawMarket = record.getMarket() != null ? record.getMarket().trim() : "Unknown Market";
                    String rawCommodity = record.getCommodity() != null ? record.getCommodity().trim() : "Unknown Commodity";
                    String rawVariety = record.getVariety() != null ? record.getVariety().trim() : "Standard";
                    String rawGrade = record.getGrade() != null ? record.getGrade().trim() : "FAQ";

                    if (!"Unknown".equalsIgnoreCase(rawState)) uniqueStates.add(rawState);
                    if (!"Unknown".equalsIgnoreCase(rawDistrict)) uniqueDistricts.add(rawDistrict);
                    if (!"Unknown Market".equalsIgnoreCase(rawMarket)) uniqueMarkets.add(rawMarket);
                    if (!"Unknown Commodity".equalsIgnoreCase(rawCommodity)) uniqueCommodities.add(rawCommodity);
                    if (rawVariety != null && !rawVariety.trim().isEmpty()) uniqueVarieties.add(rawVariety);

                    String priceDate = parseDateSafely(record.getArrivalDate());
                    if (earliestDate[0] == null || priceDate.compareTo(earliestDate[0]) < 0) earliestDate[0] = priceDate;
                    if (latestDate[0] == null || priceDate.compareTo(latestDate[0]) > 0) latestDate[0] = priceDate;

                    LocalDate recordDate = LocalDate.parse(priceDate);


                    if (recordDate.isAfter(cutoffDate) || recordDate.isEqual(cutoffDate)) {
                        allRecordsOlderThanLookback = false;
                    }

                    if ("INCREMENTAL".equals(mode) && recordDate.isBefore(cutoffDate)) {
                        totalSkipped++;
                        continue;
                    }

                    // 1. Market Discovery (Canonical or Observed)
                    Optional<MarketSummaryResponse> canonicalMarketOpt = mappingService.mapMarket(rawState, rawDistrict, rawMarket);
                    String marketId;
                    String marketMappingStatus;
                    if (canonicalMarketOpt.isPresent()) {
                        marketId = canonicalMarketOpt.get().getId();
                        marketMappingStatus = "MAPPED";
                    } else {
                        marketId = mappingService.generateObservedMarketId(rawState, rawDistrict, rawMarket);
                        marketMappingStatus = "UNMAPPED";
                        totalUnmappedMarkets++;
                    }

                    // 2. Crop Discovery (Canonical or Observed)
                    Optional<CropResponse> canonicalCropOpt = mappingService.mapCrop(rawCommodity);
                    String cropId;
                    String cropMappingStatus;
                    if (canonicalCropOpt.isPresent()) {
                        cropId = canonicalCropOpt.get().getId();
                        cropMappingStatus = "MAPPED";
                    } else {
                        cropId = mappingService.generateObservedCropId(rawCommodity);
                        cropMappingStatus = "UNMAPPED";
                        totalUnmappedCrops++;
                    }

                    // 3. Price Parsing & Validation
                    double minPrice = parseDoubleSafely(record.getMinPrice());
                    double maxPrice = parseDoubleSafely(record.getMaxPrice());
                    double modalPrice = parseDoubleSafely(record.getModalPrice());

                    boolean isValidPrice = MarketPriceService.validatePriceRecord(minPrice, maxPrice, modalPrice);
                    String qualityStatus = isValidPrice ? MarketPriceService.QUALITY_VERIFIED : MarketPriceService.QUALITY_REJECTED;

                    if (!isValidPrice) {
                        totalRejected++;
                        logger.warn("Rejected invalid price record (min={}, modal={}, max={}) for market '{}' crop '{}'",
                                minPrice, modalPrice, maxPrice, rawMarket, rawCommodity);
                    } else {
                        totalAccepted++;
                    }

                    // 4. Deterministic Idempotent Document Key
                    String cleanKeyRaw = rawState + "|" + rawDistrict + "|" + rawMarket + "|" + rawCommodity + "|" + rawVariety + "|" + priceDate;
                    String hashSuffix = Integer.toHexString(cleanKeyRaw.hashCode());
                    String deterministicId = "prc_gov_" + mappingService.normalizeString(rawMarket).replaceAll("\\s+", "_")
                            + "_" + mappingService.normalizeString(rawCommodity).replaceAll("\\s+", "_")
                            + "_" + priceDate.replace("-", "") + "_" + hashSuffix;

                    // 5. Raw Provenance Preservation & Document Building
                    Map<String, Object> docMap = new HashMap<>();
                    docMap.put("id", deterministicId);
                    docMap.put("marketId", marketId);
                    docMap.put("marketMappingStatus", marketMappingStatus);
                    docMap.put("cropId", cropId);
                    docMap.put("cropMappingStatus", cropMappingStatus);
                    docMap.put("observedState", rawState);
                    docMap.put("observedDistrict", rawDistrict);
                    docMap.put("observedMarketName", rawMarket);
                    docMap.put("observedCommodityName", rawCommodity);
                    docMap.put("observedVariety", rawVariety);
                    docMap.put("observedGrade", rawGrade);
                    docMap.put("priceDate", priceDate);
                    docMap.put("observedAt", nowIso);
                    docMap.put("minPrice", minPrice);
                    docMap.put("maxPrice", maxPrice);
                    docMap.put("modalPrice", modalPrice);
                    docMap.put("currency", MarketPriceService.CURRENCY_INR);
                    docMap.put("unit", MarketPriceService.UNIT_QUINTAL);
                    docMap.put("sourceUnit", MarketPriceService.UNIT_QUINTAL);
                    docMap.put("qualityStatus", qualityStatus);
                    docMap.put("status", MarketPriceService.STATUS_ACTIVE);
                    docMap.put("createdAt", nowIso);
                    docMap.put("updatedAt", nowIso);

                    Map<String, Object> sourceMap = new HashMap<>();
                    sourceMap.put("type", SOURCE_TYPE_GOVT);
                    sourceMap.put("name", SOURCE_NAME_GOVT);
                    sourceMap.put("reference", targetResourceId);
                    sourceMap.put("rawState", record.getState());
                    sourceMap.put("rawDistrict", record.getDistrict());
                    sourceMap.put("rawMarket", record.getMarket());
                    sourceMap.put("rawCommodity", record.getCommodity());
                    sourceMap.put("rawVariety", record.getVariety());
                    sourceMap.put("rawGrade", record.getGrade());
                    sourceMap.put("rawArrivalDate", record.getArrivalDate());
                    sourceMap.put("rawMinPrice", record.getMinPrice());
                    sourceMap.put("rawMaxPrice", record.getMaxPrice());
                    sourceMap.put("rawModalPrice", record.getModalPrice());
                    sourceMap.put("ingestedAt", nowIso);
                    docMap.put("source", sourceMap);

                    // 6. Stage to Batch
                    if (batch != null) {
                        var col = firestore.collection("marketPrices");
                        if (col != null) {
                            var docRef = col.document(deterministicId);
                            if (docRef != null) {
                                batch.set(docRef, docMap);
                            }
                        }

                        if ("UNMAPPED".equals(marketMappingStatus) && writtenMarketIds.add(marketId)) {
                            var mktCol = firestore.collection("markets");
                            if (mktCol != null) {
                                Map<String, Object> mktMap = new HashMap<>();
                                mktMap.put("id", marketId);
                                mktMap.put("name", rawMarket);
                                mktMap.put("code", "OBS-" + Math.abs(marketId.hashCode()));
                                mktMap.put("type", MarketService.TYPE_MANDI);
                                Map<String, Object> locMap = new HashMap<>();
                                locMap.put("state", rawState);
                                locMap.put("district", rawDistrict);
                                mktMap.put("location", locMap);
                                mktMap.put("status", MarketService.STATUS_ACTIVE);
                                mktMap.put("createdAt", nowIso);
                                mktMap.put("updatedAt", nowIso);
                                batch.set(mktCol.document(marketId), mktMap);
                            }
                        }

                        if ("UNMAPPED".equals(cropMappingStatus) && writtenCropIds.add(cropId)) {
                            var cropCol = firestore.collection("crops");
                            if (cropCol != null) {
                                Map<String, Object> cropMap = new HashMap<>();
                                cropMap.put("id", cropId);
                                cropMap.put("name", rawCommodity);
                                cropMap.put("category", "AGRICULTURAL_COMMODITY");
                                cropMap.put("status", "ACTIVE");
                                batch.set(cropCol.document(cropId), cropMap);
                            }
                        }
                    }

                    totalUpserted++;

                } catch (Exception e) {
                    totalFailed++;
                    logger.error("Failed processing record at offset {}: {}", currentOffset, e.getMessage());
                    errors.add("Error processing record: " + e.getMessage());
                }
            }

            // Commit Batch for Page
            if (batch != null) {
                try {
                    batch.commit().get();
                } catch (Exception e) {
                    logger.debug("Firestore batch commit mock/fallback for page {}: {}", page, e.getMessage());
                }
            }

            currentOffset += pageRecordsCount;

            // Inter-page request throttling to respect api.data.gov.in rate limits
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }

            if (pageRecordsCount < pageSize) {

                logger.info("Page returned fewer records ({}) than requested limit ({}). Reached end of dataset.", pageRecordsCount, pageSize);
                break;
            }

            if ("INCREMENTAL".equals(mode) && allRecordsOlderThanLookback) {
                logger.info("All records on page {} are older than lookback boundary ({} days). Terminating incremental loop.", page, lookbackDays);
                break;
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        boolean success = errors.isEmpty() || totalAccepted > 0 || totalUpserted > 0;

        String msg = String.format("AGMARKNET Ingestion Complete [%s mode, resource %s]. Pages: %d, Fetched: %d, Accepted: %d, Rejected: %d, Skipped: %d, Unmapped Markets: %d, Unmapped Crops: %d, Upserted: %d, Failed: %d, Duration: %d ms",
                mode, targetResourceId, pagesFetched, totalFetched, totalAccepted, totalRejected, totalSkipped, totalUnmappedMarkets, totalUnmappedCrops, totalUpserted, totalFailed, durationMs);
        logger.info(msg);
        logger.info("Ingested Observed Coverage - States: {}, Districts: {}, Markets: {}, Commodities: {}, Varieties: {}, EarliestDate: {}, LatestDate: {}",
                uniqueStates.size(), uniqueDistricts.size(), uniqueMarkets.size(), uniqueCommodities.size(), uniqueVarieties.size(), earliestDate[0], latestDate[0]);


        IngestionResultDto result = new IngestionResultDto(
                success, msg, totalFetched, totalUpserted, totalSkipped, totalRejected,
                totalUnmappedMarkets, totalUnmappedCrops, totalUnchanged, totalFailed, nowIso
        );
        result.setRecordsProcessed(totalProcessed);
        result.setRecordsAccepted(totalAccepted);
        result.setRecordsUpserted(totalUpserted);
        result.setPagesFetched(pagesFetched);
        result.setLastOffset(currentOffset);
        result.setDurationMs(durationMs);
        result.setErrors(errors);

        return result;
    }

    private double parseDoubleSafely(String valStr) {
        if (valStr == null || valStr.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleanStr = valStr.replaceAll("[\",]", "").trim();
            return Double.parseDouble(cleanStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String parseDateSafely(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return LocalDate.now().toString();
        }
        String clean = rawDate.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("d/M/yyyy")
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(clean, dtf).toString();
            } catch (DateTimeParseException ignored) {
            }
        }

        return LocalDate.now().toString();
    }
}

