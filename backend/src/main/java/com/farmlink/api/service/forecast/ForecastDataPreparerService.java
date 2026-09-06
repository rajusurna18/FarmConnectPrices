package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.service.MarketPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ForecastDataPreparerService {

    private static final Logger log = LoggerFactory.getLogger(ForecastDataPreparerService.class);

    public List<MarketPriceSummaryResponse> prepareDataset(
            List<MarketPriceSummaryResponse> rawObservations,
            String cropId,
            String marketId
    ) {
        if (rawObservations == null || rawObservations.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Filter valid records
        List<MarketPriceSummaryResponse> filtered = rawObservations.stream()
                .filter(p -> p != null)
                .filter(p -> p.getPriceDate() != null && !p.getPriceDate().trim().isEmpty())
                .filter(p -> !MarketPriceService.QUALITY_REJECTED.equalsIgnoreCase(p.getQualityStatus()))
                .filter(p -> p.getModalPrice() > 0)
                .filter(p -> MarketPriceService.validatePriceRecord(p.getMinPrice(), p.getMaxPrice(), p.getModalPrice()))
                .filter(p -> {
                    if (cropId != null && !cropId.trim().isEmpty() && p.getCropId() != null) {
                        if (!p.getCropId().equalsIgnoreCase(cropId.trim())) return false;
                    }
                    if (marketId != null && !marketId.trim().isEmpty() && p.getMarketId() != null) {
                        if (!p.getMarketId().equalsIgnoreCase(marketId.trim())) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Handle duplicates by date: group by priceDate and select best record
        Map<String, MarketPriceSummaryResponse> bestByDate = new LinkedHashMap<>();
        for (MarketPriceSummaryResponse p : filtered) {
            String dateKey = p.getPriceDate().trim();
            if (!bestByDate.containsKey(dateKey)) {
                bestByDate.put(dateKey, p);
            } else {
                MarketPriceSummaryResponse existing = bestByDate.get(dateKey);
                // Prefer VERIFIED over UNVERIFIED
                boolean pVerified = MarketPriceService.QUALITY_VERIFIED.equalsIgnoreCase(p.getQualityStatus());
                boolean existVerified = MarketPriceService.QUALITY_VERIFIED.equalsIgnoreCase(existing.getQualityStatus());
                if (pVerified && !existVerified) {
                    bestByDate.put(dateKey, p);
                }
            }
        }

        // 3. Chronological sorting: oldest to newest (priceDate ASC)
        List<MarketPriceSummaryResponse> sorted = new ArrayList<>(bestByDate.values());
        sorted.sort(Comparator.comparing(MarketPriceSummaryResponse::getPriceDate));

        log.debug("Prepared forecasting dataset of {} valid observations for cropId={} marketId={}",
                sorted.size(), cropId, marketId);
        return sorted;
    }
}
