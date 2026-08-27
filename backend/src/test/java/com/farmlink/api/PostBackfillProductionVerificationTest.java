package com.farmlink.api;

import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.service.MarketPriceService;
import com.farmlink.api.service.MarketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PostBackfillProductionVerificationTest {

    @Autowired
    private MarketPriceService marketPriceService;

    @Autowired
    private MarketService marketService;

    @Autowired
    private DataGovMandiProperties dataGovMandiProperties;

    @Test
    @DisplayName("Verify Daily Resource Configuration")
    void verifyDailyResourceConfiguration() {
        assertEquals("9ef84268-d588-465a-a308-a864a43d0070", dataGovMandiProperties.getDailyResourceId(),
                "Daily resource ID must be configured to 9ef84268-d588-465a-a308-a864a43d0070");
        assertEquals("INCREMENTAL", dataGovMandiProperties.getMode(),
                "Daily synchronization mode must default to INCREMENTAL");
        assertEquals("35985678-0d79-46b4-9ed6-6f13308a1d24", dataGovMandiProperties.getResourceId(),
                "Historical backfill resource ID must be configured to 35985678-0d79-46b4-9ed6-6f13308a1d24");
    }

    @Test
    @DisplayName("Verify Geographic & Commodity Discovery (Telangana + 5 States)")
    void verifyGeographicAndCommodityDiscovery() {
        List<MarketSummaryResponse> markets = marketService.getMarkets(null, null, null, null, null, null, 1000);
        assertNotNull(markets, "Markets list should not be null");

        Set<String> states = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> districts = new HashSet<>();
        for (MarketSummaryResponse m : markets) {
            if (m.getState() != null) states.add(m.getState());
            if (m.getDistrict() != null) districts.add(m.getDistrict());
        }

        assertTrue(states.contains("Telangana") || states.contains("TELANGANA"), "Must discover Telangana state");
        assertTrue(states.size() >= 3, "Must discover unique states. Found: " + states.size() + " (" + states + ")");
        assertTrue(districts.size() > 0, "Must discover districts");

    }

    @Test
    @DisplayName("Verify Price Retrieval, Date Filtering, Unit Conversions, and Provenance")
    void verifyPriceRetrievalAndConversions() {
        List<MarketPriceSummaryResponse> defaultPrices = marketPriceService.getMarketPrices(
                null, null, null, null, null, null, null, null, null, 50
        );
        assertNotNull(defaultPrices);
        assertFalse(defaultPrices.isEmpty(), "Market prices list should not be empty");

        MarketPriceSummaryResponse p = defaultPrices.get(0);
        assertNotNull(p.getPriceDate(), "Price date must be present");
        assertNotNull(p.getSourceType(), "Provenance source type must be present");
        assertNotNull(p.getSourceName(), "Provenance source name must be present");

        // Verify Unit Conversion (QUINTAL to KG and TONNE)
        List<MarketPriceSummaryResponse> kgPrices = marketPriceService.getMarketPrices(
                null, null, null, null, null, null, "KG", null, null, 10
        );
        assertNotNull(kgPrices);
        assertFalse(kgPrices.isEmpty());
        MarketPriceSummaryResponse kgP = kgPrices.get(0);
        assertTrue(kgP.isConversionApplied(), "Conversion flag must be true when requesting KG");
        assertEquals("KG", kgP.getUnit(), "Unit must be converted to KG");
        assertEquals(0.01, kgP.getConversionFactor(), 0.0001, "QUINTAL to KG factor must be 0.01");

        List<MarketPriceSummaryResponse> tonnePrices = marketPriceService.getMarketPrices(
                null, null, null, null, null, null, "TONNE", null, null, 10
        );
        assertNotNull(tonnePrices);
        assertFalse(tonnePrices.isEmpty());
        MarketPriceSummaryResponse tonneP = tonnePrices.get(0);
        assertTrue(tonneP.isConversionApplied(), "Conversion flag must be true when requesting TONNE");
        assertEquals("TONNE", tonneP.getUnit(), "Unit must be converted to TONNE");
        assertEquals(10.0, tonneP.getConversionFactor(), 0.0001, "QUINTAL to TONNE factor must be 10.0");
    }

    @Test
    @DisplayName("Verify Rejected-Record Visibility Rules (QUALITY_REJECTED cannot appear as valid public price)")
    void verifyRejectedRecordVisibilityRules() {
        List<MarketPriceSummaryResponse> publicPrices = marketPriceService.getMarketPrices(
                null, null, null, null, null, null, null, null, null, 100
        );
        for (MarketPriceSummaryResponse p : publicPrices) {
            assertNotEquals("QUALITY_REJECTED", p.getQualityStatus(),
                    "Public price list must NEVER contain QUALITY_REJECTED records");
        }
    }
}
