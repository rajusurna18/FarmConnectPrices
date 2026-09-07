package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.SmartSellingDecisionResponse.ForecastScenarioItem;
import com.farmlink.api.dto.SmartSellingDecisionResponse.MarketCard;
import com.farmlink.api.dto.SmartSellingDecisionResponse.TradeOffItem;
import com.farmlink.api.dto.ai.*;
import com.farmlink.api.dto.forecast.ForecastRequest;
import com.farmlink.api.dto.forecast.ForecastResponse;
import com.farmlink.api.service.ai.AiDecisionService;
import com.farmlink.api.service.forecast.ForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class SmartSellingDecisionService {

    private static final Logger logger = LoggerFactory.getLogger(SmartSellingDecisionService.class);

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_INVALID_CROP = "INVALID_CROP";
    public static final String STATUS_INVALID_MARKET = "INVALID_MARKET";
    public static final String STATUS_NO_VERIFIED_PRICE = "NO_VERIFIED_PRICE";

    public static final Set<String> SUPPORTED_HORIZONS = Set.of("1_DAY", "3_DAYS", "7_DAYS", "14_DAYS");

    private final FarmService farmService;
    private final CropMasterService cropMasterService;
    private final MarketService marketService;
    private final MarketPriceService marketPriceService;
    private final ProfitabilityCalculationService profitabilityCalculationService;
    private final FarmEconomicsService farmEconomicsService;
    private final FarmEconomicsCalculationService farmEconomicsCalculationService;
    private final MarketTrendService marketTrendService;
    private final ForecastService forecastService;
    private final SmartSellingRankingEngine rankingEngine;
    private final SmartSellingTradeOffEngine tradeOffEngine;
    private final AiDecisionService aiDecisionService;

    @Autowired
    public SmartSellingDecisionService(
            FarmService farmService,
            CropMasterService cropMasterService,
            MarketService marketService,
            MarketPriceService marketPriceService,
            ProfitabilityCalculationService profitabilityCalculationService,
            FarmEconomicsService farmEconomicsService,
            FarmEconomicsCalculationService farmEconomicsCalculationService,
            MarketTrendService marketTrendService,
            @Autowired(required = false) ForecastService forecastService,
            SmartSellingRankingEngine rankingEngine,
            SmartSellingTradeOffEngine tradeOffEngine,
            AiDecisionService aiDecisionService
    ) {
        this.farmService = farmService;
        this.cropMasterService = cropMasterService;
        this.marketService = marketService;
        this.marketPriceService = marketPriceService;
        this.profitabilityCalculationService = profitabilityCalculationService;
        this.farmEconomicsService = farmEconomicsService;
        this.farmEconomicsCalculationService = farmEconomicsCalculationService;
        this.marketTrendService = marketTrendService;
        this.forecastService = forecastService;
        this.rankingEngine = rankingEngine;
        this.tradeOffEngine = tradeOffEngine;
        this.aiDecisionService = aiDecisionService;
    }

    public SmartSellingDecisionResponse evaluateSmartSelling(String ownerUid, SmartSellingDecisionRequest req) {
        if (ownerUid == null || ownerUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Farmer owner UID is required.");
        }
        farmService.verifyFarmerRole(ownerUid);

        if (req == null) {
            return SmartSellingDecisionResponse.error(STATUS_ERROR, "Decision request cannot be null.");
        }

        // Validate basic inputs
        if (req.getCropId() == null || req.getCropId().trim().isEmpty()) {
            return SmartSellingDecisionResponse.error(STATUS_INVALID_CROP, "Crop ID is required.");
        }
        if (req.getQuantity() <= 0) {
            return SmartSellingDecisionResponse.error(STATUS_ERROR, "Quantity must be greater than zero.");
        }
        if (req.getQuantityUnit() == null || req.getQuantityUnit().trim().isEmpty()) {
            return SmartSellingDecisionResponse.error(STATUS_ERROR, "Quantity unit is required.");
        }
        if (req.getCandidateMarketIds() == null || req.getCandidateMarketIds().isEmpty()) {
            return SmartSellingDecisionResponse.error(STATUS_INVALID_MARKET, "At least one candidate market must be provided.");
        }

        // Bound candidate market list to max 10 to avoid Firestore fan-out
        List<String> marketIds = req.getCandidateMarketIds();
        if (marketIds.size() > 10) {
            marketIds = marketIds.subList(0, 10);
        }

        // Validate Forecast Horizon (Module 13 supported: 1_DAY, 3_DAYS, 7_DAYS, 14_DAYS)
        String horizon = (req.getForecastHorizon() != null && !req.getForecastHorizon().trim().isEmpty())
                ? req.getForecastHorizon().trim().toUpperCase(Locale.ROOT)
                : "7_DAYS";
        if (!SUPPORTED_HORIZONS.contains(horizon)) {
            throw new IllegalArgumentException("Unsupported forecast horizon: " + horizon + ". Supported horizons: 1_DAY, 3_DAYS, 7_DAYS, 14_DAYS.");
        }

        // Validate Crop
        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(req.getCropId());
        } catch (Exception e) {
            return SmartSellingDecisionResponse.error(STATUS_INVALID_CROP, "Invalid crop ID: " + req.getCropId());
        }

        // Verify Saved Economic Record if provided (Verifies ownership)
        FarmEconomicRecordResponse economicRecord = null;
        if (req.getEconomicRecordId() != null && !req.getEconomicRecordId().trim().isEmpty()) {
            economicRecord = farmEconomicsService.getEconomicRecordById(req.getEconomicRecordId(), ownerUid);
        }

        // Verify Farm if provided
        if (req.getFarmId() != null && !req.getFarmId().trim().isEmpty()) {
            farmService.getFarmById(req.getFarmId(), ownerUid);
        }

        // Determine Cost Precedence & Classification
        boolean hasCustomCosts = req.getCustomSellingCosts() != null;
        boolean hasSavedEconomics = economicRecord != null;
        boolean hasComparableEconomics = hasCustomCosts || hasSavedEconomics;

        String costClassification;
        if (hasCustomCosts) {
            costClassification = "USER_ESTIMATE";
        } else if (hasSavedEconomics) {
            costClassification = "SAVED_FARMER_RECORD";
        } else {
            costClassification = "NOT_VERIFIED";
        }

        SellingCostsDto sellingCostsToUse;
        if (hasCustomCosts) {
            sellingCostsToUse = req.getCustomSellingCosts();
        } else if (hasSavedEconomics) {
            sellingCostsToUse = economicRecord.getSellingCosts();
        } else {
            sellingCostsToUse = new SellingCostsDto(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<MarketCard> evaluatedCards = new ArrayList<>();
        List<MarketEvaluationResponse> unavailableMarkets = new ArrayList<>();
        List<ForecastScenarioItem> forecastScenarios = new ArrayList<>();

        String basis = (req.getPriceBasis() != null && !req.getPriceBasis().trim().isEmpty())
                ? req.getPriceBasis().trim().toUpperCase(Locale.ROOT)
                : DecisionSupportService.PRICE_BASIS_MODAL;

        String todayStr = LocalDate.now().toString();

        for (String marketId : marketIds) {
            if (marketId == null || marketId.trim().isEmpty()) continue;

            MarketResponse marketObj;
            try {
                marketObj = marketService.getMarketById(marketId);
            } catch (Exception e) {
                logger.warn("Market not found for ID: {}", marketId);
                continue;
            }

            MarketSummaryResponse marketSummary = new MarketSummaryResponse(
                    marketObj.getId(), marketObj.getName(), marketObj.getCode(), marketObj.getType(),
                    marketObj.getLocation().getState(), marketObj.getLocation().getDistrict(), marketObj.getLocation().getMandal(),
                    marketObj.getStatus(), 0
            );

            // 1. Fetch Latest Verified Price (Module 07)
            List<MarketPriceResponse> priceRecords = marketPriceService.getMarketPrices(
                    null, null, marketId, req.getCropId(), null,
                    MarketPriceService.QUALITY_VERIFIED, null, "priceDate", "desc", 1, 10, 10
            );

            if (priceRecords.isEmpty()) {
                MarketEvaluationResponse unavail = MarketEvaluationResponse.error(
                        STATUS_NO_VERIFIED_PRICE, "No verified price is available for this market."
                );
                unavail.setCrop(crop);
                unavail.setMarket(marketSummary);
                unavailableMarkets.add(unavail);
                continue;
            }

            MarketPriceResponse priceRecord = priceRecords.get(0);
            double rawPrice;
            if (DecisionSupportService.PRICE_BASIS_MIN.equals(basis)) {
                rawPrice = priceRecord.getMinPrice();
            } else if (DecisionSupportService.PRICE_BASIS_MAX.equals(basis)) {
                rawPrice = priceRecord.getMaxPrice();
            } else {
                rawPrice = priceRecord.getModalPrice();
            }

            boolean isStale = priceRecord.getPriceDate() != null && priceRecord.getPriceDate().compareTo(todayStr) < 0;

            // 2. Compute Net Realization & Revenue (Module 09 / 10)
            BigDecimal bPrice = BigDecimal.valueOf(rawPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bQuantity = BigDecimal.valueOf(req.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bTransport = sellingCostsToUse.getTransportationCost() != null ? sellingCostsToUse.getTransportationCost() : BigDecimal.ZERO;
            BigDecimal bOther = sellingCostsToUse.getOtherSellingCosts() != null ? sellingCostsToUse.getOtherSellingCosts() : BigDecimal.ZERO;

            ProfitabilityCalculationService.ProfitabilityResult profitCalc = profitabilityCalculationService.calculate(
                    bPrice, bQuantity, bTransport, bOther
            );

            // 3. Compute Farm Profitability if production cost exists (Module 10)
            BigDecimal productionCost = (economicRecord != null) ? economicRecord.getTotalProductionCost() : null;
            BigDecimal netProfit = null;
            BigDecimal roi = null;
            String profitStatus = "UNKNOWN";

            if (economicRecord != null && economicRecord.getProductionCosts() != null) {
                FarmEconomicsCalculationService.CalculationResult fullCalc = farmEconomicsCalculationService.calculateProfitability(
                        bPrice, bQuantity, economicRecord.getProductionCosts(), sellingCostsToUse
                );
                netProfit = fullCalc.getEstimatedProfit();
                roi = fullCalc.getRoi();
                profitStatus = fullCalc.getProfitabilityStatus();
            }

            // 4. Fetch 30-Day Trend (Module 12)
            MarketTrendResponse trendResponse = null;
            String trendDirection = "UNKNOWN";
            String trendQuality = "INSUFFICIENT";
            try {
                trendResponse = marketTrendService.calculateTrend(req.getCropId(), marketId, "30D", null, null, req.getQuantityUnit());
                if (trendResponse != null) {
                    trendDirection = trendResponse.getTrendDirection();
                    trendQuality = trendResponse.getDataQuality();
                }
            } catch (Exception e) {
                logger.warn("Trend evaluation failed for market {}: {}", marketId, e.getMessage());
            }

            // 5. Fetch Forecast Scenario (Module 13)
            String fConfidence = "UNAVAILABLE";
            if (forecastService != null) {
                try {
                    ForecastRequest fReq = new ForecastRequest(req.getCropId(), marketId, horizon, 30, req.getQuantityUnit());
                    ForecastResponse fResp = forecastService.getPriceForecast(fReq);
                    if (fResp != null && fResp.getForecastPrice() != null) {
                        fConfidence = fResp.getConfidence() != null ? fResp.getConfidence().name() : "MEDIUM";

                        ForecastScenarioItem scenarioItem = new ForecastScenarioItem();
                        scenarioItem.setMarketId(marketId);
                        scenarioItem.setMarketName(marketSummary.getName());
                        scenarioItem.setHorizon(horizon);
                        scenarioItem.setDirection(fResp.getDirection() != null ? fResp.getDirection().name() : "UNCERTAIN");
                        scenarioItem.setConfidence(fConfidence);
                        scenarioItem.setScenario(true);
                        scenarioItem.setCurrentVerifiedPrice(rawPrice);
                        scenarioItem.setExpectedPrice(fResp.getForecastPrice());
                        scenarioItem.setBearPrice(fResp.getForecastLowerBound());
                        scenarioItem.setBullPrice(fResp.getForecastUpperBound());

                        // Compute scenario net realizations
                        if (fResp.getForecastPrice() != null) {
                            scenarioItem.setExpectedNetRealization(profitabilityCalculationService.calculate(
                                    BigDecimal.valueOf(fResp.getForecastPrice()), bQuantity, bTransport, bOther
                            ).getEstimatedNetRealization());
                        }
                        if (fResp.getForecastLowerBound() != null) {
                            scenarioItem.setBearNetRealization(profitabilityCalculationService.calculate(
                                    BigDecimal.valueOf(fResp.getForecastLowerBound()), bQuantity, bTransport, bOther
                            ).getEstimatedNetRealization());
                        }
                        if (fResp.getForecastUpperBound() != null) {
                            scenarioItem.setBullNetRealization(profitabilityCalculationService.calculate(
                                    BigDecimal.valueOf(fResp.getForecastUpperBound()), bQuantity, bTransport, bOther
                            ).getEstimatedNetRealization());
                        }

                        forecastScenarios.add(scenarioItem);
                    }
                } catch (Exception e) {
                    logger.warn("Forecast service call failed for market {}: {}", marketId, e.getMessage());
                }
            }

            // Build MarketCard
            MarketCard card = new MarketCard();
            card.setMarket(marketSummary);
            card.setSelectedPrice(bPrice);
            card.setPriceUnit(priceRecord.getUnit());
            card.setPriceDate(priceRecord.getPriceDate());
            card.setStalePrice(isStale);
            card.setCostSourceClassification(costClassification);

            card.setGrossRevenue(profitCalc.getGrossRevenue());
            card.setTotalSellingCost(profitCalc.getTotalSellingCosts());
            card.setEstimatedNetRealization(profitCalc.getEstimatedNetRealization());
            card.setNetRealizationPerUnit(profitCalc.getNetRealizationPerUnit());

            card.setTotalProductionCost(productionCost);
            card.setEstimatedNetProfit(netProfit);
            card.setRoi(roi);
            card.setProfitabilityStatus(profitStatus);

            card.setTrendDirection(trendDirection);
            card.setTrendDataQuality(trendQuality);
            card.setDecisionBasis(hasComparableEconomics ? "COMPLETE_ECONOMIC_NET_REALIZATION" : "RAW_VERIFIED_PRICE_ONLY");

            evaluatedCards.add(card);
        }

        if (evaluatedCards.isEmpty()) {
            SmartSellingDecisionResponse resp = SmartSellingDecisionResponse.error(
                    STATUS_NO_VERIFIED_PRICE, "No verified price observations were available for the candidate markets."
            );
            resp.setCrop(crop);
            resp.setQuantity(req.getQuantity());
            resp.setQuantityUnit(req.getQuantityUnit());
            resp.setPriceBasis(basis);
            resp.setUnavailableMarkets(unavailableMarkets);
            return resp;
        }

        // 6. Rank Market Cards via Lexicographic Ranking Engine
        rankingEngine.rankMarketCards(evaluatedCards, hasComparableEconomics);
        MarketCard topCard = evaluatedCards.get(0);

        // 7. Compute Decoupled Confidence & Quality Indicators
        String overallConfidence = rankingEngine.computeOverallConfidence(evaluatedCards, hasComparableEconomics);

        String economicsCompleteness;
        if (hasSavedEconomics) {
            economicsCompleteness = "COMPLETE_FARM_ECONOMICS";
        } else if (hasCustomCosts) {
            economicsCompleteness = "COMPLETE_SELLING_COSTS";
        } else {
            economicsCompleteness = "MISSING";
        }

        String topTrendQuality = topCard.getTrendDataQuality();
        String primaryForecastConfidence = forecastScenarios.isEmpty() ? "UNAVAILABLE" : forecastScenarios.get(0).getConfidence();

        // 8. Generate Consequence-Verified Trade-Offs
        List<TradeOffItem> tradeOffs = tradeOffEngine.evaluateTradeOffs(evaluatedCards);

        // 9. Formulate Structured Response
        SmartSellingDecisionResponse response = new SmartSellingDecisionResponse();
        response.setStatus(STATUS_SUCCESS);
        response.setMessage("Smart selling decision evaluation completed successfully.");
        response.setCrop(crop);
        response.setQuantity(req.getQuantity());
        response.setQuantityUnit(req.getQuantityUnit());
        response.setPriceBasis(basis);

        response.setRecommendedMarketId(topCard.getMarket().getId());
        response.setRecommendationType(hasSavedEconomics ? "BEST_NET_PROFIT" : (hasComparableEconomics ? "BEST_ESTIMATED_NET_REALIZATION" : "BEST_VERIFIED_CURRENT_PRICE"));
        response.setDecisionBasis(topCard.getDecisionBasis());

        response.setOverallDecisionConfidence(overallConfidence);
        response.setForecastConfidence(primaryForecastConfidence);
        response.setTrendDataQuality(topTrendQuality);
        response.setEconomicsCompleteness(economicsCompleteness);

        response.setPrimaryRecommendation(topCard);
        response.setRankedMarkets(evaluatedCards);
        response.setUnavailableMarkets(unavailableMarkets);
        response.setTradeOffs(tradeOffs);
        response.setForecastScenarios(forecastScenarios);

        // 10. AI Explanation Integration (Module 11)
        try {
            AiDecisionRequest aiReq = new AiDecisionRequest();
            aiReq.setDecisionType(AiDecisionType.SMART_SELLING_EXPLANATION);
            aiReq.setCropId(req.getCropId());
            aiReq.setMarketIds(List.of(topCard.getMarket().getId()));
            aiReq.setQuantity(BigDecimal.valueOf(req.getQuantity()));
            aiReq.setQuantityUnit(req.getQuantityUnit());

            AiDecisionResponse aiResp = aiDecisionService.processDecisionRequest(ownerUid, aiReq);
            response.setAiExplanation(aiResp);
            response.setRisks(aiResp.getRisks());
            response.setNextSteps(aiResp.getNextSteps());
            response.setLimitations(aiResp.getLimitations());
        } catch (Exception e) {
            logger.warn("AI decision explanation failed: {}", e.getMessage());
            List<String> defaultRisks = List.of("Market prices fluctuate throughout the day and can change before sale execution.");
            List<String> defaultSteps = List.of("Verify current price with local mandi traders before finalizing transport.");
            List<String> defaultLimits = List.of("Estimates are based on latest verified market data and farmer-entered costs.");

            response.setRisks(defaultRisks);
            response.setNextSteps(defaultSteps);
            response.setLimitations(defaultLimits);
        }

        return response;
    }
}
