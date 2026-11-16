package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.FarmEconomicRecordResponse;
import com.farmlink.api.dto.FarmResponse;
import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.engine.EntityResolverService;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.exception.UnauthorizedEconomicsAccessException;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.service.FarmEconomicsService;
import com.farmlink.api.service.FarmService;
import com.farmlink.api.service.MarketPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Handles FARM_ECONOMICS_INQUIRY with strict caller ownership boundaries.
 * Restricted exclusively to FARMER role. All queries scoped to authenticatedUid.
 */
@Component
public class FarmEconomicsIntentHandler implements IntentExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(FarmEconomicsIntentHandler.class);

    private final FarmService farmService;
    private final FarmEconomicsService farmEconomicsService;
    private final MarketPriceService marketPriceService;
    private final EntityResolverService entityResolver;

    public FarmEconomicsIntentHandler(
            FarmService farmService,
            FarmEconomicsService farmEconomicsService,
            MarketPriceService marketPriceService,
            EntityResolverService entityResolver
    ) {
        this.farmService = farmService;
        this.farmEconomicsService = farmEconomicsService;
        this.marketPriceService = marketPriceService;
        this.entityResolver = entityResolver;
    }

    @Override
    public boolean supports(AgriIntent intent) {
        return intent == AgriIntent.FARM_ECONOMICS_INQUIRY;
    }

    @Override
    public DomainExecutionResult execute(
            RouterTurnResponse turn,
            ExtractedEntitiesDto entities,
            UserDomainRole callerRole,
            String authenticatedUid
    ) {
        long start = System.currentTimeMillis();

        // 1. Independent Role Verification: Must be a FARMER
        if (callerRole != UserDomainRole.FARMER) {
            throw new UnauthorizedEconomicsAccessException(
                    "Access denied: Farm economics inquiry is restricted to farmers. Current role: " + callerRole
            );
        }

        List<String> warnings = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();

        // 2. Fetch farms strictly for the authenticated UID
        List<FarmResponse> farms = farmService.getFarmsForOwner(authenticatedUid);
        if (farms == null || farms.isEmpty()) {
            warnings.add("No farms currently registered for your account.");
            String summary = "You do not have any registered farms in FarmLink. Please register your farm profile to view economics and break-even calculations.";
            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.partial(AgriIntent.FARM_ECONOMICS_INQUIRY, data, warnings, summary, latency);
        }

        // 3. Fetch economic records strictly for the authenticated UID
        List<FarmEconomicRecordResponse> records = farmEconomicsService.getEconomicRecordsForOwner(authenticatedUid);
        if (records == null || records.isEmpty()) {
            warnings.add("No farm economic records recorded for your account.");
            data.put("farmCount", farms.size());
            data.put("farmName", farms.get(0).getName());
            String summary = String.format("Found farm '%s' registered to your account, but no crop economic budgets have been recorded yet.", farms.get(0).getName());
            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.partial(AgriIntent.FARM_ECONOMICS_INQUIRY, data, warnings, summary, latency);
        }

        // Match requested crop if specified, or take most recent record
        String rawCrop = !entities.crops().isEmpty() ? entities.crops().get(0) : null;
        Optional<CropResponse> cropOpt = entityResolver.resolveCrop(rawCrop);
        String targetCropId = cropOpt.map(CropResponse::getId).orElse(null);

        FarmEconomicRecordResponse selectedRecord = null;
        if (targetCropId != null) {
            for (FarmEconomicRecordResponse r : records) {
                if (targetCropId.equalsIgnoreCase(r.getCropId())) {
                    selectedRecord = r;
                    break;
                }
            }
        }
        if (selectedRecord == null) {
            selectedRecord = records.get(0);
        }

        Map<String, Object> recordDetails = new LinkedHashMap<>();
        recordDetails.put("farmName", selectedRecord.getFarmName());
        recordDetails.put("cropName", selectedRecord.getCropName());
        recordDetails.put("cultivatedArea", selectedRecord.getCultivatedArea());
        recordDetails.put("cultivatedAreaUnit", selectedRecord.getCultivatedAreaUnit());
        recordDetails.put("expectedYield", selectedRecord.getExpectedYield());
        recordDetails.put("yieldUnit", selectedRecord.getYieldUnit());
        recordDetails.put("totalProductionCost", selectedRecord.getTotalProductionCost());
        recordDetails.put("totalSellingCost", selectedRecord.getTotalSellingCost());
        recordDetails.put("totalCost", selectedRecord.getTotalCost());
        recordDetails.put("totalCostPerUnit", selectedRecord.getTotalCostPerUnit());
        recordDetails.put("breakEvenSellingPrice", selectedRecord.getBreakEvenSellingPrice());

        data.put("economicRecord", recordDetails);

        // Enrich with current market price comparison if crop is known
        try {
            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    null, null, null, selectedRecord.getCropId(), null,
                    MarketPriceService.QUALITY_VERIFIED, selectedRecord.getYieldUnit(), "priceDate", "desc", 1, 1, 1
            );
            if (prices != null && !prices.isEmpty()) {
                MarketPriceResponse latest = prices.get(0);
                double currentModal = latest.getModalPrice();
                BigDecimal breakEven = selectedRecord.getBreakEvenSellingPrice();
                if (breakEven != null && breakEven.compareTo(BigDecimal.ZERO) > 0) {
                    double marginPerUnit = currentModal - breakEven.doubleValue();
                    double marginPct = (marginPerUnit / breakEven.doubleValue()) * 100.0;
                    data.put("currentMarketPrice", currentModal);
                    data.put("projectedMarginPerUnit", marginPerUnit);
                    data.put("projectedMarginPercent", marginPct);
                }
            }
        } catch (Exception e) {
            log.debug("Market price comparison skipped for economics: {}", e.getMessage());
        }

        String summary = String.format("Farm economics for %s on farm '%s': Total cost is ₹%s (₹%s per %s). Break-even selling price is ₹%s per %s.",
                selectedRecord.getCropName(),
                selectedRecord.getFarmName(),
                selectedRecord.getTotalCost(),
                selectedRecord.getTotalCostPerUnit(),
                selectedRecord.getYieldUnit(),
                selectedRecord.getBreakEvenSellingPrice(),
                selectedRecord.getYieldUnit());

        long latency = System.currentTimeMillis() - start;
        return DomainExecutionResult.success(AgriIntent.FARM_ECONOMICS_INQUIRY, data, summary, latency);
    }
}
