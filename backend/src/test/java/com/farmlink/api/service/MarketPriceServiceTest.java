package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.MarketResponse;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketPriceServiceTest {

    private Firestore firestore;
    private MarketService marketService;
    private CropMasterService cropMasterService;
    private PriceUnitConversionService conversionService;
    private MarketPriceService marketPriceService;

    @BeforeEach
    void setUp() {
        firestore = mock(Firestore.class);
        marketService = mock(MarketService.class);
        cropMasterService = mock(CropMasterService.class);
        conversionService = new PriceUnitConversionService();

        marketPriceService = new MarketPriceService(firestore, marketService, cropMasterService, conversionService);
    }

    @Test
    void validatePriceRecord_validatesRelationshipCorrectly() {
        // min <= modal <= max (Valid)
        assertTrue(MarketPriceService.validatePriceRecord(100.0, 200.0, 150.0));
        assertTrue(MarketPriceService.validatePriceRecord(100.0, 100.0, 100.0));

        // Invalid relationships
        assertFalse(MarketPriceService.validatePriceRecord(200.0, 100.0, 150.0)); // min > max
        assertFalse(MarketPriceService.validatePriceRecord(100.0, 200.0, 250.0)); // modal > max
        assertFalse(MarketPriceService.validatePriceRecord(150.0, 200.0, 100.0)); // modal < min
        assertFalse(MarketPriceService.validatePriceRecord(-10.0, 100.0, 50.0));  // negative min
    }

    @Test
    void getMarketPrices_filtersByMarketAndCrop_andExcludesRejected() {
        List<MarketPriceSummaryResponse> prices = marketPriceService.getMarketPrices(
                "mkt-guntur-mandi", "crop-chilli", null, null, null, null, null, null, null, 10
        );

        assertNotNull(prices);
        assertFalse(prices.isEmpty());
        assertEquals("mkt-guntur-mandi", prices.get(0).getMarketId());
        assertEquals("crop-chilli", prices.get(0).getCropId());
        assertNotEquals("REJECTED", prices.get(0).getQualityStatus());
    }

    @Test
    void getMarketPrices_convertsPricesToKg() {
        List<MarketPriceSummaryResponse> prices = marketPriceService.getMarketPrices(
                "mkt-guntur-mandi", "crop-chilli", null, null, null, null, "KG", null, null, 10
        );

        assertNotNull(prices);
        assertFalse(prices.isEmpty());
        MarketPriceSummaryResponse p = prices.get(0);
        assertEquals("KG", p.getUnit());
        assertEquals("QUINTAL", p.getSourceUnit());
        assertTrue(p.isConversionApplied());
        assertEquals(205.0, p.getModalPrice()); // 20500 / 100 = 205
    }

    @Test
    void getMarketPrices_convertsPricesToTonne() {
        List<MarketPriceSummaryResponse> prices = marketPriceService.getMarketPrices(
                "mkt-guntur-mandi", "crop-chilli", null, null, null, null, "TONNE", null, null, 10
        );

        assertNotNull(prices);
        assertFalse(prices.isEmpty());
        MarketPriceSummaryResponse p = prices.get(0);
        assertEquals("TONNE", p.getUnit());
        assertEquals("QUINTAL", p.getSourceUnit());
        assertTrue(p.isConversionApplied());
        assertEquals(205000.0, p.getModalPrice()); // 20500 * 10 = 205000
    }

    @Test
    void getLatestMarketPrice_returnsMostRecentVerifiedPrice_whenValid() {
        MarketResponse dummyMarket = new MarketResponse(
                "mkt-guntur-mandi", "Guntur Mandi", "GNT-001", "MANDI",
                new LocationDto("Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
                16.29, 80.43, "ACTIVE", "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
        );
        CropResponse dummyCrop = new CropResponse(
                "crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"
        );

        when(marketService.getMarketById("mkt-guntur-mandi")).thenReturn(dummyMarket);
        when(cropMasterService.getCropById("crop-chilli")).thenReturn(dummyCrop);

        MarketPriceResponse latest = marketPriceService.getLatestMarketPrice("mkt-guntur-mandi", "crop-chilli");

        assertNotNull(latest);
        assertEquals("mkt-guntur-mandi", latest.getMarket().getId());
        assertEquals("crop-chilli", latest.getCrop().getId());
        assertEquals("2026-08-26", latest.getPriceDate());
    }

    @Test
    void getLatestMarketPrice_throwsNoSuchElementException_whenNoPricesExist() {
        MarketResponse dummyMarket = new MarketResponse(
                "mkt-guntur-mandi", "Guntur Mandi", "GNT-001", "MANDI",
                new LocationDto("Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
                16.29, 80.43, "ACTIVE", "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
        );
        CropResponse dummyCrop = new CropResponse(
                "crop-unknown", "Unknown Crop", "OTHER", "Unknown", "ACTIVE"
        );

        when(marketService.getMarketById("mkt-guntur-mandi")).thenReturn(dummyMarket);
        when(cropMasterService.getCropById("crop-unknown")).thenReturn(dummyCrop);

        assertThrows(NoSuchElementException.class, () ->
                marketPriceService.getLatestMarketPrice("mkt-guntur-mandi", "crop-unknown")
        );
    }

    @Test
    void getMarketPriceHistory_returnsDeterministicOrderedList() {
        MarketResponse dummyMarket = new MarketResponse(
                "mkt-guntur-mandi", "Guntur Mandi", "GNT-001", "MANDI",
                new LocationDto("Andhra Pradesh", "Guntur", "Guntur West", "Pattabhipuram", "522006"),
                16.29, 80.43, "ACTIVE", "2026-08-26T00:00:00Z", "2026-08-26T00:00:00Z"
        );
        CropResponse dummyCrop = new CropResponse(
                "crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"
        );

        when(marketService.getMarketById("mkt-guntur-mandi")).thenReturn(dummyMarket);
        when(cropMasterService.getCropById("crop-chilli")).thenReturn(dummyCrop);

        List<MarketPriceResponse> history = marketPriceService.getMarketPriceHistory("mkt-guntur-mandi", "crop-chilli", null, null);

        assertNotNull(history);
        assertTrue(history.size() >= 2);
        // Newest date first
        assertTrue(history.get(0).getPriceDate().compareTo(history.get(1).getPriceDate()) >= 0);
    }

    @Test
    void getMarketPriceById_returnsPriceRecord_whenFound() {
        MarketPriceResponse p = marketPriceService.getMarketPriceById("prc-gnt-chilli-20260826");
        assertNotNull(p);
        assertEquals("prc-gnt-chilli-20260826", p.getId());
        assertEquals("Development Reference Seed Data", p.getSource().getName());
    }

    @Test
    void getMarketPriceById_throwsNoSuchElementException_whenNotFound() {
        assertThrows(NoSuchElementException.class, () ->
                marketPriceService.getMarketPriceById("prc-nonexistent-999")
        );
    }
}
