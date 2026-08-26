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
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "unit", required = false) String unit
    ) {
        MarketComparisonResponse response = intelligenceService.compareMarkets(
                cropId, date, fromDate, toDate, state, district, unit
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<MarketIntelligenceSummaryResponse> getSummary(
            @RequestParam(value = "cropId", required = false) String cropId,
            @RequestParam(value = "marketId", required = false) String marketId,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "unit", required = false) String unit
    ) {
        MarketIntelligenceSummaryResponse summary = intelligenceService.getSummary(
                cropId, marketId, fromDate, toDate, state, district, unit
        );
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/trends")
    public ResponseEntity<PriceTrendResponse> getTrends(
            @RequestParam(value = "cropId", required = false) String cropId,
            @RequestParam(value = "marketId", required = false) String marketId,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "unit", required = false) String unit
    ) {
        PriceTrendResponse trend = intelligenceService.getTrends(
                cropId, marketId, fromDate, toDate, unit
        );
        return ResponseEntity.ok(trend);
    }
}
