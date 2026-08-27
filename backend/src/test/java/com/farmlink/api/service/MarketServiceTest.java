package com.farmlink.api.service;

import com.farmlink.api.dto.MarketCropResponse;
import com.farmlink.api.dto.MarketResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class MarketServiceTest {

    private Firestore firestore;
    private CropMasterService cropMasterService;
    private MarketService marketService;

    @BeforeEach
    void setUp() {
        firestore = null; // Uninitialized Firestore simulation
        cropMasterService = Mockito.mock(CropMasterService.class);
        marketService = new MarketService(firestore, cropMasterService);
    }

    @Test
    void getMarkets_returnsEmptyList_whenFirestoreEmpty() {
        List<MarketSummaryResponse> markets = marketService.getMarkets(null, null, null, null, null, null, null);
        assertNotNull(markets);
        assertTrue(markets.isEmpty(), "Module 10B: Must return empty list when Firestore is empty without default fallbacks");
    }

    @Test
    void getMarketById_throwsNoSuchElement_whenIdUnknown() {
        assertThrows(NoSuchElementException.class, () -> marketService.getMarketById("mkt-unknown-999"));
    }

    @Test
    void getMarketCrops_returnsEmptyList_whenFirestoreEmpty() {
        List<MarketCropResponse> crops = marketService.getMarketCrops("mkt-guntur-mandi");
        assertNotNull(crops);
        assertTrue(crops.isEmpty(), "Module 10B: Must return empty list when market crops are not found in Firestore");
    }
}
