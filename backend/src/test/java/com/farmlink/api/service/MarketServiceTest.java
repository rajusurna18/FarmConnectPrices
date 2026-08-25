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
        firestore = Mockito.mock(Firestore.class);
        cropMasterService = Mockito.mock(CropMasterService.class);
        Mockito.when(cropMasterService.getCropById("crop-chilli"))
                .thenReturn(new com.farmlink.api.dto.CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"));
        Mockito.when(cropMasterService.getCropById("crop-paddy"))
                .thenReturn(new com.farmlink.api.dto.CropResponse("crop-paddy", "Rice", "CEREAL", "Oryza sativa", "ACTIVE"));
        marketService = new MarketService(firestore, cropMasterService);
    }

    @Test
    void getMarkets_returnsDefaultSeedMarkets_whenFirestoreEmpty() {
        List<MarketSummaryResponse> markets = marketService.getMarkets(null, null, null, null, null, null, null);
        assertNotNull(markets);
        assertFalse(markets.isEmpty());
        assertEquals(4, markets.size());
    }

    @Test
    void getMarkets_filtersByState_correctly() {
        List<MarketSummaryResponse> apMarkets = marketService.getMarkets("Andhra Pradesh", null, null, null, null, null, null);
        assertEquals(1, apMarkets.size());
        assertEquals("Guntur Agricultural Market", apMarkets.get(0).getName());
    }

    @Test
    void getMarkets_filtersByDistrict_correctly() {
        List<MarketSummaryResponse> wglMarkets = marketService.getMarkets(null, "Warangal", null, null, null, null, null);
        assertEquals(1, wglMarkets.size());
        assertEquals("WGL-MND-002", wglMarkets.get(0).getCode());
    }

    @Test
    void getMarkets_filtersByType_correctly() {
        List<MarketSummaryResponse> rythuBazaars = marketService.getMarkets(null, null, null, "RYTHU_BAZAAR", null, null, null);
        assertEquals(1, rythuBazaars.size());
        assertEquals("Devanahalli Rythu Bazaar", rythuBazaars.get(0).getName());
    }

    @Test
    void getMarketById_returnsMarket_whenIdValid() {
        MarketResponse m = marketService.getMarketById("mkt-guntur-mandi");
        assertNotNull(m);
        assertEquals("GNT-MND-001", m.getCode());
        assertEquals("Andhra Pradesh", m.getLocation().getState());
    }

    @Test
    void getMarketById_throwsNoSuchElement_whenIdUnknown() {
        assertThrows(NoSuchElementException.class, () -> marketService.getMarketById("mkt-unknown-999"));
    }

    @Test
    void getMarketCrops_returnsSeedMappings_whenFirestoreEmpty() {
        List<MarketCropResponse> crops = marketService.getMarketCrops("mkt-guntur-mandi");
        assertNotNull(crops);
        assertFalse(crops.isEmpty());
    }
}
