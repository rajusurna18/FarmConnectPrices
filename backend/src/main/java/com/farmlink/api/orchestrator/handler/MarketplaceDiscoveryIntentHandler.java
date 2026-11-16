package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.ListingPageResponse;
import com.farmlink.api.dto.ProductListingResponse;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.engine.EntityResolverService;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.service.ProductListingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Grounds MARKETPLACE_DISCOVERY queries using read-only public marketplace listings.
 * Strictly bounded to maximum 5 listings to satisfy Redis turn payload limits.
 */
@Component
public class MarketplaceDiscoveryIntentHandler implements IntentExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceDiscoveryIntentHandler.class);
    private static final int MAX_LISTINGS_LIMIT = 5;

    private final ProductListingService productListingService;
    private final EntityResolverService entityResolver;

    public MarketplaceDiscoveryIntentHandler(
            ProductListingService productListingService,
            EntityResolverService entityResolver
    ) {
        this.productListingService = productListingService;
        this.entityResolver = entityResolver;
    }

    @Override
    public boolean supports(AgriIntent intent) {
        return intent == AgriIntent.MARKETPLACE_DISCOVERY;
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
        String rawLocation = !entities.locations().isEmpty() ? entities.locations().get(0) : null;

        Optional<CropResponse> cropOpt = entityResolver.resolveCrop(rawCrop);
        String cropId = cropOpt.map(CropResponse::getId).orElse(null);
        String cropName = cropOpt.map(CropResponse::getName).orElse(rawCrop);

        try {
            ListingPageResponse page = productListingService.browsePublicListings(
                    cropId,
                    rawLocation,
                    null,
                    null,
                    null,
                    "newest",
                    0,
                    MAX_LISTINGS_LIMIT
            );

            List<ProductListingResponse> items = page != null && page.getItems() != null ? page.getItems() : Collections.emptyList();
            long totalElements = page != null ? page.getTotalElements() : 0;

            List<Map<String, Object>> listingSummaries = new ArrayList<>();
            for (ProductListingResponse item : items) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("listingId", item.getListingId());
                summary.put("cropName", item.getCropName());
                summary.put("variety", item.getDescription());
                summary.put("description", item.getDescription());
                summary.put("askingPrice", item.getAskingPrice());
                summary.put("priceUnit", item.getPriceUnit());
                summary.put("availableQuantity", item.getAvailableQuantity());
                summary.put("quantityUnit", item.getUnit());
                summary.put("qualityGrade", item.getQualityGrade());
                if (item.getLocation() != null) {
                    summary.put("state", item.getLocation().getState());
                    summary.put("district", item.getLocation().getDistrict());
                }
                listingSummaries.add(summary);
            }

            data.put("listings", listingSummaries);
            data.put("displayedCount", listingSummaries.size());
            data.put("totalActiveListings", totalElements);
            data.put("targetCrop", cropName != null ? cropName : "All Commodities");

            String summaryText = String.format("Found %d active marketplace listings (displaying top %d) for %s.",
                    totalElements,
                    listingSummaries.size(),
                    cropName != null ? cropName : "agricultural commodities");

            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.success(AgriIntent.MARKETPLACE_DISCOVERY, data, summaryText, latency);
        } catch (Exception e) {
            log.error("Failed to browse public marketplace listings: {}", e.getMessage(), e);
            warnings.add("Marketplace lookup error: " + e.getMessage());
            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.failure(AgriIntent.MARKETPLACE_DISCOVERY, warnings,
                    "Marketplace listing catalog temporarily unavailable.", latency);
        }
    }
}
