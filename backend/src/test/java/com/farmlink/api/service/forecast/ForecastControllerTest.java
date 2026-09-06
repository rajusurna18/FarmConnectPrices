package com.farmlink.api.service.forecast;

import com.farmlink.api.controller.ForecastController;
import com.farmlink.api.dto.forecast.ForecastRequest;
import com.farmlink.api.dto.forecast.ForecastResponse;
import com.farmlink.api.dto.forecast.ForecastScenarioProfitabilityResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class ForecastControllerTest {

    private ForecastService forecastService;
    private FarmService farmService;
    private ForecastController controller;

    @BeforeEach
    void setUp() {
        forecastService = Mockito.mock(ForecastService.class);
        farmService = Mockito.mock(FarmService.class);
        controller = new ForecastController(forecastService, farmService);
    }

    @Test
    void testGetPriceForecast_Returns200Ok() {
        ForecastResponse mockResp = new ForecastResponse();
        mockResp.setCropId("crop-1");
        mockResp.setMarketId("mkt-1");
        mockResp.setForecastPrice(2475.0);

        Mockito.when(forecastService.getPriceForecast(any(ForecastRequest.class))).thenReturn(mockResp);

        ResponseEntity<ForecastResponse> response = controller.getPriceForecast("crop-1", "mkt-1", "7_DAYS", 30, "QUINTAL", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2475.0, response.getBody().getForecastPrice());
    }

    @Test
    void testEvaluateForecastScenario_Returns200Ok() {
        FirebaseAuthenticationToken token = Mockito.mock(FirebaseAuthenticationToken.class);
        Mockito.when(token.getUid()).thenReturn("farmer-uid-1");

        ForecastScenarioProfitabilityResponse mockResp = new ForecastScenarioProfitabilityResponse();
        mockResp.setFarmEconomicRecordId("econ-1");
        mockResp.setCurrentPriceUsed(2400.0);
        mockResp.setForecastPriceUsed(2475.0);

        Mockito.when(forecastService.evaluateScenarioProfitability("farmer-uid-1", "econ-1", "7_DAYS"))
                .thenReturn(mockResp);

        ResponseEntity<ForecastScenarioProfitabilityResponse> response = controller.evaluateForecastScenario(token, "econ-1", "7_DAYS");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("econ-1", response.getBody().getFarmEconomicRecordId());
    }
}
