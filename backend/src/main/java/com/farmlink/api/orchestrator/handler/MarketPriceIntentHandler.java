package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.engine.EntityResolverService;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.service.MarketPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Grounds MARKET_PRICE_INQUIRY queries using verified observations from MarketPriceService.
 */
@Component
public class MarketPriceIntentHandler implements IntentExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceIntentHandler.class);

    private final MarketPriceService marketPriceService;
    private final EntityResolverService entityResolver;

    public MarketPriceIntentHandler(
            MarketPriceService marketPriceService,
            EntityResolverService entityResolver
    ) {
        this.marketPriceService = marketPriceService;
        this.entityResolver = entityResolver;
    }

    @Override
    public boolean supports(AgriIntent intent) {
        return intent == AgriIntent.MARKET_PRICE_INQUIRY;
    }

    @Override
    public DomainExecutionResult execute(
            RouterTurnResponse turn,
            ExtractedEntitiesDto entities,
            UserDomainRole callerRole,
            String authenticatedUid
    ) {
        long start = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();

        String rawCrop = !entities.crops().isEmpty() ? entities.crops().get(0) : null;
        String rawMandi = !entities.mandis().isEmpty() ? entities.mandis().get(0) : null;
        String rawLocation = !entities.locations().isEmpty() ? entities.locations().get(0) : null;

        // Resolve Crop
        Optional<CropResponse> cropOpt = entityResolver.resolveCrop(rawCrop);
        String cropId = cropOpt.map(CropResponse::getId).orElse(null);
        String cropName = cropOpt.map(CropResponse::getName).orElse(rawCrop != null ? rawCrop : "Unknown Commodity");
        data.put("cropName", cropName);
        if (cropId != null) {
            data.put("cropId", cropId);
        } else if (rawCrop != null) {
            warnings.add("Could not map crop '" + rawCrop + "' to a canonical commodity ID. Searching broadly.");
        }

        // Resolve Mandi / Location
        Optional<MarketSummaryResponse> marketOpt = entityResolver.resolveMarket(rawLocation, null, rawMandi);
        String marketId = marketOpt.map(MarketSummaryResponse::getId).orElse(null);
        String marketName = marketOpt.map(MarketSummaryResponse::getName).orElse(rawMandi);
        if (marketName != null) {
            data.put("marketName", marketName);
        }
        if (marketId != null) {
            data.put("marketId", marketId);
        }

        try {
            // Fetch verified prices (bounded to 10 observations to respect payload limits)
            List<MarketPriceResponse> prices = marketPriceService.getMarketPrices(
                    rawLocation,
                    null,
                    marketId,
                    cropId,
                    null,
                    MarketPriceService.QUALITY_VERIFIED,
                    null,
                    "priceDate",
                    "desc",
                    1,
                    10,
                    10
            );

            if (prices != null && !prices.isEmpty()) {
                MarketPriceResponse latest = prices.get(0);
                Map<String, Object> latestDetails = new LinkedHashMap<>();
                latestDetails.put("modalPrice", latest.getModalPrice());
                latestDetails.put("minPrice", latest.getMinPrice());
                latestDetails.put("maxPrice", latest.getMaxPrice());
                latestDetails.put("priceDate", latest.getPriceDate());
                latestDetails.put("priceUnit", latest.getUnit() != null ? latest.getUnit() : "QUINTAL");
                latestDetails.put("currency", latest.getCurrency() != null ? latest.getCurrency() : "INR");
                latestDetails.put("marketName", latest.getMarket() != null ? latest.getMarket().getName() : marketName);
                latestDetails.put("state", latest.getMarket() != null ? latest.getMarket().getState() : rawLocation);
                latestDetails.put("qualityStatus", latest.getQualityStatus());

                data.put("latestPrice", latestDetails);
                data.put("observationCount", prices.size());

                String summary = String.format("Verified modal price for %s at %s is ₹%.2f per %s (as of %s).",
                        cropName,
                        latestDetails.get("marketName"),
                        latest.getModalPrice(),
                        latestDetails.get("priceUnit"),
                        latest.getPriceDate());

                long latency = System.currentTimeMillis() - start;
                return warnings.isEmpty()
                        ? DomainExecutionResult.success(AgriIntent.MARKET_PRICE_INQUIRY, data, summary, latency)
                        : DomainExecutionResult.partial(AgriIntent.MARKET_PRICE_INQUIRY, data, warnings, summary, latency);
            } else {
                warnings.add("No verified market price records found matching criteria.");
                String summary = String.format("No verified market price observations found for %s in %s.",
                        cropName, marketName != null ? marketName : "the specified region");
                long latency = System.currentTimeMillis() - start;
                return DomainExecutionResult.partial(AgriIntent.MARKET_PRICE_INQUIRY, data, warnings, summary, latency);
            }
        } catch (Exception e) {
            log.error("Failed to query market prices: {}", e.getMessage(), e);
            warnings.add("Market price service lookup error: " + e.getMessage());
            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.failure(AgriIntent.MARKET_PRICE_INQUIRY, warnings,
                    "Failed to retrieve market prices due to service latency or unavailability.", latency);
        }
    }
}
