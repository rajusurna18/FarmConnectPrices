package com.farmlink.api;

import com.farmlink.api.dto.IngestionResultDto;
import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.service.DataGovIngestionService;
import com.farmlink.api.service.MarketPriceService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("live-verification")
@org.junit.jupiter.api.Disabled("Manual production backfill runner")
public class ControlledProductionBackfillRunnerTest {


    private static final Logger logger = LoggerFactory.getLogger(ControlledProductionBackfillRunnerTest.class);

    @Autowired
    private DataGovIngestionService ingestionService;

    @Autowired
    private MarketPriceService marketPriceService;

    @Autowired
    private com.farmlink.api.config.DataGovMandiProperties properties;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        java.io.File envFile = new java.io.File(".env");
        if (!envFile.exists()) {
            envFile = new java.io.File("backend/.env");
        }
        if (envFile.exists()) {
            List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("DATA_GOV_IN_API_KEY=")) {
                    String apiKey = line.split("=", 2)[1].trim();
                    properties.setApiKey(apiKey);
                }
            }
        }
    }


    @Test
    public void executeControlledProductionBackfill() {
        System.out.println("===================================================================");
        System.out.println("   AGMARKNET CONTROLLED PRODUCTION BACKFILL - LIVE RUN (50,000 RECS)  ");
        System.out.println("===================================================================");

        String resourceId = "35985678-0d79-46b4-9ed6-6f13308a1d24";
        int pageSize = 500;
        int maxPages = 100; // Cap at 50,000 records requested max

        long startTime = System.currentTimeMillis();
        IngestionResultDto result = ingestionService.ingestMandiPrices(
                "BACKFILL",
                resourceId,
                pageSize,
                maxPages,
                null,
                0
        );

        long durationMs = System.currentTimeMillis() - startTime;

        assertNotNull(result, "Ingestion result must not be null");
        assertTrue(result.getRecordsFetched() > 0, "Should fetch records from live API");
        assertTrue(result.getRecordsFetched() <= 50000, "Must not request more than 50,000 records");

        // Query stored/observed market price records
        List<com.farmlink.api.dto.MarketPriceSummaryResponse> storedPrices = marketPriceService.getMarketPrices(
                null, null, null, null, null, null, null, null, null, 1000
        );

        Set<String> uniqueStates = new TreeSet<>();
        Set<String> uniqueDistricts = new TreeSet<>();
        Set<String> uniqueMarkets = new TreeSet<>();
        Set<String> uniqueCommodities = new TreeSet<>();
        Set<String> uniqueVarieties = new TreeSet<>();

        String earliestDate = null;
        String latestDate = null;

        for (com.farmlink.api.dto.MarketPriceSummaryResponse p : storedPrices) {
            if (p.getMarketName() != null) {
                uniqueMarkets.add(p.getMarketName());
            }
            if (p.getCropName() != null) {
                uniqueCommodities.add(p.getCropName());
            }
            if (p.getPriceDate() != null) {
                String d = p.getPriceDate();
                if (earliestDate == null || d.compareTo(earliestDate) < 0) {
                    earliestDate = d;
                }
                if (latestDate == null || d.compareTo(latestDate) > 0) {
                    latestDate = d;
                }
            }
        }

        System.out.println("-------------------------------------------------------------------");
        System.out.println("1. Records Fetched:      " + result.getRecordsFetched());
        System.out.println("2. Records Accepted:     " + result.getRecordsAccepted());
        System.out.println("3. Records Rejected:     " + result.getRecordsRejected());
        System.out.println("4. Records Upserted:     " + result.getRecordsUpserted());
        System.out.println("5. Records Unchanged:    " + result.getRecordsUnchanged());
        System.out.println("6. Unmapped Markets:     " + result.getRecordsUnmappedMarkets());
        System.out.println("7. Unmapped Commodities: " + result.getRecordsUnmappedCrops());

        System.out.println("8. Unique States:        " + uniqueStates.size());
        System.out.println("9. Unique Districts:     " + uniqueDistricts.size());
        System.out.println("10. Unique Markets:      " + uniqueMarkets.size());
        System.out.println("11. Unique Commodities:  " + uniqueCommodities.size());
        System.out.println("12. Unique Varieties:    " + uniqueVarieties.size());
        System.out.println("13. Earliest Date:       " + (earliestDate != null ? earliestDate : "N/A"));
        System.out.println("14. Latest Date:         " + (latestDate != null ? latestDate : "N/A"));
        System.out.println("15. API Errors/Retries:  " + (result.getErrors() != null ? result.getErrors().size() : 0));
        System.out.println("16. Firestore Writes:    " + result.getRecordsUpserted());
        System.out.println("17. Total Duration:      " + durationMs + " ms");
        System.out.println("-------------------------------------------------------------------");

    }
}
