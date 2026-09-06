package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.forecast.*;
import com.farmlink.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class ForecastServiceTest {

    private MarketPriceService marketPriceService;
    private CropMasterService cropMasterService;
    private MarketService marketService;
    private ForecastDataPreparerService dataPreparerService;
    private WeightedMovingAverageForecastModel wmaModel;
    private SimpleMovingAverageForecastModel smaModel;
    private ForecastBacktestService backtestService;
    private ForecastConfidenceService confidenceService;
    private FarmEconomicsService farmEconomicsService;

    private ForecastService forecastService;

    @BeforeEach
    void setUp() {
        marketPriceService = Mockito.mock(MarketPriceService.class);
        cropMasterService = Mockito.mock(CropMasterService.class);
        marketService = Mockito.mock(MarketService.class);
        farmEconomicsService = Mockito.mock(FarmEconomicsService.class);

        dataPreparerService = new ForecastDataPreparerService();
        wmaModel = new WeightedMovingAverageForecastModel();
        smaModel = new SimpleMovingAverageForecastModel();
        backtestService = new ForecastBacktestService();
        confidenceService = new ForecastConfidenceService();

        forecastService = new ForecastService(
                marketPriceService,
                cropMasterService,
                marketService,
                dataPreparerService,
                wmaModel,
                smaModel,
                backtestService,
                confidenceService,
                farmEconomicsService
        );
    }

    @Test
    void testGetPriceForecast_ValidObservations_ReturnsForecastResponse() {
        List<MarketPriceSummaryResponse> mockPrices = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            MarketPriceSummaryResponse p = new MarketPriceSummaryResponse();
            p.setCropId("crop-1");
            p.setMarketId("mkt-1");
            p.setPriceDate(String.format("2026-09-%02d", i));
            p.setModalPrice(2400.0);
            p.setMinPrice(2300.0);
            p.setMaxPrice(2500.0);
            p.setQualityStatus("VERIFIED");
            mockPrices.add(p);
        }

        when(marketPriceService.getMarketPrices(anyString(), anyString(), any(), any(), any(), anyString(), anyString(), any(), any(), anyInt()))
                .thenReturn(mockPrices);

        CropResponse cropResp = new CropResponse();
        cropResp.setId("crop-1");
        cropResp.setName("Tomato");
        when(cropMasterService.getCropById("crop-1")).thenReturn(cropResp);

        MarketResponse mktResp = new MarketResponse();
        mktResp.setId("mkt-1");
        mktResp.setName("Kolar Mandi");
        when(marketService.getMarketById("mkt-1")).thenReturn(mktResp);

        ForecastRequest req = new ForecastRequest("crop-1", "mkt-1", "7_DAYS", 30, "QUINTAL");
        ForecastResponse resp = forecastService.getPriceForecast(req);

        assertNotNull(resp);
        assertEquals("crop-1", resp.getCropId());
        assertEquals("Tomato", resp.getCropName());
        assertEquals("mkt-1", resp.getMarketId());
        assertEquals("Kolar Mandi", resp.getMarketName());
        assertNotNull(resp.getForecastPrice());
        assertEquals(ForecastModelType.WEIGHTED_MOVING_AVERAGE_V1.getCode(), resp.getModel());
        assertNotNull(resp.getDisclaimer());
    }

    @Test
    void testGetPriceForecast_MissingRequiredParams_ThrowsException() {
        ForecastRequest req = new ForecastRequest(null, "mkt-1", "7_DAYS", 30, "QUINTAL");
        assertThrows(IllegalArgumentException.class, () -> forecastService.getPriceForecast(req));
    }
}
