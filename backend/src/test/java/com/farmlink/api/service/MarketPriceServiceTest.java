package com.farmlink.api.service;

import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.dto.MarketPriceSummaryResponse;
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
        firestore = null; // Uninitialized Firestore simulation
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
    void getMarketPrices_returnsEmptyList_whenFirestoreEmpty() {
        List<MarketPriceSummaryResponse> prices = marketPriceService.getMarketPrices(
                "mkt-guntur-mandi", "crop-chilli", null, null, null, null, null, null, null, 10
        );

        assertNotNull(prices);
        assertTrue(prices.isEmpty(), "Module 10B: Must return empty list when Firestore is empty without default fallback prices");
    }

    @Test
    void getLatestMarketPrice_throwsNoSuchElementException_whenNoPricesExist() {
        assertThrows(NoSuchElementException.class, () ->
                marketPriceService.getLatestMarketPrice("mkt-guntur-mandi", "crop-chilli")
        );
    }

    @Test
    void getMarketPriceById_throwsNoSuchElementException_whenNotFound() {
        assertThrows(NoSuchElementException.class, () ->
                marketPriceService.getMarketPriceById("prc-nonexistent-999")
        );
    }
}
