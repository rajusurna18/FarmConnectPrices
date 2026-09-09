package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.*;
import com.farmlink.api.dto.forecast.*;
import com.farmlink.api.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastService.class);

    private final MarketPriceService marketPriceService;
    private final CropMasterService cropMasterService;
    private final MarketService marketService;
    private final ForecastDataPreparerService dataPreparerService;
    private final WeightedMovingAverageForecastModel wmaModel;
    private final SimpleMovingAverageForecastModel smaModel;
    private final ForecastBacktestService backtestService;
    private final ForecastConfidenceService confidenceService;
    private final FarmEconomicsService farmEconomicsService;

    public ForecastService(
            MarketPriceService marketPriceService,
            CropMasterService cropMasterService,
            MarketService marketService,
            ForecastDataPreparerService dataPreparerService,
            WeightedMovingAverageForecastModel wmaModel,
            SimpleMovingAverageForecastModel smaModel,
            ForecastBacktestService backtestService,
            ForecastConfidenceService confidenceService,
            FarmEconomicsService farmEconomicsService
    ) {
        this.marketPriceService = marketPriceService;
        this.cropMasterService = cropMasterService;
        this.marketService = marketService;
        this.dataPreparerService = dataPreparerService;
        this.wmaModel = wmaModel;
        this.smaModel = smaModel;
        this.backtestService = backtestService;
        this.confidenceService = confidenceService;
        this.farmEconomicsService = farmEconomicsService;
    }

    @Cacheable(value = "priceForecasts", key = "(#request.cropId != null ? #request.cropId : '') + '_' + (#request.marketId != null ? #request.marketId : '') + '_' + (#request.horizon != null ? #request.horizon : '') + '_' + (#request.unit != null ? #request.unit : 'QUINTAL')", unless = "#result == null || #result.confidence == T(com.farmlink.api.dto.forecast.ForecastConfidence).INSUFFICIENT_DATA")
    public ForecastResponse getPriceForecast(ForecastRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Forecast request cannot be null.");
        }
        if (request.getCropId() == null || request.getCropId().trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }
        if (request.getMarketId() == null || request.getMarketId().trim().isEmpty()) {
            throw new IllegalArgumentException("Market ID is required.");
        }

        ForecastHorizon horizon = ForecastHorizon.fromCode(request.getHorizon());
        String targetUnit = request.getUnit() != null && !request.getUnit().trim().isEmpty() ? request.getUnit().trim() : MarketPriceService.UNIT_QUINTAL;
        int lookbackDays = request.getLookbackDays() != null && request.getLookbackDays() >= 7 && request.getLookbackDays() <= 90
                ? request.getLookbackDays() : 30;

        // Select Forecast Model Strategy
        ForecastModel model = selectModel(request.getModel());

        // Resolve Crop and Market names
        String cropName = resolveCropName(request.getCropId());
        String marketName = resolveMarketName(request.getMarketId());

        // Fetch Bounded Historical Verified Observations via MarketPriceService
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(lookbackDays);

        List<MarketPriceSummaryResponse> rawPrices = marketPriceService.getMarketPrices(
                request.getMarketId().trim(),
                request.getCropId().trim(),
                null, // priceDate
                fromDate.toString(),
                today.toString(),
                MarketPriceService.QUALITY_VERIFIED,
                targetUnit,
                null, // state
                null, // district
                100   // limit
        );

        List<MarketPriceSummaryResponse> observations = dataPreparerService.prepareDataset(
                rawPrices, request.getCropId(), request.getMarketId()
        );

        ForecastResponse response = new ForecastResponse();
        response.setCropId(request.getCropId().trim());
        response.setCropName(cropName);
        response.setMarketId(request.getMarketId().trim());
        response.setMarketName(marketName);
        response.setModel(model.getModelType().getCode());
        response.setHorizon(horizon.getCode());
        response.setUnit(targetUnit);

        int minRequired = model.getMinimumObservations(horizon);
        if (observations.size() < minRequired) {
            response.setForecastPrice(null);
            response.setForecastLowerBound(null);
            response.setForecastUpperBound(null);
            response.setDirection(ForecastDirection.UNCERTAIN);
            response.setConfidence(ForecastConfidence.INSUFFICIENT_DATA);
            response.setDataQuality("INSUFFICIENT");
            response.setObservationsUsed(observations.size());
            response.setLatestObservationDate(observations.isEmpty() ? null : observations.get(observations.size() - 1).getPriceDate());
            response.getLimitations().add("Insufficient historical verified observations. Minimum required: " + minRequired + ", actual available: " + observations.size());
            response.setBacktest(new ForecastBacktestDto(0, null, null, horizon.getCode()));
            return response;
        }

        // 1. Current Verified Price (latest observation)
        MarketPriceSummaryResponse latestObs = observations.get(observations.size() - 1);
        double currentPrice = latestObs.getModalPrice();
        response.setCurrentVerifiedPrice(currentPrice);
        response.setLatestObservationDate(latestObs.getPriceDate());
        response.setObservationsUsed(observations.size());

        // 2. Generate Point Forecast via ForecastModel
        Double pointForecast = model.forecastPrice(observations, request);
        response.setForecastPrice(pointForecast);

        // 3. Execute Origin-Relative Walk-Forward Backtesting
        ForecastBacktestService.DetailedBacktestResult backtestResult = backtestService.executeBacktest(
                model, observations, request, horizon
        );
        response.setBacktest(backtestResult.getDto());

        // 4. Deterministic Confidence, Bounds, Freshness, and Direction
        ForecastConfidenceService.ConfidenceEvaluationResult evalResult = confidenceService.evaluateConfidenceAndBounds(
                pointForecast, currentPrice, observations, horizon, backtestResult.getDto(), backtestResult.getResiduals(), today
        );

        response.setConfidence(evalResult.getConfidence());
        response.setDirection(evalResult.getDirection());
        response.setDataQuality(evalResult.getDataQuality());
        response.setForecastLowerBound(evalResult.getLowerBound());
        response.setForecastUpperBound(evalResult.getUpperBound());

        // 5. Limitations & Transparency Audit Trail
        response.getLimitations().add("Forecast is based on historical market prices and pure recursive Weighted Moving Average projection.");
        response.getLimitations().add("Future market conditions may differ from historical observations.");
        if (evalResult.getLowerBound() == null) {
            response.getLimitations().add("Insufficient historical backtest observations to derive an empirical uncertainty interval for this horizon.");
        }
        if (!evalResult.isFresh()) {
            response.getLimitations().add("Market price data is " + evalResult.getFreshnessGapDays() + " days old. Verify latest market prices before selling.");
        }

        return response;
    }

    public ForecastScenarioProfitabilityResponse evaluateScenarioProfitability(
            String farmerUid,
            String farmEconomicRecordId,
            String horizonCode
    ) {
        if (farmerUid == null || farmerUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Farmer UID is required.");
        }
        if (farmEconomicRecordId == null || farmEconomicRecordId.trim().isEmpty()) {
            throw new IllegalArgumentException("Farm economic record ID is required.");
        }

        // Fetch Farmer's Economic Record
        FarmEconomicRecordResponse record = farmEconomicsService.getEconomicRecordById(farmEconomicRecordId, farmerUid);
        if (record == null) {
            throw new NoSuchElementException("Farm economic record not found for ID: " + farmEconomicRecordId);
        }

        String cropId = record.getCropId();
        String farmId = record.getFarmId();

        if (cropId == null) {
            throw new IllegalStateException("Farm economic record must have an associated crop ID.");
        }

        // Fetch user's farms or default market
        List<FarmResponse> farms = farmService.getFarmsForOwner(farmerUid);
        String marketId = null;
        if (farms != null && !farms.isEmpty()) {
            for (FarmResponse f : farms) {
                if (f != null && f.getId().equals(farmId) && f.getLocation() != null) {
                    // Try to resolve default market ID if location is present
                    marketId = "mkt_default";
                    break;
                }
            }
        }
        if (marketId == null) {
            // Pick default market for crop if available
            List<MarketSummaryResponse> mkts = marketService.getMarkets(null, null, null, null, null, cropId, 1);
            if (mkts != null && !mkts.isEmpty()) {
                marketId = mkts.get(0).getId();
            }
        }

        if (marketId == null) {
            throw new IllegalStateException("Could not resolve market ID for farm economic record scenario.");
        }

        String unit = record.getYieldUnit() != null ? record.getYieldUnit() : MarketPriceService.UNIT_QUINTAL;

        // Request canonical forecast
        ForecastRequest forecastReq = new ForecastRequest(cropId, marketId, horizonCode, 30, unit);
        ForecastResponse forecast = getPriceForecast(forecastReq);

        if (forecast.getForecastPrice() == null) {
            throw new IllegalStateException("Forecast price unavailable for horizon: " + horizonCode + ". Confidence: " + forecast.getConfidence());
        }

        // Evaluate Current Profitability at current market price
        FarmProfitabilityEvaluationRequest currentReq = new FarmProfitabilityEvaluationRequest();
        currentReq.setEconomicRecordId(farmEconomicRecordId);
        currentReq.setMarketId(marketId);
        FarmProfitabilityEvaluationResponse currentEval = farmEconomicsService.evaluateProfitability(farmerUid, currentReq);

        // Evaluate Scenario Profitability at forecast price (READ-ONLY)
        FarmProfitabilityEvaluationRequest scenarioReq = new FarmProfitabilityEvaluationRequest();
        scenarioReq.setEconomicRecordId(farmEconomicRecordId);
        scenarioReq.setMarketId(marketId);
        FarmProfitabilityEvaluationResponse scenarioEval = farmEconomicsService.evaluateProfitability(farmerUid, scenarioReq);

        ForecastScenarioProfitabilityResponse scenarioResp = new ForecastScenarioProfitabilityResponse();
        scenarioResp.setFarmEconomicRecordId(farmEconomicRecordId);
        scenarioResp.setCurrentPriceUsed(forecast.getCurrentVerifiedPrice());
        scenarioResp.setForecastPriceUsed(forecast.getForecastPrice());
        double diff = forecast.getForecastPrice() - (forecast.getCurrentVerifiedPrice() != null ? forecast.getCurrentVerifiedPrice() : 0.0);
        scenarioResp.setPriceDifference(BigDecimal.valueOf(diff).setScale(2, RoundingMode.HALF_UP).doubleValue());
        if (forecast.getCurrentVerifiedPrice() != null && forecast.getCurrentVerifiedPrice() > 0) {
            double pct = (diff / forecast.getCurrentVerifiedPrice()) * 100.0;
            scenarioResp.setPriceDifferencePercentage(BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        scenarioResp.setCurrentEvaluation(currentEval);
        scenarioResp.setScenarioEvaluation(scenarioEval);

        if (currentEval != null && scenarioEval != null) {
            double currentRev = currentEval.getGrossRevenue() != null ? currentEval.getGrossRevenue().doubleValue() : 0.0;
            double scenarioRev = scenarioEval.getGrossRevenue() != null ? scenarioEval.getGrossRevenue().doubleValue() : 0.0;
            double currentProfit = currentEval.getEstimatedProfit() != null ? currentEval.getEstimatedProfit().doubleValue() : 0.0;
            double scenarioProfit = scenarioEval.getEstimatedProfit() != null ? scenarioEval.getEstimatedProfit().doubleValue() : 0.0;

            scenarioResp.setScenarioRevenueDelta(BigDecimal.valueOf(scenarioRev - currentRev).setScale(2, RoundingMode.HALF_UP).doubleValue());
            scenarioResp.setScenarioProfitDelta(BigDecimal.valueOf(scenarioProfit - currentProfit).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        return scenarioResp;
    }

    private ForecastModel selectModel(String modelCode) {
        ForecastModelType type = ForecastModelType.fromCode(modelCode);
        if (type == ForecastModelType.SIMPLE_MOVING_AVERAGE_V1) {
            return smaModel;
        }
        return wmaModel;
    }

    private String resolveCropName(String cropId) {
        if (cropId == null) return "Unknown Crop";
        try {
            CropResponse crop = cropMasterService.getCropById(cropId);
            return crop != null ? crop.getName() : cropId;
        } catch (Exception e) {
            return cropId;
        }
    }

    private String resolveMarketName(String marketId) {
        if (marketId == null) return "Unknown Market";
        try {
            MarketResponse mkt = marketService.getMarketById(marketId);
            return mkt != null ? mkt.getName() : marketId;
        } catch (Exception e) {
            return marketId;
        }
    }
}
