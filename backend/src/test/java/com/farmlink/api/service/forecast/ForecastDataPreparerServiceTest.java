package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForecastDataPreparerServiceTest {

    private ForecastDataPreparerService dataPreparerService;

    @BeforeEach
    void setUp() {
        dataPreparerService = new ForecastDataPreparerService();
    }

    @Test
    void testPrepareDataset_SortsChronologically() {
        List<MarketPriceSummaryResponse> raw = new ArrayList<>();

        MarketPriceSummaryResponse p1 = createObs("2026-09-05", 2500, "VERIFIED");
        MarketPriceSummaryResponse p2 = createObs("2026-09-01", 2100, "VERIFIED");
        MarketPriceSummaryResponse p3 = createObs("2026-09-03", 2300, "VERIFIED");

        raw.add(p1);
        raw.add(p2);
        raw.add(p3);

        List<MarketPriceSummaryResponse> prepared = dataPreparerService.prepareDataset(raw, "crop-1", "mkt-1");

        assertEquals(3, prepared.size());
        assertEquals("2026-09-01", prepared.get(0).getPriceDate());
        assertEquals("2026-09-03", prepared.get(1).getPriceDate());
        assertEquals("2026-09-05", prepared.get(2).getPriceDate());
    }

    @Test
    void testPrepareDataset_PreservesMissingDatesWithoutInterpolation() {
        // Dates: 2026-09-01 and 2026-09-05 (missing 2nd, 3rd, 4th)
        List<MarketPriceSummaryResponse> raw = new ArrayList<>();
        raw.add(createObs("2026-09-05", 2500, "VERIFIED"));
        raw.add(createObs("2026-09-01", 2100, "VERIFIED"));

        List<MarketPriceSummaryResponse> prepared = dataPreparerService.prepareDataset(raw, "crop-1", "mkt-1");

        assertEquals(2, prepared.size());
        assertEquals("2026-09-01", prepared.get(0).getPriceDate());
        assertEquals("2026-09-05", prepared.get(1).getPriceDate());
    }

    @Test
    void testPrepareDataset_FiltersRejectedAndInvalidRecords() {
        List<MarketPriceSummaryResponse> raw = new ArrayList<>();
        raw.add(createObs("2026-09-01", 2100, "VERIFIED"));

        MarketPriceSummaryResponse rejected = createObs("2026-09-02", 2200, "REJECTED");
        raw.add(rejected);

        MarketPriceSummaryResponse invalidModal = createObs("2026-09-03", 0, "VERIFIED");
        raw.add(invalidModal);

        List<MarketPriceSummaryResponse> prepared = dataPreparerService.prepareDataset(raw, "crop-1", "mkt-1");

        assertEquals(1, prepared.size());
        assertEquals("2026-09-01", prepared.get(0).getPriceDate());
    }

    private MarketPriceSummaryResponse createObs(String date, double price, String status) {
        MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
        p.setCropId("crop-1");
        p.setMarketId("mkt-1");
        p.setPriceDate(date);
        p.setModalPrice(price);
        p.setMinPrice(price > 0 ? price - 50 : 0);
        p.setMaxPrice(price > 0 ? price + 50 : 0);
        p.setQualityStatus(status);
        p.setCurrency("INR");
        p.setUnit("QUINTAL");
        return p;
    }
}
