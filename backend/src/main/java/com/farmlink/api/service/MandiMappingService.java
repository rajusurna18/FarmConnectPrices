package com.farmlink.api.service;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MandiMappingService {

    private static final Logger logger = LoggerFactory.getLogger(MandiMappingService.class);

    private final MarketService marketService;
    private final CropMasterService cropMasterService;

    // Static commodity synonyms mapping to crop IDs
    private static final Map<String, String> COMMODITY_SYNONYMS = new HashMap<>();

    static {
        // Chilli
        COMMODITY_SYNONYMS.put("chilli", "crop-chilli");
        COMMODITY_SYNONYMS.put("chilli red", "crop-chilli");
        COMMODITY_SYNONYMS.put("red chilli", "crop-chilli");
        COMMODITY_SYNONYMS.put("chillies(red)", "crop-chilli");
        COMMODITY_SYNONYMS.put("chillies", "crop-chilli");

        // Rice / Paddy
        COMMODITY_SYNONYMS.put("paddy", "crop-paddy");
        COMMODITY_SYNONYMS.put("paddy(dhan)", "crop-paddy");
        COMMODITY_SYNONYMS.put("rice", "crop-paddy");

        // Wheat
        COMMODITY_SYNONYMS.put("wheat", "crop-wheat");

        // Maize
        COMMODITY_SYNONYMS.put("maize", "crop-maize");

        // Cotton
        COMMODITY_SYNONYMS.put("cotton", "crop-cotton");
        COMMODITY_SYNONYMS.put("cotton(ginned)", "crop-cotton");
        COMMODITY_SYNONYMS.put("cotton(unginned)", "crop-cotton");

        // Tomato
        COMMODITY_SYNONYMS.put("tomato", "crop-tomato");

        // Onion
        COMMODITY_SYNONYMS.put("onion", "crop-onion");

        // Groundnut
        COMMODITY_SYNONYMS.put("groundnut", "crop-groundnut");
        COMMODITY_SYNONYMS.put("groundnut (split)", "crop-groundnut");

        // Turmeric
        COMMODITY_SYNONYMS.put("turmeric", "crop-turmeric");

        // Sugarcane
        COMMODITY_SYNONYMS.put("sugarcane", "crop-sugarcane");

        // Pulses / Red Gram
        COMMODITY_SYNONYMS.put("arhar (tur/red gram)", "crop-pulses");
        COMMODITY_SYNONYMS.put("arhar", "crop-pulses");
        COMMODITY_SYNONYMS.put("tur", "crop-pulses");
        COMMODITY_SYNONYMS.put("red gram", "crop-pulses");

        // Mango
        COMMODITY_SYNONYMS.put("mango", "crop-mango");
    }

    public MandiMappingService(MarketService marketService, CropMasterService cropMasterService) {
        this.marketService = marketService;
        this.cropMasterService = cropMasterService;
    }

    /**
     * Maps external state, district, and market name to an existing Module 06 Market.
     * Does NOT create or modify markets automatically.
     */
    public Optional<MarketSummaryResponse> mapMarket(String rawState, String rawDistrict, String rawMarketName) {
        if (rawMarketName == null || rawMarketName.trim().isEmpty()) {
            return Optional.empty();
        }

        List<MarketSummaryResponse> allMarkets = marketService.getMarkets(null, null, null, null, null, null, 100);
        String normMarket = rawMarketName.trim().toLowerCase(Locale.ROOT);
        String normDistrict = rawDistrict != null ? rawDistrict.trim().toLowerCase(Locale.ROOT) : "";
        String normState = rawState != null ? rawState.trim().toLowerCase(Locale.ROOT) : "";

        // 1. Direct name match
        for (MarketSummaryResponse m : allMarkets) {
            String mName = m.getName().toLowerCase(Locale.ROOT);
            if (mName.equalsIgnoreCase(normMarket) || mName.contains(normMarket) || normMarket.contains(mName)) {
                return Optional.of(m);
            }
        }

        // 2. District + keyword matching
        for (MarketSummaryResponse m : allMarkets) {
            String mDist = m.getDistrict() != null ? m.getDistrict().toLowerCase(Locale.ROOT) : "";
            String mState = m.getState() != null ? m.getState().toLowerCase(Locale.ROOT) : "";

            if (!normDistrict.isEmpty() && mDist.contains(normDistrict)) {
                return Optional.of(m);
            }
            if (!normState.isEmpty() && mState.contains(normState) && normMarket.contains(mDist)) {
                return Optional.of(m);
            }
        }

        // 3. Known fallback maps for major mandis
        if (normMarket.contains("guntur") || normDistrict.contains("guntur")) {
            return findMarketById(allMarkets, "mkt-guntur-mandi");
        }
        if (normMarket.contains("warangal") || normMarket.contains("enumamula") || normDistrict.contains("warangal")) {
            return findMarketById(allMarkets, "mkt-enumamula-warangal");
        }
        if (normMarket.contains("malakpet") || normMarket.contains("hyderabad") || normDistrict.contains("hyderabad")) {
            return findMarketById(allMarkets, "mkt-malakpet-hyderabad");
        }
        if (normMarket.contains("devanahalli") || normMarket.contains("bengaluru") || normDistrict.contains("bengaluru")) {
            return findMarketById(allMarkets, "mkt-devanahalli-bengaluru");
        }

        logger.debug("Unmapped external market: State='{}', District='{}', Market='{}'", rawState, rawDistrict, rawMarketName);
        return Optional.empty();
    }

    /**
     * Maps external commodity string to an existing Module 05 Crop Response.
     * Does NOT create crops automatically when commodity mapping is uncertain.
     */
    public Optional<CropResponse> mapCrop(String rawCommodity) {
        if (rawCommodity == null || rawCommodity.trim().isEmpty()) {
            return Optional.empty();
        }

        String normCommodity = rawCommodity.trim().toLowerCase(Locale.ROOT);

        // Check synonyms map
        String targetCropId = COMMODITY_SYNONYMS.get(normCommodity);
        if (targetCropId == null) {
            for (Map.Entry<String, String> entry : COMMODITY_SYNONYMS.entrySet()) {
                if (normCommodity.contains(entry.getKey())) {
                    targetCropId = entry.getValue();
                    break;
                }
            }
        }

        if (targetCropId != null) {
            CropResponse crop = cropMasterService.getCropById(targetCropId);
            if (crop != null) {
                return Optional.of(crop);
            }
        }

        // Direct matching against all crops
        List<CropResponse> allCrops = cropMasterService.getAllCrops();
        for (CropResponse crop : allCrops) {
            String cName = crop.getName().toLowerCase(Locale.ROOT);
            if (cName.contains(normCommodity) || normCommodity.contains(cName)) {
                return Optional.of(crop);
            }
        }

        logger.debug("Unmapped external commodity: '{}'", rawCommodity);
        return Optional.empty();
    }

    private Optional<MarketSummaryResponse> findMarketById(List<MarketSummaryResponse> list, String id) {
        return list.stream().filter(m -> m.getId().equalsIgnoreCase(id)).findFirst();
    }
}
