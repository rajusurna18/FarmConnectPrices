package com.farmlink.api.orchestrator.engine;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.LocationMasterResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.service.CropMasterService;
import com.farmlink.api.service.LocationMasterService;
import com.farmlink.api.service.MandiMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves extracted informal entity strings (crops, mandis, locations)
 * to canonical repository objects and database IDs.
 */
@Service
public class EntityResolverService {

    private static final Logger log = LoggerFactory.getLogger(EntityResolverService.class);

    private final CropMasterService cropMasterService;
    private final MandiMappingService mandiMappingService;
    private final LocationMasterService locationMasterService;

    public EntityResolverService(
            CropMasterService cropMasterService,
            MandiMappingService mandiMappingService,
            LocationMasterService locationMasterService
    ) {
        this.cropMasterService = cropMasterService;
        this.mandiMappingService = mandiMappingService;
        this.locationMasterService = locationMasterService;
    }

    /**
     * Resolves a raw crop name (or colloquial synonym) to a canonical CropResponse.
     */
    public Optional<CropResponse> resolveCrop(String rawCropName) {
        if (rawCropName == null || rawCropName.isBlank()) {
            return Optional.empty();
        }

        String trimmed = rawCropName.trim();

        // 1. Try MandiMappingService synonyms and cached crops
        try {
            Optional<CropResponse> mapped = mandiMappingService.mapCrop(trimmed);
            if (mapped.isPresent()) {
                return mapped;
            }
        } catch (Exception e) {
            log.warn("Error in MandiMappingService.mapCrop for '{}': {}", trimmed, e.getMessage());
        }

        // 2. Direct lookup in CropMasterService cache
        try {
            CropResponse direct = cropMasterService.getCropById(trimmed.toLowerCase(Locale.ROOT));
            if (direct != null) {
                return Optional.of(direct);
            }
        } catch (Exception e) {
            log.warn("Error in CropMasterService.getCropById for '{}': {}", trimmed, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Resolves raw market and location names to a canonical MarketSummaryResponse.
     */
    public Optional<MarketSummaryResponse> resolveMarket(String rawState, String rawDistrict, String rawMarketName) {
        if (rawMarketName == null || rawMarketName.isBlank()) {
            return Optional.empty();
        }

        try {
            return mandiMappingService.mapMarket(rawState, rawDistrict, rawMarketName.trim());
        } catch (Exception e) {
            log.warn("Error in MandiMappingService.mapMarket for state='{}', district='{}', market='{}': {}",
                    rawState, rawDistrict, rawMarketName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Resolves raw state and district strings to a canonical LocationMasterResponse.
     */
    public Optional<LocationMasterResponse> resolveLocation(String rawState, String rawDistrict) {
        if ((rawState == null || rawState.isBlank()) && (rawDistrict == null || rawDistrict.isBlank())) {
            return Optional.empty();
        }

        try {
            java.util.List<LocationMasterResponse> locations = locationMasterService.getAllLocations();
            if (locations == null || locations.isEmpty()) {
                return Optional.empty();
            }

            String targetState = rawState != null ? rawState.trim() : null;
            String targetDistrict = rawDistrict != null ? rawDistrict.trim() : null;

            // 1. Both state and district provided: match both (case-insensitive)
            if (targetState != null && !targetState.isBlank() && targetDistrict != null && !targetDistrict.isBlank()) {
                for (LocationMasterResponse loc : locations) {
                    if (loc.getState() != null && loc.getState().equalsIgnoreCase(targetState) &&
                            loc.getDistrict() != null && loc.getDistrict().equalsIgnoreCase(targetDistrict)) {
                        return Optional.of(loc);
                    }
                }
            }

            // 2. Only district provided: match district
            if (targetDistrict != null && !targetDistrict.isBlank()) {
                for (LocationMasterResponse loc : locations) {
                    if (loc.getDistrict() != null && loc.getDistrict().equalsIgnoreCase(targetDistrict)) {
                        return Optional.of(loc);
                    }
                }
            }

            // 3. Only state provided: match state
            if (targetState != null && !targetState.isBlank()) {
                for (LocationMasterResponse loc : locations) {
                    if (loc.getState() != null && loc.getState().equalsIgnoreCase(targetState)) {
                        return Optional.of(loc);
                    }
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error resolving location for state='{}', district='{}': {}",
                    rawState, rawDistrict, e.getMessage());
            return Optional.empty();
        }
    }
}
