package com.farmlink.api.service.ai;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.ai.AiDecisionContext;
import com.farmlink.api.dto.ai.AiDecisionRequest;
import com.farmlink.api.dto.ai.AiDecisionType;
import com.farmlink.api.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class AiDecisionContextBuilder {
    private static final Logger log = LoggerFactory.getLogger(AiDecisionContextBuilder.class);

    private final FarmService farmService;
    private final CropMasterService cropMasterService;
    private final MarketService marketService;
    private final MarketPriceService marketPriceService;
    private final DecisionSupportService decisionSupportService;
    private final FarmEconomicsService farmEconomicsService;
    private final MarketIntelligenceService marketIntelligenceService;
    private final MarketTrendService marketTrendService;
    private final com.farmlink.api.service.forecast.ForecastService forecastService;

    public AiDecisionContextBuilder(FarmService farmService,
                                    CropMasterService cropMasterService,
                                    MarketService marketService,
                                    MarketPriceService marketPriceService,
                                    DecisionSupportService decisionSupportService,
                                    FarmEconomicsService farmEconomicsService,
                                    MarketIntelligenceService marketIntelligenceService) {
        this(farmService, cropMasterService, marketService, marketPriceService, decisionSupportService, farmEconomicsService, marketIntelligenceService,
                new MarketTrendService(marketPriceService, marketService, cropMasterService, new PriceUnitConversionService()), null);
    }

    public AiDecisionContextBuilder(FarmService farmService,
                                    CropMasterService cropMasterService,
                                    MarketService marketService,
                                    MarketPriceService marketPriceService,
                                    DecisionSupportService decisionSupportService,
                                    FarmEconomicsService farmEconomicsService,
                                    MarketIntelligenceService marketIntelligenceService,
                                    MarketTrendService marketTrendService) {
        this(farmService, cropMasterService, marketService, marketPriceService, decisionSupportService, farmEconomicsService, marketIntelligenceService, marketTrendService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiDecisionContextBuilder(FarmService farmService,
                                    CropMasterService cropMasterService,
                                    MarketService marketService,
                                    MarketPriceService marketPriceService,
                                    DecisionSupportService decisionSupportService,
                                    FarmEconomicsService farmEconomicsService,
                                    MarketIntelligenceService marketIntelligenceService,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) MarketTrendService marketTrendService,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) com.farmlink.api.service.forecast.ForecastService forecastService) {
        this.farmService = farmService;
        this.cropMasterService = cropMasterService;
        this.marketService = marketService;
        this.marketPriceService = marketPriceService;
        this.decisionSupportService = decisionSupportService;
        this.farmEconomicsService = farmEconomicsService;
        this.marketIntelligenceService = marketIntelligenceService;
        this.marketTrendService = (marketTrendService != null) ? marketTrendService :
                new MarketTrendService(marketPriceService, marketService, cropMasterService, new PriceUnitConversionService());
        this.forecastService = forecastService;
    }

    public AiDecisionContext buildContext(String farmerUid, AiDecisionRequest request) {
        // Enforce role authorization
        farmService.verifyFarmerRole(farmerUid);

        AiDecisionContext context = new AiDecisionContext();
        context.setFarmerUid(farmerUid);
        context.setQuantity(request.getQuantity());
        context.setQuantityUnit(request.getQuantityUnit());
        context.setTransportationCost(request.getTransportationCost());
        context.setOtherSellingCosts(request.getOtherSellingCosts());

        List<String> dataGaps = new ArrayList<>();

        // 1. Economic Record Lookup (if provided)
        if (request.getEconomicRecordId() != null && !request.getEconomicRecordId().isBlank()) {
            try {
                FarmEconomicRecordResponse record = farmEconomicsService.getEconomicRecordById(farmerUid, request.getEconomicRecordId());
                context.setEconomicRecord(record);
                if (request.getCropId() == null || request.getCropId().isBlank()) {
                    request.setCropId(record.getCropId());
                }
                if (request.getFarmId() == null || request.getFarmId().isBlank()) {
                    request.setFarmId(record.getFarmId());
                }
                if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    context.setQuantity(record.getExpectedYield());
                    context.setQuantityUnit(record.getYieldUnit());
                }
            } catch (Exception e) {
                log.warn("Could not fetch economic record {}: {}", request.getEconomicRecordId(), e.getMessage());
                dataGaps.add("Specified economic record could not be loaded: " + e.getMessage());
            }
        }

        // 2. Farm Lookup (if provided)
        if (request.getFarmId() != null && !request.getFarmId().isBlank()) {
            try {
                FarmResponse farm = farmService.getFarmById(farmerUid, request.getFarmId());
                context.setFarm(farm);
            } catch (Exception e) {
                log.warn("Could not fetch farm {}: {}", request.getFarmId(), e.getMessage());
                dataGaps.add("Farm details unavailable: " + e.getMessage());
            }
        }

        // 3. Crop Lookup (if provided)
        if (request.getCropId() != null && !request.getCropId().isBlank()) {
            try {
                CropResponse crop = cropMasterService.getCropById(request.getCropId());
                context.setCrop(crop);
            } catch (Exception e) {
                log.warn("Could not fetch crop {}: {}", request.getCropId(), e.getMessage());
                dataGaps.add("Crop details unavailable: " + e.getMessage());
            }
        } else {
            dataGaps.add("No crop specified for decision evaluation.");
        }

        // 4. Markets Lookup
        List<MarketResponse> candidateMarkets = new ArrayList<>();
        if (request.getMarketIds() != null && !request.getMarketIds().isEmpty()) {
            for (String marketId : request.getMarketIds()) {
                if (marketId != null && !marketId.isBlank()) {
                    try {
                        MarketResponse market = marketService.getMarketById(marketId);
                        candidateMarkets.add(market);
                    } catch (Exception e) {
                        log.warn("Could not fetch market {}: {}", marketId, e.getMessage());
                    }
                }
            }
        }
        context.setCandidateMarkets(candidateMarkets);

        // 5. Evaluate Single / Multiple Markets via Trusted Services (Module 09)
        if (context.getCrop() != null && !candidateMarkets.isEmpty()) {
            List<MarketEvaluationResponse> evaluations = new ArrayList<>();
            for (MarketResponse market : candidateMarkets) {
                if (market == null || market.getId() == null) continue;
                try {
                    MarketEvaluationRequest evalReq = new MarketEvaluationRequest();
                    evalReq.setCropId(context.getCrop().getId());
                    evalReq.setMarketId(market.getId());
                    evalReq.setQuantity(context.getQuantity() != null && context.getQuantity().compareTo(BigDecimal.ZERO) > 0 ? context.getQuantity().doubleValue() : 10.0);
                    evalReq.setQuantityUnit(context.getQuantityUnit() != null ? context.getQuantityUnit() : "QUINTAL");
                    evalReq.setPriceBasis(request.getPriceBasis() != null ? request.getPriceBasis() : "MODAL");
                    evalReq.setPriceMode(request.getPriceMode() != null ? request.getPriceMode() : "LATEST_AVAILABLE");
                    evalReq.setDate(request.getDate());
                    evalReq.setTransportationCost(request.getTransportationCost() != null ? request.getTransportationCost().doubleValue() : 0.0);
                    evalReq.setOtherSellingCosts(request.getOtherSellingCosts() != null ? request.getOtherSellingCosts().doubleValue() : 0.0);

                    MarketEvaluationResponse evalRes = decisionSupportService.evaluateMarket(evalReq);
                    if (evalRes != null) {
                        evaluations.add(evalRes);
                        if ("SUCCESS".equalsIgnoreCase(evalRes.getStatus())) {
                            context.setHasVerifiedPrice(true);
                            if (evalRes.isStalePrice()) {
                                context.setHasStalePrice(true);
                            }
                            if (evalRes.getPriceDate() != null) {
                                context.setPriceDate(evalRes.getPriceDate());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error evaluating market {}: {}", market.getId(), e.getMessage());
                }
            }
            context.setMarketEvaluations(evaluations);

            // Fetch Module 08 Market Intelligence
            if (context.getCrop() != null && !candidateMarkets.isEmpty()) {
                try {
                    String primaryMktId = candidateMarkets.get(0).getId();
                    MarketIntelligenceSummaryResponse intelSummary = marketIntelligenceService.getSummary(
                            context.getCrop().getId(),
                            primaryMktId,
                            null, null, null, null,
                            context.getQuantityUnit()
                    );
                    context.setMarketIntelligence(intelSummary);
                } catch (Exception e) {
                    log.warn("Could not fetch Market Intelligence summary: {}", e.getMessage());
                }
            }
        }

        // 6. Evaluate Farm Profitability via FarmEconomicsService (Module 10)
        if (context.getEconomicRecord() != null) {
            try {
                FarmProfitabilityEvaluationRequest profReq = new FarmProfitabilityEvaluationRequest();
                profReq.setEconomicRecordId(context.getEconomicRecord().getId());
                if (!candidateMarkets.isEmpty()) {
                    profReq.setMarketId(candidateMarkets.get(0).getId());
                }
                profReq.setPriceBasis(request.getPriceBasis());
                profReq.setPriceMode(request.getPriceMode());
                profReq.setDate(request.getDate());

                FarmProfitabilityEvaluationResponse profRes = farmEconomicsService.evaluateProfitability(farmerUid, profReq);
                context.setProfitabilityEvaluation(profRes);
            } catch (Exception e) {
                log.warn("Could not evaluate profitability for record {}: {}", context.getEconomicRecord().getId(), e.getMessage());
                dataGaps.add("Farm profitability evaluation unavailable: " + e.getMessage());
            }

            // Also perform market comparison if multiple markets provided
            if (request.getMarketIds() != null && request.getMarketIds().size() > 1) {
                try {
                    FarmMarketComparisonRequest compReq = new FarmMarketComparisonRequest();
                    compReq.setEconomicRecordId(context.getEconomicRecord().getId());
                    List<MarketCostInputDto> marketInputs = new ArrayList<>();
                    for (String mktId : request.getMarketIds()) {
                        Double trans = request.getTransportationCost() != null ? request.getTransportationCost().doubleValue() : 0.0;
                        Double other = request.getOtherSellingCosts() != null ? request.getOtherSellingCosts().doubleValue() : 0.0;
                        marketInputs.add(new MarketCostInputDto(mktId, trans, other));
                    }
                    compReq.setMarkets(marketInputs);
                    compReq.setPriceBasis(request.getPriceBasis());
                    compReq.setPriceMode(request.getPriceMode());
                    compReq.setDate(request.getDate());

                    FarmMarketComparisonResponse compRes = farmEconomicsService.compareMarkets(farmerUid, compReq);
                    context.setMarketComparison(compRes);
                } catch (Exception e) {
                    log.warn("Could not compare markets for record {}: {}", context.getEconomicRecord().getId(), e.getMessage());
                }
            }
        }

        // 7. Market Trend Lookup (Module 12)
        if (request.getDecisionType() == AiDecisionType.MARKET_TREND_EXPLANATION || request.getCropId() != null) {
            try {
                String mktId = (!candidateMarkets.isEmpty()) ? candidateMarkets.get(0).getId() : null;
                MarketTrendResponse trendRes = marketTrendService.calculateTrend(
                        request.getCropId(),
                        mktId,
                        "30D",
                        null, null,
                        context.getQuantityUnit()
                );
                context.setMarketTrend(trendRes);
                if (trendRes != null && "FRESH".equalsIgnoreCase(trendRes.getFreshnessStatus())) {
                    context.setHasVerifiedPrice(true);
                }
            } catch (Exception e) {
                log.warn("Could not fetch Market Trend intelligence context: {}", e.getMessage());
            }
        }

        // 8. Price Forecast Lookup (Module 13)
        if ((request.getDecisionType() == AiDecisionType.PRICE_FORECAST_EXPLANATION || request.getCropId() != null) && forecastService != null) {
            try {
                String mktId = (!candidateMarkets.isEmpty()) ? candidateMarkets.get(0).getId() : (request.getMarketIds() != null && !request.getMarketIds().isEmpty() ? request.getMarketIds().get(0) : null);
                if (request.getCropId() != null && mktId != null) {
                    com.farmlink.api.dto.forecast.ForecastRequest fcReq = new com.farmlink.api.dto.forecast.ForecastRequest(
                            request.getCropId(), mktId, "7_DAYS", 30, context.getQuantityUnit()
                    );
                    com.farmlink.api.dto.forecast.ForecastResponse fcRes = forecastService.getPriceForecast(fcReq);
                    context.setPriceForecast(fcRes);
                }
            } catch (Exception e) {
                log.warn("Could not fetch Price Forecast context: {}", e.getMessage());
            }
        }

        context.setDataGaps(dataGaps);
        return context;
    }
}
