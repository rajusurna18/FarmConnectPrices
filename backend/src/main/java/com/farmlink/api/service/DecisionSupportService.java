package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DecisionSupportService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionSupportService.class);

    public static final String PRICE_BASIS_MODAL = "MODAL";
    public static final String PRICE_BASIS_MIN = "MIN";
    public static final String PRICE_BASIS_MAX = "MAX";

    public static final String MODE_LATEST_AVAILABLE = "LATEST_AVAILABLE";
    public static final String MODE_EXACT_DATE = "EXACT_DATE";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_NO_VERIFIED_PRICE = "NO_VERIFIED_PRICE";
    public static final String STATUS_UNIT_MISMATCH = "UNIT_MISMATCH";
    public static final String STATUS_INVALID_CROP = "INVALID_CROP";
    public static final String STATUS_INVALID_MARKET = "INVALID_MARKET";
    public static final String STATUS_ERROR = "ERROR";

    private final MarketPriceService marketPriceService;
    private final MarketService marketService;
    private final CropMasterService cropMasterService;
    private final ProfitabilityCalculationService calculationService;

    public DecisionSupportService(
            MarketPriceService marketPriceService,
            MarketService marketService,
            CropMasterService cropMasterService,
            ProfitabilityCalculationService calculationService
    ) {
        this.marketPriceService = marketPriceService;
        this.marketService = marketService;
        this.cropMasterService = cropMasterService;
        this.calculationService = calculationService;
    }

    public MarketEvaluationResponse evaluateMarket(MarketEvaluationRequest req) {
        if (req == null) {
            return MarketEvaluationResponse.error(STATUS_ERROR, "Request cannot be null.");
        }

        // Validate basic inputs
        if (req.getCropId() == null || req.getCropId().trim().isEmpty()) {
            return MarketEvaluationResponse.error(STATUS_INVALID_CROP, "Crop ID is required.");
        }
        if (req.getMarketId() == null || req.getMarketId().trim().isEmpty()) {
            return MarketEvaluationResponse.error(STATUS_INVALID_MARKET, "Market ID is required.");
        }
        if (req.getQuantity() <= 0) {
            return MarketEvaluationResponse.error(STATUS_ERROR, "Quantity must be greater than zero.");
        }
        if (req.getQuantityUnit() == null || req.getQuantityUnit().trim().isEmpty()) {
            return MarketEvaluationResponse.error(STATUS_ERROR, "Quantity unit is required.");
        }
        if (req.getTransportationCost() != null && req.getTransportationCost() < 0) {
            return MarketEvaluationResponse.error(STATUS_ERROR, "Transportation cost cannot be negative.");
        }
        if (req.getOtherSellingCosts() != null && req.getOtherSellingCosts() < 0) {
            return MarketEvaluationResponse.error(STATUS_ERROR, "Other selling costs cannot be negative.");
        }

        String mode = (req.getPriceMode() != null && !req.getPriceMode().trim().isEmpty())
                ? req.getPriceMode().trim().toUpperCase(Locale.ROOT)
                : MODE_LATEST_AVAILABLE;

        if (MODE_EXACT_DATE.equals(mode) && (req.getDate() == null || req.getDate().trim().isEmpty())) {
            return MarketEvaluationResponse.error(STATUS_ERROR, "Date is required when priceMode is EXACT_DATE.");
        }

        // Validate Crop
        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(req.getCropId());
        } catch (Exception e) {
            return MarketEvaluationResponse.error(STATUS_INVALID_CROP, "Invalid or non-existent crop: " + req.getCropId());
        }

        // Validate Market
        MarketSummaryResponse marketSummary;
        try {
            MarketResponse market = marketService.getMarketById(req.getMarketId());
            marketSummary = new MarketSummaryResponse(
                    market.getId(), market.getName(), market.getCode(), market.getType(),
                    market.getLocation().getState(), market.getLocation().getDistrict(), market.getLocation().getMandal(),
                    market.getStatus(), 0
            );
        } catch (Exception e) {
            return MarketEvaluationResponse.error(STATUS_INVALID_MARKET, "Invalid or non-existent market: " + req.getMarketId());
        }

        // Retrieve verified price record using existing MarketPriceService
        MarketPriceResponse priceRecord = null;
        if (MODE_EXACT_DATE.equals(mode)) {
            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    null, null, req.getMarketId(), req.getCropId(), req.getDate().trim(),
                    MarketPriceService.QUALITY_VERIFIED, null, "priceDate", "desc", 1, 10, 10
            );
            if (!prices.isEmpty()) {
                priceRecord = prices.get(0);
            }
        } else {
            // LATEST_AVAILABLE mode
            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    null, null, req.getMarketId(), req.getCropId(), null,
                    MarketPriceService.QUALITY_VERIFIED, null, "priceDate", "desc", 1, 10, 10
            );
            if (!prices.isEmpty()) {
                priceRecord = prices.get(0);
            }
        }

        if (priceRecord == null) {
            String noPriceMsg = MODE_EXACT_DATE.equals(mode)
                    ? "No verified price is available for this crop and market for the selected date."
                    : "No verified price is available for this crop and market.";
            MarketEvaluationResponse resp = MarketEvaluationResponse.error(STATUS_NO_VERIFIED_PRICE, noPriceMsg);
            resp.setCrop(crop);
            resp.setMarket(marketSummary);
            return resp;
        }

        // Unit compatibility check
        String priceUnit = priceRecord.getUnit() != null ? priceRecord.getUnit().trim() : "";
        String requestedUnit = req.getQuantityUnit().trim();

        if (!requestedUnit.equalsIgnoreCase(priceUnit)) {
            MarketEvaluationResponse resp = MarketEvaluationResponse.error(
                    STATUS_UNIT_MISMATCH, "Price and quantity units are incompatible."
            );
            resp.setCrop(crop);
            resp.setMarket(marketSummary);
            resp.setPriceUnit(priceUnit);
            resp.setQuantityUnit(requestedUnit);
            return resp;
        }

        // Extract selected price based on price basis
        String basis = (req.getPriceBasis() != null && !req.getPriceBasis().trim().isEmpty())
                ? req.getPriceBasis().trim().toUpperCase(Locale.ROOT)
                : PRICE_BASIS_MODAL;

        double rawPrice;
        if (PRICE_BASIS_MIN.equals(basis)) {
            rawPrice = priceRecord.getMinPrice();
        } else if (PRICE_BASIS_MAX.equals(basis)) {
            rawPrice = priceRecord.getMaxPrice();
        } else {
            basis = PRICE_BASIS_MODAL;
            rawPrice = priceRecord.getModalPrice();
        }

        // Perform calculation using ProfitabilityCalculationService
        BigDecimal bPrice = BigDecimal.valueOf(rawPrice);
        BigDecimal bQuantity = BigDecimal.valueOf(req.getQuantity());
        BigDecimal bTransport = BigDecimal.valueOf(req.getTransportationCost() != null ? req.getTransportationCost() : 0.0);
        BigDecimal bOther = BigDecimal.valueOf(req.getOtherSellingCosts() != null ? req.getOtherSellingCosts() : 0.0);

        ProfitabilityCalculationService.ProfitabilityResult calc = calculationService.calculate(
                bPrice, bQuantity, bTransport, bOther
        );

        // Check freshness / stale date
        String todayStr = LocalDate.now().toString();
        boolean isStale = priceRecord.getPriceDate() != null && priceRecord.getPriceDate().compareTo(todayStr) < 0;
        String staleMsg = isStale
                ? "Based on the latest available verified price from " + priceRecord.getPriceDate() + "."
                : null;

        MarketEvaluationResponse resp = new MarketEvaluationResponse();
        resp.setStatus(STATUS_SUCCESS);
        resp.setMessage("Evaluation completed successfully.");
        resp.setCrop(crop);
        resp.setMarket(marketSummary);
        resp.setSelectedPrice(calc.getPrice());
        resp.setPriceBasis(basis);
        resp.setPriceUnit(priceUnit);
        resp.setQuantity(calc.getQuantity());
        resp.setQuantityUnit(requestedUnit);
        resp.setGrossRevenue(calc.getGrossRevenue());
        resp.setTransportationCost(calc.getTransportationCost());
        resp.setOtherSellingCosts(calc.getOtherSellingCosts());
        resp.setTotalSellingCosts(calc.getTotalSellingCosts());
        resp.setEstimatedNetRealization(calc.getEstimatedNetRealization());
        resp.setNetRealizationPerUnit(calc.getNetRealizationPerUnit());
        resp.setSellingCostBreakEvenPrice(calc.getSellingCostBreakEvenPrice());
        resp.setCurrency(priceRecord.getCurrency() != null ? priceRecord.getCurrency() : "INR");
        resp.setPriceDate(priceRecord.getPriceDate());
        resp.setObservedAt(priceRecord.getObservedAt());
        resp.setSource(priceRecord.getSource());
        resp.setQualityStatus(priceRecord.getQualityStatus());
        resp.setStalePrice(isStale);
        resp.setStaleMessage(staleMsg);
        resp.setLoss(calc.isLoss());

        return resp;
    }

    public MarketProfitabilityComparisonResponse compareMarkets(MarketComparisonRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Comparison request cannot be null.");
        }
        if (req.getCropId() == null || req.getCropId().trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }
        if (req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (req.getQuantityUnit() == null || req.getQuantityUnit().trim().isEmpty()) {
            throw new IllegalArgumentException("Quantity unit is required.");
        }
        if (req.getMarkets() == null || req.getMarkets().isEmpty()) {
            throw new IllegalArgumentException("At least one market must be selected for comparison.");
        }

        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(req.getCropId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid crop ID: " + req.getCropId());
        }

        List<MarketEvaluationResponse> evaluations = new ArrayList<>();
        for (MarketCostInputDto costInput : req.getMarkets()) {
            if (costInput.getMarketId() == null || costInput.getMarketId().trim().isEmpty()) {
                continue;
            }

            MarketEvaluationRequest singleReq = new MarketEvaluationRequest(
                    req.getCropId(),
                    costInput.getMarketId(),
                    req.getQuantity(),
                    req.getQuantityUnit(),
                    req.getPriceBasis(),
                    req.getPriceMode(),
                    req.getDate(),
                    costInput.getTransportationCost(),
                    costInput.getOtherSellingCosts()
            );

            MarketEvaluationResponse eval = evaluateMarket(singleReq);
            evaluations.add(eval);
        }

        // Separate successful evaluations for ranking
        List<MarketEvaluationResponse> successful = evaluations.stream()
                .filter(e -> STATUS_SUCCESS.equals(e.getStatus()))
                .collect(Collectors.toList());

        // Sort by estimatedNetRealization descending
        successful.sort((a, b) -> b.getEstimatedNetRealization().compareTo(a.getEstimatedNetRealization()));

        // Re-combine: successful first (sorted), followed by unavailable markets
        List<MarketEvaluationResponse> sortedEvaluations = new ArrayList<>(successful);
        for (MarketEvaluationResponse e : evaluations) {
            if (!STATUS_SUCCESS.equals(e.getStatus())) {
                sortedEvaluations.add(e);
            }
        }

        MarketEvaluationResponse topMarket = !successful.isEmpty() ? successful.get(0) : null;
        String rankingSummary = topMarket != null
                ? "Highest estimated net realization: " + topMarket.getMarket().getName() + " (₹" + topMarket.getEstimatedNetRealization() + ")"
                : "No valid verified market prices were available for comparison.";

        return new MarketProfitabilityComparisonResponse(
                crop,
                req.getQuantity(),
                req.getQuantityUnit(),
                req.getPriceBasis(),
                req.getPriceMode(),
                req.getDate(),
                sortedEvaluations,
                topMarket,
                rankingSummary
        );
    }
}
