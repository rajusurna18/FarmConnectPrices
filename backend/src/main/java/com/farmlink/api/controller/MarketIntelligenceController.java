package com.farmlink.api.controller;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.ai.AiDecisionRequest;

import com.farmlink.api.dto.ai.AiDecisionResponse;
import com.farmlink.api.dto.ai.AiDecisionType;
import com.farmlink.api.service.MarketIntelligenceService;
import com.farmlink.api.service.MarketTrendService;
import com.farmlink.api.service.ai.AiDecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-intelligence")
public class MarketIntelligenceController {

    private final MarketIntelligenceService intelligenceService;
    private final MarketTrendService trendService;
    private final AiDecisionService aiDecisionService;

    public MarketIntelligenceController(
            MarketIntelligenceService intelligenceService,
            MarketTrendService trendService,
            @Autowired(required = false) AiDecisionService aiDecisionService
    ) {
        this.intelligenceService = intelligenceService;
        this.trendService = trendService;
        this.aiDecisionService = aiDecisionService;
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
    public ResponseEntity<MarketTrendResponse> getTrends(
            @RequestParam(value = "cropId", required = false) String cropId,
            @RequestParam(value = "crop", required = false) String cropAlias,
            @RequestParam(value = "marketId", required = false) String marketId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "includeAiExplanation", required = false, defaultValue = "false") boolean includeAiExplanation,
            Authentication authentication
    ) {
        String effectiveCropId = (cropId != null && !cropId.trim().isEmpty()) ? cropId.trim() : cropAlias;
        String effectiveStartDate = (startDate != null && !startDate.trim().isEmpty()) ? startDate.trim() : fromDate;
        String effectiveEndDate = (endDate != null && !endDate.trim().isEmpty()) ? endDate.trim() : toDate;

        MarketTrendResponse trend = trendService.calculateTrend(
                effectiveCropId, marketId, period, effectiveStartDate, effectiveEndDate, unit
        );

        if (includeAiExplanation && aiDecisionService != null) {
            try {
                String farmerUid = (authentication != null) ? authentication.getName() : "anonymous";
                AiDecisionRequest aiReq = new AiDecisionRequest();
                aiReq.setDecisionType(AiDecisionType.MARKET_TREND_EXPLANATION);
                aiReq.setCropId(effectiveCropId);
                if (marketId != null && !marketId.trim().isEmpty()) {
                    aiReq.setMarketIds(List.of(marketId.trim()));
                }
                AiDecisionResponse explanation = aiDecisionService.processDecisionRequest(farmerUid, aiReq);
                trend.setAiExplanation(explanation);
            } catch (Exception e) {
                // If AI execution fails or user is not authorized for farmer AI support, proceed with deterministic response
            }
        }

        return ResponseEntity.ok(trend);
    }
}
