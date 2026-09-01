package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FarmEconomicsServiceTest {

    private FarmService farmService;
    private CropMasterService cropMasterService;
    private MarketService marketService;
    private MarketPriceService marketPriceService;
    private FarmEconomicsCalculationService calculationService;
    private FarmEconomicsService farmEconomicsService;

    private static final String FARMER_UID = "farmer-uid-1";

    @BeforeEach
    void setUp() {
        farmService = mock(FarmService.class);
        cropMasterService = mock(CropMasterService.class);
        marketService = mock(MarketService.class);
        marketPriceService = mock(MarketPriceService.class);
        calculationService = new FarmEconomicsCalculationService();

        farmEconomicsService = new FarmEconomicsService(
                null, // test with in-memory mode
                farmService,
                cropMasterService,
                marketService,
                marketPriceService,
                calculationService
        );
    }

    @Test
    void testCreateAndGetEconomicRecord() {
        doNothing().when(farmService).verifyFarmerRole(FARMER_UID);

        FarmResponse farm = new FarmResponse("farm-1", FARMER_UID, "Green Acres", new LocationDto("AP", "Guntur", "Mandal", "Village"), 5.0, "ACRE", "ACTIVE", "", "");
        when(farmService.getFarmById("farm-1", FARMER_UID)).thenReturn(farm);

        CropResponse crop = new CropResponse("crop-1", "Rice (Paddy)", "CEREAL", "Oryza sativa", "ACTIVE");
        when(cropMasterService.getCropById("crop-1")).thenReturn(crop);

        FarmEconomicRecordRequest req = new FarmEconomicRecordRequest();
        req.setFarmId("farm-1");
        req.setCropId("crop-1");
        req.setSeason("KHARIF");
        req.setCultivatedArea(BigDecimal.valueOf(5));
        req.setCultivatedAreaUnit("ACRE");
        req.setExpectedYield(BigDecimal.valueOf(10));
        req.setYieldUnit("QUINTAL");
        req.setProductionCosts(List.of(
                new ProductionCostItemDto("SEEDS", "Rice seed", 2500.0),
                new ProductionCostItemDto("FERTILIZER", "Urea", 4000.0)
        ));
        req.setSellingCosts(new SellingCostsDto(300.0, 100.0));

        FarmEconomicRecordResponse created = farmEconomicsService.createEconomicRecord(FARMER_UID, req);

        assertNotNull(created.getId());
        assertEquals("farm-1", created.getFarmId());
        assertEquals("crop-1", created.getCropId());
        assertEquals(new BigDecimal("6500.00"), created.getTotalProductionCost());
        assertEquals(new BigDecimal("400.00"), created.getTotalSellingCost());
        assertEquals(new BigDecimal("6900.00"), created.getTotalCost());

        List<FarmEconomicRecordResponse> list = farmEconomicsService.getEconomicRecordsForOwner(FARMER_UID);
        assertEquals(1, list.size());
        assertEquals(created.getId(), list.get(0).getId());
    }

    @Test
    void testGetRecordAccessDeniedDifferentOwner() {
        doNothing().when(farmService).verifyFarmerRole("farmer-uid-2");

        FarmResponse farm = new FarmResponse("farm-1", FARMER_UID, "Green Acres", new LocationDto("AP", "Guntur", "Mandal", "Village"), 5.0, "ACRE", "ACTIVE", "", "");
        when(farmService.getFarmById("farm-1", FARMER_UID)).thenReturn(farm);

        CropResponse crop = new CropResponse("crop-1", "Rice (Paddy)", "CEREAL", "Oryza sativa", "ACTIVE");
        when(cropMasterService.getCropById("crop-1")).thenReturn(crop);

        FarmEconomicRecordRequest req = new FarmEconomicRecordRequest();
        req.setFarmId("farm-1");
        req.setCropId("crop-1");
        req.setSeason("KHARIF");
        req.setCultivatedArea(BigDecimal.valueOf(5));
        req.setCultivatedAreaUnit("ACRE");
        req.setExpectedYield(BigDecimal.valueOf(10));
        req.setYieldUnit("QUINTAL");

        FarmEconomicRecordResponse created = farmEconomicsService.createEconomicRecord(FARMER_UID, req);

        assertThrows(AccessDeniedException.class, () -> farmEconomicsService.getEconomicRecordById(created.getId(), "farmer-uid-2"));
    }
}
