package com.farmlink.api.controller;

import com.farmlink.api.dto.forecast.ForecastRequest;
import com.farmlink.api.dto.forecast.ForecastResponse;
import com.farmlink.api.dto.forecast.ForecastScenarioProfitabilityResponse;
import com.farmlink.api.security.FirebaseAuthenticationToken;
import com.farmlink.api.service.FarmService;
import com.farmlink.api.service.forecast.ForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ForecastController {

    private static final Logger log = LoggerFactory.getLogger(ForecastController.class);

    private final ForecastService forecastService;
    private final FarmService farmService;

    public ForecastController(ForecastService forecastService, FarmService farmService) {
        this.forecastService = forecastService;
        this.farmService = farmService;
    }

    @GetMapping("/market-intelligence/forecast")
    public ResponseEntity<ForecastResponse> getPriceForecast(
            @RequestParam(required = true) String cropId,
            @RequestParam(required = true) String marketId,
            @RequestParam(required = false, defaultValue = "7_DAYS") String horizon,
            @RequestParam(required = false, defaultValue = "30") Integer lookbackDays,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String model
    ) {
        log.info("REST request for price forecast: cropId={}, marketId={}, horizon={}, lookbackDays={}",
                cropId, marketId, horizon, lookbackDays);

        ForecastRequest req = new ForecastRequest(cropId, marketId, horizon, lookbackDays, unit);
        req.setModel(model);

        ForecastResponse response = forecastService.getPriceForecast(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/farm-economics/{id}/forecast-scenario")
    public ResponseEntity<ForecastScenarioProfitabilityResponse> evaluateForecastScenario(
            FirebaseAuthenticationToken token,
            @PathVariable("id") String id,
            @RequestParam(required = false, defaultValue = "7_DAYS") String horizon
    ) {
        if (token == null || token.getUid() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String farmerUid = token.getUid();
        farmService.verifyFarmerRole(farmerUid);

        log.info("REST request for forecast scenario profitability: farmerUid={}, recordId={}, horizon={}",
                farmerUid, id, horizon);

        ForecastScenarioProfitabilityResponse response = forecastService.evaluateScenarioProfitability(
                farmerUid, id, horizon
        );
        return ResponseEntity.ok(response);
    }
}
