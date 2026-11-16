package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.MarketPriceResponse;
import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.dto.MarketTrendResponse;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.engine.EntityResolverService;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.service.MarketPriceService;
import com.farmlink.api.service.MarketTrendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Grounds MARKET_TREND_INQUIRY queries using MarketTrendService and MarketPriceService.
 */
@Component
public class MarketTrendIntentHandler implements IntentExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketTrendIntentHandler.class);

    private final MarketTrendService marketTrendService;
    private final MarketPriceService marketPriceService;
    private final EntityResolverService entityResolver;

    public MarketTrendIntentHandler(
            MarketTrendService marketTrendService,
            MarketPriceService marketPriceService,
            EntityResolverService entityResolver
    ) {
        this.marketTrendService = marketTrendService;
        this.marketPriceService = marketPriceService;
        this.entityResolver = entityResolver;
    }

    @Override
    public boolean supports(AgriIntent intent) {
        return intent == AgriIntent.MARKET_TREND_INQUIRY;
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

        Optional<CropResponse> cropOpt = entityResolver.resolveCrop(rawCrop);
        String cropId = cropOpt.map(CropResponse::getId).orElse(null);
        String cropName = cropOpt.map(CropResponse::getName).orElse(rawCrop != null ? rawCrop : "Unknown Commodity");
        data.put("cropName", cropName);
        if (cropId != null) data.put("cropId", cropId);

        Optional<MarketSummaryResponse> marketOpt = entityResolver.resolveMarket(rawLocation, null, rawMandi);
        String marketId = marketOpt.map(MarketSummaryResponse::getId).orElse(null);
        String marketName = marketOpt.map(MarketSummaryResponse::getName).orElse(rawMandi);
        if (marketName != null) data.put("marketName", marketName);
        if (marketId != null) data.put("marketId", marketId);

        try {
            MarketTrendResponse trend = marketTrendService.calculateTrend(
                    cropId,
                    marketId,
                    "30d",
                    null,
                    null,
                    MarketPriceService.UNIT_QUINTAL
            );

            if (trend != null) {
                Map<String, Object> trendDetails = new LinkedHashMap<>();
                trendDetails.put("trendDirection", trend.getTrendDirection());
                trendDetails.put("percentageChange", trend.getPercentageChange());
                trendDetails.put("absoluteChange", trend.getAbsoluteChange());
                trendDetails.put("observationCount", trend.getObservationCount());
                trendDetails.put("latestPrice", trend.getLatestPrice());
                trendDetails.put("avgPrice", trend.getAvgPrice());
                trendDetails.put("unit", trend.getUnit());
                trendDetails.put("dataQuality", trend.getDataQuality());
                trendDetails.put("latestObservationDate", trend.getLatestObservationDate());

                data.put("trend", trendDetails);

                String directionText = trend.getTrendDirection() != null ? trend.getTrendDirection() : "STABLE";
                double pct = trend.getPercentageChange() != null ? trend.getPercentageChange() : 0.0;
                String summary = String.format("Price trend for %s in %s over the last 30 days is %s (%.2f%% change across %d observations).",
                        cropName,
                        trend.getMarketName() != null ? trend.getMarketName() : (marketName != null ? marketName : "market"),
                        directionText,
                        pct,
                        trend.getObservationCount());

                long latency = System.currentTimeMillis() - start;
                return DomainExecutionResult.success(AgriIntent.MARKET_TREND_INQUIRY, data, summary, latency);
            } else {
                warnings.add("Insufficient historical price observations to calculate trend.");
                String summary = String.format("Insufficient data to establish a 30-day price trend for %s.", cropName);
                long latency = System.currentTimeMillis() - start;
                return DomainExecutionResult.partial(AgriIntent.MARKET_TREND_INQUIRY, data, warnings, summary, latency);
            }
        } catch (Exception e) {
            log.error("Failed to calculate market trend: {}", e.getMessage(), e);
            warnings.add("Market trend calculation error: " + e.getMessage());

            // Fallback: try fetching latest single price to provide partial value
            try {
                List<MarketPriceResponse> fallbackPrices = marketPriceService.getMarketPrices(
                        rawLocation, null, marketId, cropId, null,
                        MarketPriceService.QUALITY_VERIFIED, null, "priceDate", "desc", 1, 1, 1
                );
                if (fallbackPrices != null && !fallbackPrices.isEmpty()) {
                    MarketPriceResponse latest = fallbackPrices.get(0);
                    data.put("latestPrice", Map.of(
                            "modalPrice", latest.getModalPrice(),
                            "priceDate", latest.getPriceDate(),
                            "unit", latest.getUnit() != null ? latest.getUnit() : "QUINTAL"
                    ));
                    warnings.add("Historical trend analysis unavailable; current modal price provided instead.");
                }
            } catch (Exception ignored) {
            }

            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.partial(AgriIntent.MARKET_TREND_INQUIRY, data, warnings,
                    "Historical trend data temporarily unavailable.", latency);
        }
    }
}
