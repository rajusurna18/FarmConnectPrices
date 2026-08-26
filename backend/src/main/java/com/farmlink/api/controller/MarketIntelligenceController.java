package com.farmlink.api.controller;

import com.farmlink.api.dto.MarketComparisonResponse;
import com.farmlink.api.dto.MarketIntelligenceSummaryResponse;
import com.farmlink.api.dto.PriceTrendResponse;
import com.farmlink.api.service.MarketIntelligenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market-intelligence")
public class MarketIntelligenceController {

    private final MarketIntelligenceService intelligenceService;

    public MarketIntelligenceController(MarketIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/compare")
    public ResponseEntity<MarketComparisonResponse> compareMarkets(
            @RequestParam(value = "cropId", required = false) String cropId,
            @RequestParam(value = "crop", required = false) String cropAlias,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "unit", required = false) String unit
    ) {
        String effectiveCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : cropAlias;
        MarketComparisonResponse response = intelligenceService.compareMarkets(
                effectiveCropId, date, fromDate, toDate, state, district, unit
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<MarketIntelligenceSummaryResponse> getSummary(
            @RequestParam(value = "cropId", required = false) String cropId,
            @RequestParam(value = "crop", required = false) String cropAlias,
            @RequestParam(value = "marketId", required = false) String marketId,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "unit", required = false) String unit
    ) {
        String effectiveCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : cropAlias;
        MarketIntelligenceSummaryResponse summary = intelligenceService.getSummary(
                effectiveCropId, marketId, fromDate, toDate, state, district, unit
        );
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/trends")
    public ResponseEntity<PriceTrendResponse> getTrends(
            @RequestParam(value = "cropId", required = false) String cropId,
            @RequestParam(value = "crop", required = false) String cropAlias,
            @RequestParam(value = "marketId", required = false) String marketId,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "unit", required = false) String unit
    ) {
        String effectiveCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : cropAlias;
        PriceTrendResponse trend = intelligenceService.getTrends(
                effectiveCropId, marketId, fromDate, toDate, unit
        );
        return ResponseEntity.ok(trend);
    }
}
