package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.LocationDto;
import com.farmlink.api.dto.MarketSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MandiMappingServiceTest {

    private MarketService marketService;
    private CropMasterService cropMasterService;
    private MandiMappingService mappingService;

    @BeforeEach
    void setUp() {
        marketService = mock(MarketService.class);
        cropMasterService = mock(CropMasterService.class);

        MarketSummaryResponse guntur = new MarketSummaryResponse(
                "mkt-guntur-mandi", "Guntur Agricultural Market", "GNT-MND-001", "MANDI",
                "Andhra Pradesh", "Guntur", "Guntur West", "ACTIVE", 4
        );
        when(marketService.getMarkets(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(guntur));

        when(cropMasterService.getCropById("crop-chilli"))
                .thenReturn(new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE"));
        when(cropMasterService.getAllCrops())
                .thenReturn(List.of(new CropResponse("crop-chilli", "Red Chilli", "SPICE", "Capsicum annuum", "ACTIVE")));

        mappingService = new MandiMappingService(marketService, cropMasterService);
    }

    @Test
    void mapMarket_mapsKnownMarketSuccessfully() {
        Optional<MarketSummaryResponse> m = mappingService.mapMarket("Andhra Pradesh", "Guntur", "Guntur Mandi");
        assertTrue(m.isPresent());
        assertEquals("mkt-guntur-mandi", m.get().getId());
    }

    @Test
    void mapMarket_returnsEmpty_whenMarketUnknown() {
        Optional<MarketSummaryResponse> m = mappingService.mapMarket("Unknown State", "Unknown District", "Random Unmapped Mandi 999");
        assertTrue(m.isEmpty());
    }

    @Test
    void mapCrop_mapsKnownCommoditySuccessfully() {
        Optional<CropResponse> c = mappingService.mapCrop("Chilli Red");
        assertTrue(c.isPresent());
        assertEquals("crop-chilli", c.get().getId());
    }

    @Test
    void mapCrop_returnsEmpty_whenCommodityUnknown() {
        Optional<CropResponse> c = mappingService.mapCrop("Exotic Unmapped Plant X");
        assertTrue(c.isEmpty());
    }
}
