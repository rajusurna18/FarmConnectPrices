package com.farmlink.api.service;

import com.farmlink.api.dto.*;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FarmEconomicsService {

    private static final Logger logger = LoggerFactory.getLogger(FarmEconomicsService.class);

    public static final Set<String> VALID_AREA_UNITS = Set.of("ACRE", "HECTARE");
    public static final Set<String> VALID_YIELD_UNITS = Set.of("KG", "QUINTAL", "TON");
    public static final Set<String> VALID_SEASONS = Set.of("KHARIF", "RABI", "ZAID");

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_NO_VERIFIED_PRICE = "NO_VERIFIED_PRICE";
    public static final String STATUS_UNIT_MISMATCH = "UNIT_MISMATCH";
    public static final String STATUS_INVALID_CROP = "INVALID_CROP";
    public static final String STATUS_INVALID_FARM = "INVALID_FARM";
    public static final String STATUS_INVALID_MARKET = "INVALID_MARKET";
    public static final String STATUS_ERROR = "ERROR";

    private final Firestore firestore;
    private final FarmService farmService;
    private final CropMasterService cropMasterService;
    private final MarketService marketService;
    private final MarketPriceService marketPriceService;
    private final FarmEconomicsCalculationService calculationService;

    private final Map<String, FarmEconomicRecordResponse> inMemoryRecords = new HashMap<>();

    public FarmEconomicsService(
            Firestore firestore,
            FarmService farmService,
            CropMasterService cropMasterService,
            MarketService marketService,
            MarketPriceService marketPriceService,
            FarmEconomicsCalculationService calculationService
    ) {
        this.firestore = firestore;
        this.farmService = farmService;
        this.cropMasterService = cropMasterService;
        this.marketService = marketService;
        this.marketPriceService = marketPriceService;
        this.calculationService = calculationService;
    }

    // ==========================================
    // CRUD OPERATIONS
    // ==========================================

    public List<FarmEconomicRecordResponse> getEconomicRecordsForOwner(String ownerUid) {
        farmService.verifyFarmerRole(ownerUid);
        List<FarmEconomicRecordResponse> records = new ArrayList<>();

        if (firestore != null) {
            try {
                QuerySnapshot snapshot = firestore.collection("farmEconomics")
                        .whereEqualTo("ownerUid", ownerUid)
                        .get().get();

                if (snapshot != null && !snapshot.isEmpty()) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        records.add(mapDocToRecordResponse(doc));
                    }
                    return records;
                }
            } catch (Exception e) {
                logger.warn("Error fetching farmEconomics for ownerUid {}: {}", ownerUid, e.getMessage());
            }
        }

        // Fallback for in-memory / offline test environment
        for (FarmEconomicRecordResponse r : inMemoryRecords.values()) {
            if (ownerUid.equals(r.getOwnerUid())) {
                records.add(r);
            }
        }
        return records;
    }

    public FarmEconomicRecordResponse getEconomicRecordById(String id, String ownerUid) {
        farmService.verifyFarmerRole(ownerUid);
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Economic record ID cannot be empty.");
        }

        if (firestore != null) {
            try {
                DocumentSnapshot doc = firestore.collection("farmEconomics").document(id).get().get();
                if (doc.exists()) {
                    String recordOwner = doc.getString("ownerUid");
                    if (!ownerUid.equals(recordOwner)) {
                        throw new AccessDeniedException("Access denied. You do not own this economic record.");
                    }
                    return mapDocToRecordResponse(doc);
                } else {
                    throw new NoSuchElementException("Farm economic record not found with ID: " + id);
                }
            } catch (AccessDeniedException | NoSuchElementException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Error fetching farmEconomics ID {}: {}", id, e.getMessage());
            }
        }

        FarmEconomicRecordResponse r = inMemoryRecords.get(id);
        if (r == null) {
            throw new NoSuchElementException("Farm economic record not found with ID: " + id);
        }
        if (!ownerUid.equals(r.getOwnerUid())) {
            throw new AccessDeniedException("Access denied. You do not own this economic record.");
        }
        return r;
    }

    public FarmEconomicRecordResponse createEconomicRecord(String ownerUid, FarmEconomicRecordRequest request) {
        farmService.verifyFarmerRole(ownerUid);
        validateRecordRequest(ownerUid, request);

        FarmResponse farm = farmService.getFarmById(request.getFarmId(), ownerUid);
        CropResponse crop = cropMasterService.getCropById(request.getCropId());

        String id = UUID.randomUUID().toString();
        String nowIso = Instant.now().toString();

        BigDecimal totalProductionCost = calculationService.calculateTotalProductionCost(request.getProductionCosts());
        BigDecimal totalSellingCost = calculationService.calculateTotalSellingCost(request.getSellingCosts());
        BigDecimal totalCost = totalProductionCost.add(totalSellingCost);

        BigDecimal productionCostPerUnit = totalProductionCost.divide(request.getExpectedYield(), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalCostPerUnit = totalCost.divide(request.getExpectedYield(), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal breakEvenSellingPrice = totalCostPerUnit;

        FarmEconomicRecordResponse response = buildResponse(
                id, ownerUid, farm.getId(), crop.getId(), farm.getName(), crop.getName(),
                request.getSeason().trim().toUpperCase(), request.getCultivatedArea(), request.getCultivatedAreaUnit().trim().toUpperCase(),
                request.getExpectedYield(), request.getYieldUnit().trim().toUpperCase(),
                request.getProductionCosts(), request.getSellingCosts(),
                totalProductionCost, totalSellingCost, totalCost,
                productionCostPerUnit, totalCostPerUnit, breakEvenSellingPrice,
                nowIso, nowIso
        );

        Map<String, Object> data = mapResponseToMap(response);

        if (firestore != null) {
            try {
                firestore.collection("farmEconomics").document(id).set(data).get();
            } catch (Exception e) {
                logger.warn("Error saving farmEconomics to Firestore: {}", e.getMessage());
            }
        }

        inMemoryRecords.put(id, response);
        return response;
    }

    public FarmEconomicRecordResponse updateEconomicRecord(String id, String ownerUid, FarmEconomicRecordRequest request) {
        farmService.verifyFarmerRole(ownerUid);
        FarmEconomicRecordResponse existing = getEconomicRecordById(id, ownerUid);
        validateRecordRequest(ownerUid, request);

        FarmResponse farm = farmService.getFarmById(request.getFarmId(), ownerUid);
        CropResponse crop = cropMasterService.getCropById(request.getCropId());

        String nowIso = Instant.now().toString();

        BigDecimal totalProductionCost = calculationService.calculateTotalProductionCost(request.getProductionCosts());
        BigDecimal totalSellingCost = calculationService.calculateTotalSellingCost(request.getSellingCosts());
        BigDecimal totalCost = totalProductionCost.add(totalSellingCost);

        BigDecimal productionCostPerUnit = totalProductionCost.divide(request.getExpectedYield(), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalCostPerUnit = totalCost.divide(request.getExpectedYield(), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal breakEvenSellingPrice = totalCostPerUnit;

        FarmEconomicRecordResponse response = buildResponse(
                id, ownerUid, farm.getId(), crop.getId(), farm.getName(), crop.getName(),
                request.getSeason().trim().toUpperCase(), request.getCultivatedArea(), request.getCultivatedAreaUnit().trim().toUpperCase(),
                request.getExpectedYield(), request.getYieldUnit().trim().toUpperCase(),
                request.getProductionCosts(), request.getSellingCosts(),
                totalProductionCost, totalSellingCost, totalCost,
                productionCostPerUnit, totalCostPerUnit, breakEvenSellingPrice,
                existing.getCreatedAt(), nowIso
        );

        Map<String, Object> data = mapResponseToMap(response);

        if (firestore != null) {
            try {
                firestore.collection("farmEconomics").document(id).set(data, SetOptions.merge()).get();
            } catch (Exception e) {
                logger.warn("Error updating farmEconomics in Firestore: {}", e.getMessage());
            }
        }

        inMemoryRecords.put(id, response);
        return response;
    }

    public void deleteEconomicRecord(String id, String ownerUid) {
        farmService.verifyFarmerRole(ownerUid);
        getEconomicRecordById(id, ownerUid); // verifies ownership & existence

        if (firestore != null) {
            try {
                firestore.collection("farmEconomics").document(id).delete().get();
            } catch (Exception e) {
                logger.warn("Error deleting farmEconomics from Firestore: {}", e.getMessage());
            }
        }

        inMemoryRecords.remove(id);
    }

    // ==========================================
    // PROFITABILITY EVALUATION & COMPARISON
    // ==========================================

    public FarmProfitabilityEvaluationResponse evaluateProfitability(String ownerUid, FarmProfitabilityEvaluationRequest req) {
        farmService.verifyFarmerRole(ownerUid);

        if (req == null) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_ERROR, "Evaluation request cannot be null.");
        }

        // If referencing saved economic record
        if (req.getEconomicRecordId() != null && !req.getEconomicRecordId().trim().isEmpty()) {
            FarmEconomicRecordResponse rec = getEconomicRecordById(req.getEconomicRecordId(), ownerUid);
            req.setFarmId(rec.getFarmId());
            req.setCropId(rec.getCropId());
            req.setSeason(rec.getSeason());
            req.setCultivatedArea(rec.getCultivatedArea());
            req.setCultivatedAreaUnit(rec.getCultivatedAreaUnit());
            req.setExpectedYield(rec.getExpectedYield());
            req.setYieldUnit(rec.getYieldUnit());
            req.setProductionCosts(rec.getProductionCosts());
            if (req.getTransportationCost() == null || req.getTransportationCost().compareTo(BigDecimal.ZERO) == 0) {
                req.setTransportationCost(rec.getSellingCosts().getTransportationCost());
            }
            if (req.getOtherSellingCosts() == null || req.getOtherSellingCosts().compareTo(BigDecimal.ZERO) == 0) {
                req.setOtherSellingCosts(rec.getSellingCosts().getOtherSellingCosts());
            }
        }

        // Basic validations
        if (req.getFarmId() == null || req.getFarmId().trim().isEmpty()) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_INVALID_FARM, "Farm ID is required.");
        }
        if (req.getCropId() == null || req.getCropId().trim().isEmpty()) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_INVALID_CROP, "Crop ID is required.");
        }
        if (req.getMarketId() == null || req.getMarketId().trim().isEmpty()) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_INVALID_MARKET, "Market ID is required.");
        }
        if (req.getExpectedYield() == null || req.getExpectedYield().compareTo(BigDecimal.ZERO) <= 0) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_ERROR, "Expected yield must be greater than zero.");
        }
        if (req.getYieldUnit() == null || req.getYieldUnit().trim().isEmpty()) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_ERROR, "Yield unit is required.");
        }

        // Validate Farm ownership
        FarmResponse farm;
        try {
            farm = farmService.getFarmById(req.getFarmId(), ownerUid);
        } catch (Exception e) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_INVALID_FARM, "Invalid or non-existent farm: " + req.getFarmId());
        }

        // Validate Crop
        CropResponse crop;
        try {
            crop = cropMasterService.getCropById(req.getCropId());
        } catch (Exception e) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_INVALID_CROP, "Invalid or non-existent crop: " + req.getCropId());
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
            return FarmProfitabilityEvaluationResponse.error(STATUS_INVALID_MARKET, "Invalid or non-existent market: " + req.getMarketId());
        }

        String mode = (req.getPriceMode() != null && !req.getPriceMode().trim().isEmpty())
                ? req.getPriceMode().trim().toUpperCase(Locale.ROOT)
                : DecisionSupportService.MODE_LATEST_AVAILABLE;

        if (DecisionSupportService.MODE_EXACT_DATE.equals(mode) && (req.getDate() == null || req.getDate().trim().isEmpty())) {
            return FarmProfitabilityEvaluationResponse.error(STATUS_ERROR, "Date is required when priceMode is EXACT_DATE.");
        }

        // Retrieve verified price using trusted MarketPriceService
        MarketPriceResponse priceRecord = null;
        if (DecisionSupportService.MODE_EXACT_DATE.equals(mode)) {
            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    null, null, req.getMarketId(), req.getCropId(), req.getDate().trim(),
                    MarketPriceService.QUALITY_VERIFIED, null, "priceDate", "desc", 1, 10, 10
            );
            if (!prices.isEmpty()) priceRecord = prices.get(0);
        } else {
            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    null, null, req.getMarketId(), req.getCropId(), null,
                    MarketPriceService.QUALITY_VERIFIED, null, "priceDate", "desc", 1, 10, 10
            );
            if (!prices.isEmpty()) priceRecord = prices.get(0);
        }

        if (priceRecord == null) {
            String noPriceMsg = DecisionSupportService.MODE_EXACT_DATE.equals(mode)
                    ? "No verified price is available for this crop and market for the selected date."
                    : "No verified price is available for this crop and market.";
            FarmProfitabilityEvaluationResponse resp = FarmProfitabilityEvaluationResponse.error(STATUS_NO_VERIFIED_PRICE, noPriceMsg);
            resp.setCrop(crop);
            resp.setFarm(farm);
            resp.setMarket(marketSummary);
            return resp;
        }

        // Unit compatibility check
        String priceUnit = priceRecord.getUnit() != null ? priceRecord.getUnit().trim() : "";
        String yieldUnit = req.getYieldUnit().trim();

        if (!yieldUnit.equalsIgnoreCase(priceUnit)) {
            FarmProfitabilityEvaluationResponse resp = FarmProfitabilityEvaluationResponse.error(
                    STATUS_UNIT_MISMATCH, "Price unit (" + priceUnit + ") and yield unit (" + yieldUnit + ") are incompatible."
            );
            resp.setCrop(crop);
            resp.setFarm(farm);
            resp.setMarket(marketSummary);
            resp.setPriceUnit(priceUnit);
            resp.setYieldUnit(yieldUnit);
            return resp;
        }

        // Extract selected price based on price basis
        String basis = (req.getPriceBasis() != null && !req.getPriceBasis().trim().isEmpty())
                ? req.getPriceBasis().trim().toUpperCase(Locale.ROOT)
                : DecisionSupportService.PRICE_BASIS_MODAL;

        double rawPrice;
        if (DecisionSupportService.PRICE_BASIS_MIN.equals(basis)) {
            rawPrice = priceRecord.getMinPrice();
        } else if (DecisionSupportService.PRICE_BASIS_MAX.equals(basis)) {
            rawPrice = priceRecord.getMaxPrice();
        } else {
            basis = DecisionSupportService.PRICE_BASIS_MODAL;
            rawPrice = priceRecord.getModalPrice();
        }

        SellingCostsDto sellingCosts = new SellingCostsDto(req.getTransportationCost(), req.getOtherSellingCosts());

        FarmEconomicsCalculationService.CalculationResult calc = calculationService.calculateProfitability(
                BigDecimal.valueOf(rawPrice),
                req.getExpectedYield(),
                req.getProductionCosts(),
                sellingCosts
        );

        String todayStr = LocalDate.now().toString();
        boolean isStale = priceRecord.getPriceDate() != null && priceRecord.getPriceDate().compareTo(todayStr) < 0;
        String staleMsg = isStale ? "Based on the latest available verified price from " + priceRecord.getPriceDate() + "." : null;

        FarmProfitabilityEvaluationResponse resp = new FarmProfitabilityEvaluationResponse();
        resp.setStatus(STATUS_SUCCESS);
        resp.setMessage("Farm profitability evaluation completed successfully.");
        resp.setCrop(crop);
        resp.setFarm(farm);
        resp.setMarket(marketSummary);
        resp.setSelectedPrice(BigDecimal.valueOf(rawPrice).setScale(2, java.math.RoundingMode.HALF_UP));
        resp.setPriceBasis(basis);
        resp.setPriceUnit(priceUnit);
        resp.setExpectedYield(req.getExpectedYield().setScale(2, java.math.RoundingMode.HALF_UP));
        resp.setYieldUnit(yieldUnit);

        resp.setGrossRevenue(calc.getGrossRevenue());
        resp.setTotalProductionCost(calc.getTotalProductionCost());
        resp.setTotalSellingCost(calc.getTotalSellingCost());
        resp.setTotalCost(calc.getTotalCost());
        resp.setEstimatedNetRealization(calc.getEstimatedNetRealization());
        resp.setEstimatedProfit(calc.getEstimatedProfit());
        resp.setProfitPerUnit(calc.getProfitPerUnit());
        resp.setProductionCostPerUnit(calc.getProductionCostPerUnit());
        resp.setTotalCostPerUnit(calc.getTotalCostPerUnit());
        resp.setBreakEvenSellingPrice(calc.getBreakEvenSellingPrice());
        resp.setRoi(calc.getRoi());
        resp.setProfitabilityStatus(calc.getProfitabilityStatus());

        resp.setCurrency(priceRecord.getCurrency() != null ? priceRecord.getCurrency() : "INR");
        resp.setPriceDate(priceRecord.getPriceDate());
        resp.setObservedAt(priceRecord.getObservedAt());
        resp.setSource(priceRecord.getSource());
        resp.setQualityStatus(priceRecord.getQualityStatus());
        resp.setStalePrice(isStale);
        resp.setStaleMessage(staleMsg);
        resp.setCostCompleteness(calc.getCostCompleteness());

        return resp;
    }

    public FarmMarketComparisonResponse compareMarkets(String ownerUid, FarmMarketComparisonRequest req) {
        farmService.verifyFarmerRole(ownerUid);

        if (req == null) {
            throw new IllegalArgumentException("Comparison request cannot be null.");
        }

        if (req.getEconomicRecordId() != null && !req.getEconomicRecordId().trim().isEmpty()) {
            FarmEconomicRecordResponse rec = getEconomicRecordById(req.getEconomicRecordId(), ownerUid);
            req.setFarmId(rec.getFarmId());
            req.setCropId(rec.getCropId());
            req.setSeason(rec.getSeason());
            req.setExpectedYield(rec.getExpectedYield());
            req.setYieldUnit(rec.getYieldUnit());
            req.setProductionCosts(rec.getProductionCosts());
        }

        if (req.getCropId() == null || req.getCropId().trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }
        if (req.getExpectedYield() == null || req.getExpectedYield().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expected yield must be greater than zero.");
        }
        if (req.getYieldUnit() == null || req.getYieldUnit().trim().isEmpty()) {
            throw new IllegalArgumentException("Yield unit is required.");
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

        List<FarmProfitabilityEvaluationResponse> evaluations = new ArrayList<>();
        for (MarketCostInputDto costInput : req.getMarkets()) {
            if (costInput.getMarketId() == null || costInput.getMarketId().trim().isEmpty()) {
                continue;
            }

            FarmProfitabilityEvaluationRequest singleReq = new FarmProfitabilityEvaluationRequest();
            singleReq.setFarmId(req.getFarmId());
            singleReq.setCropId(req.getCropId());
            singleReq.setSeason(req.getSeason());
            singleReq.setExpectedYield(req.getExpectedYield());
            singleReq.setYieldUnit(req.getYieldUnit());
            singleReq.setProductionCosts(req.getProductionCosts());
            singleReq.setMarketId(costInput.getMarketId());
            singleReq.setPriceBasis(req.getPriceBasis());
            singleReq.setPriceMode(req.getPriceMode());
            singleReq.setDate(req.getDate());
            singleReq.setTransportationCost(BigDecimal.valueOf(costInput.getTransportationCost()));
            singleReq.setOtherSellingCosts(BigDecimal.valueOf(costInput.getOtherSellingCosts()));

            FarmProfitabilityEvaluationResponse eval = evaluateProfitability(ownerUid, singleReq);
            evaluations.add(eval);
        }

        List<FarmProfitabilityEvaluationResponse> successful = evaluations.stream()
                .filter(e -> STATUS_SUCCESS.equals(e.getStatus()))
                .collect(Collectors.toList());

        // Sort by estimatedProfit descending
        successful.sort((a, b) -> b.getEstimatedProfit().compareTo(a.getEstimatedProfit()));

        List<FarmProfitabilityEvaluationResponse> sortedEvaluations = new ArrayList<>(successful);
        for (FarmProfitabilityEvaluationResponse e : evaluations) {
            if (!STATUS_SUCCESS.equals(e.getStatus())) {
                sortedEvaluations.add(e);
            }
        }

        FarmProfitabilityEvaluationResponse topMarket = !successful.isEmpty() ? successful.get(0) : null;
        String rankingSummary = topMarket != null
                ? "Highest estimated profit: " + topMarket.getMarket().getName() + " (₹" + topMarket.getEstimatedProfit() + ")"
                : "No valid verified market prices were available for comparison.";

        return new FarmMarketComparisonResponse(
                crop,
                req.getExpectedYield(),
                req.getYieldUnit(),
                req.getPriceBasis(),
                req.getPriceMode(),
                req.getDate(),
                sortedEvaluations,
                topMarket,
                rankingSummary
        );
    }

    // ==========================================
    // HELPERS & VALIDATION
    // ==========================================

    private void validateRecordRequest(String ownerUid, FarmEconomicRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Economic record request cannot be null.");
        }
        if (request.getFarmId() == null || request.getFarmId().trim().isEmpty()) {
            throw new IllegalArgumentException("Farm ID is required.");
        }
        if (request.getCropId() == null || request.getCropId().trim().isEmpty()) {
            throw new IllegalArgumentException("Crop ID is required.");
        }
        if (request.getSeason() == null || !VALID_SEASONS.contains(request.getSeason().trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid season: " + request.getSeason() + ". Valid seasons: " + VALID_SEASONS);
        }
        if (request.getCultivatedArea() == null || request.getCultivatedArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cultivated area must be greater than zero.");
        }
        if (request.getCultivatedAreaUnit() == null || !VALID_AREA_UNITS.contains(request.getCultivatedAreaUnit().trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid cultivated area unit: " + request.getCultivatedAreaUnit() + ". Valid units: " + VALID_AREA_UNITS);
        }
        if (request.getExpectedYield() == null || request.getExpectedYield().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expected yield must be greater than zero.");
        }
        if (request.getYieldUnit() == null || !VALID_YIELD_UNITS.contains(request.getYieldUnit().trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid yield unit: " + request.getYieldUnit() + ". Valid units: " + VALID_YIELD_UNITS);
        }
    }

    private FarmEconomicRecordResponse buildResponse(
            String id, String ownerUid, String farmId, String cropId, String farmName, String cropName,
            String season, BigDecimal cultivatedArea, String cultivatedAreaUnit,
            BigDecimal expectedYield, String yieldUnit,
            List<ProductionCostItemDto> productionCosts, SellingCostsDto sellingCosts,
            BigDecimal totalProductionCost, BigDecimal totalSellingCost, BigDecimal totalCost,
            BigDecimal productionCostPerUnit, BigDecimal totalCostPerUnit, BigDecimal breakEvenSellingPrice,
            String createdAt, String updatedAt
    ) {
        FarmEconomicRecordResponse r = new FarmEconomicRecordResponse();
        r.setId(id);
        r.setOwnerUid(ownerUid);
        r.setFarmId(farmId);
        r.setCropId(cropId);
        r.setFarmName(farmName);
        r.setCropName(cropName);
        r.setSeason(season);
        r.setCultivatedArea(cultivatedArea);
        r.setCultivatedAreaUnit(cultivatedAreaUnit);
        r.setExpectedYield(expectedYield);
        r.setYieldUnit(yieldUnit);
        r.setProductionCosts(productionCosts);
        r.setSellingCosts(sellingCosts);
        r.setTotalProductionCost(totalProductionCost);
        r.setTotalSellingCost(totalSellingCost);
        r.setTotalCost(totalCost);
        r.setProductionCostPerUnit(productionCostPerUnit);
        r.setTotalCostPerUnit(totalCostPerUnit);
        r.setBreakEvenSellingPrice(breakEvenSellingPrice);
        r.setCreatedAt(createdAt);
        r.setUpdatedAt(updatedAt);
        return r;
    }

    @SuppressWarnings("unchecked")
    private FarmEconomicRecordResponse mapDocToRecordResponse(DocumentSnapshot doc) {
        FarmEconomicRecordResponse r = new FarmEconomicRecordResponse();
        r.setId(doc.getId());
        r.setOwnerUid(doc.getString("ownerUid"));
        r.setFarmId(doc.getString("farmId"));
        r.setCropId(doc.getString("cropId"));
        r.setFarmName(doc.getString("farmName"));
        r.setCropName(doc.getString("cropName"));
        r.setSeason(doc.getString("season"));

        Double area = doc.getDouble("cultivatedArea");
        r.setCultivatedArea(area != null ? BigDecimal.valueOf(area) : BigDecimal.ZERO);
        r.setCultivatedAreaUnit(doc.getString("cultivatedAreaUnit"));

        Double y = doc.getDouble("expectedYield");
        r.setExpectedYield(y != null ? BigDecimal.valueOf(y) : BigDecimal.ZERO);
        r.setYieldUnit(doc.getString("yieldUnit"));

        List<Map<String, Object>> costs = (List<Map<String, Object>>) doc.get("productionCosts");
        List<ProductionCostItemDto> itemList = new ArrayList<>();
        if (costs != null) {
            for (Map<String, Object> m : costs) {
                Object amt = m.get("amount");
                BigDecimal bAmt = amt instanceof Number ? BigDecimal.valueOf(((Number) amt).doubleValue()) : BigDecimal.ZERO;
                itemList.add(new ProductionCostItemDto(
                        (String) m.get("id"),
                        (String) m.get("category"),
                        (String) m.get("description"),
                        bAmt,
                        (String) m.get("currency")
                ));
            }
        }
        r.setProductionCosts(itemList);

        Map<String, Object> sellMap = (Map<String, Object>) doc.get("sellingCosts");
        if (sellMap != null) {
            Object tAmt = sellMap.get("transportationCost");
            Object oAmt = sellMap.get("otherSellingCosts");
            BigDecimal tCost = tAmt instanceof Number ? BigDecimal.valueOf(((Number) tAmt).doubleValue()) : BigDecimal.ZERO;
            BigDecimal oCost = oAmt instanceof Number ? BigDecimal.valueOf(((Number) oAmt).doubleValue()) : BigDecimal.ZERO;
            r.setSellingCosts(new SellingCostsDto(tCost, oCost));
        }

        Double tProd = doc.getDouble("totalProductionCost");
        Double tSell = doc.getDouble("totalSellingCost");
        Double tCost = doc.getDouble("totalCost");
        Double pCostPerUnit = doc.getDouble("productionCostPerUnit");
        Double tCostPerUnit = doc.getDouble("totalCostPerUnit");
        Double bEven = doc.getDouble("breakEvenSellingPrice");

        r.setTotalProductionCost(tProd != null ? BigDecimal.valueOf(tProd) : BigDecimal.ZERO);
        r.setTotalSellingCost(tSell != null ? BigDecimal.valueOf(tSell) : BigDecimal.ZERO);
        r.setTotalCost(tCost != null ? BigDecimal.valueOf(tCost) : BigDecimal.ZERO);
        r.setProductionCostPerUnit(pCostPerUnit != null ? BigDecimal.valueOf(pCostPerUnit) : BigDecimal.ZERO);
        r.setTotalCostPerUnit(tCostPerUnit != null ? BigDecimal.valueOf(tCostPerUnit) : BigDecimal.ZERO);
        r.setBreakEvenSellingPrice(bEven != null ? BigDecimal.valueOf(bEven) : BigDecimal.ZERO);

        r.setCreatedAt(doc.getString("createdAt"));
        r.setUpdatedAt(doc.getString("updatedAt"));
        return r;
    }

    private Map<String, Object> mapResponseToMap(FarmEconomicRecordResponse r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("ownerUid", r.getOwnerUid());
        m.put("farmId", r.getFarmId());
        m.put("cropId", r.getCropId());
        m.put("farmName", r.getFarmName());
        m.put("cropName", r.getCropName());
        m.put("season", r.getSeason());
        m.put("cultivatedArea", r.getCultivatedArea() != null ? r.getCultivatedArea().doubleValue() : 0.0);
        m.put("cultivatedAreaUnit", r.getCultivatedAreaUnit());
        m.put("expectedYield", r.getExpectedYield() != null ? r.getExpectedYield().doubleValue() : 0.0);
        m.put("yieldUnit", r.getYieldUnit());

        List<Map<String, Object>> costsList = new ArrayList<>();
        if (r.getProductionCosts() != null) {
            for (ProductionCostItemDto item : r.getProductionCosts()) {
                Map<String, Object> cm = new HashMap<>();
                cm.put("id", item.getId());
                cm.put("category", item.getCategory());
                cm.put("description", item.getDescription());
                cm.put("amount", item.getAmount() != null ? item.getAmount().doubleValue() : 0.0);
                cm.put("currency", item.getCurrency());
                costsList.add(cm);
            }
        }
        m.put("productionCosts", costsList);

        Map<String, Object> sellMap = new HashMap<>();
        sellMap.put("transportationCost", r.getSellingCosts() != null && r.getSellingCosts().getTransportationCost() != null ? r.getSellingCosts().getTransportationCost().doubleValue() : 0.0);
        sellMap.put("otherSellingCosts", r.getSellingCosts() != null && r.getSellingCosts().getOtherSellingCosts() != null ? r.getSellingCosts().getOtherSellingCosts().doubleValue() : 0.0);
        m.put("sellingCosts", sellMap);

        m.put("totalProductionCost", r.getTotalProductionCost() != null ? r.getTotalProductionCost().doubleValue() : 0.0);
        m.put("totalSellingCost", r.getTotalSellingCost() != null ? r.getTotalSellingCost().doubleValue() : 0.0);
        m.put("totalCost", r.getTotalCost() != null ? r.getTotalCost().doubleValue() : 0.0);
        m.put("productionCostPerUnit", r.getProductionCostPerUnit() != null ? r.getProductionCostPerUnit().doubleValue() : 0.0);
        m.put("totalCostPerUnit", r.getTotalCostPerUnit() != null ? r.getTotalCostPerUnit().doubleValue() : 0.0);
        m.put("breakEvenSellingPrice", r.getBreakEvenSellingPrice() != null ? r.getBreakEvenSellingPrice().doubleValue() : 0.0);

        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }
}
