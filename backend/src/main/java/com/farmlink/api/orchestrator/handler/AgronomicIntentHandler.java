package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.dto.CropResponse;
import com.farmlink.api.dto.forecast.ForecastRequest;
import com.farmlink.api.dto.forecast.ForecastResponse;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.engine.EntityResolverService;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import com.farmlink.api.service.forecast.ForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handles agronomic inquiry intents within the frozen first-release grounding boundary.
 * Grounds crop botanical identity and general extension guidelines.
 * Strictly prevents inventing chemical fertilizer/pesticide dosages or laboratory soil test prescriptions.
 */
@Component
public class AgronomicIntentHandler implements IntentExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(AgronomicIntentHandler.class);

    private static final Set<AgriIntent> SUPPORTED_INTENTS = Set.of(
            AgriIntent.CROP_SUITABILITY,
            AgriIntent.CROP_SELECTION,
            AgriIntent.SOIL_MANAGEMENT,
            AgriIntent.FERTILIZER_NUTRIENT,
            AgriIntent.IRRIGATION_MANAGEMENT,
            AgriIntent.CROP_CULTIVATION_PRACTICES,
            AgriIntent.CROP_ROTATION,
            AgriIntent.HARVEST_POST_HARVEST,
            AgriIntent.GENERAL_AGRI_KNOWLEDGE
    );

    public static final String GENERAL_EXTENSION_DISCLAIMER =
            "Recommended agronomic practices are general agricultural extension guidelines. " +
            "Please consult your local Krishi Vigyan Kendra (KVK) or State Agricultural University extension officer " +
            "for customized soil-test-based fertilizer dosages and plant protection prescriptions.";

    private final EntityResolverService entityResolver;
    private final ForecastService forecastService;

    public AgronomicIntentHandler(
            EntityResolverService entityResolver,
            @Autowired(required = false) ForecastService forecastService
    ) {
        this.entityResolver = entityResolver;
        this.forecastService = forecastService;
    }

    @Override
    public boolean supports(AgriIntent intent) {
        return SUPPORTED_INTENTS.contains(intent);
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
        String cropName = cropOpt.map(CropResponse::getName).orElse(rawCrop != null ? rawCrop : "Agricultural Commodity");
        String scientificName = cropOpt.map(CropResponse::getScientificName).orElse("");
        String category = cropOpt.map(CropResponse::getCategory).orElse("AGRICULTURAL_COMMODITY");

        data.put("cropName", cropName);
        data.put("scientificName", scientificName);
        data.put("category", category);
        data.put("verifiedInMaster", cropOpt.isPresent());
        data.put("disclaimer", GENERAL_EXTENSION_DISCLAIMER);

        if (rawLocation != null) {
            data.put("location", rawLocation);
        }

        // Special handling for CROP_SELECTION: if market forecast is available, enrich with price projection
        if (turn.intent() == AgriIntent.CROP_SELECTION && forecastService != null && cropOpt.isPresent()) {
            String cropId = cropOpt.get().getId();
            String rawMandi = !entities.mandis().isEmpty() ? entities.mandis().get(0) : null;
            var marketOpt = entityResolver.resolveMarket(rawLocation, null, rawMandi);
            if (marketOpt.isPresent()) {
                String marketId = marketOpt.get().getId();
                try {
                    ForecastRequest forecastReq = new ForecastRequest();
                    forecastReq.setCropId(cropId);
                    forecastReq.setMarketId(marketId);
                    forecastReq.setHorizon("7_DAYS");
                    ForecastResponse forecast = forecastService.getPriceForecast(forecastReq);
                    if (forecast != null && forecast.getForecastPrice() != null) {
                        data.put("forecastPrice", forecast.getForecastPrice());
                        data.put("forecastDirection", forecast.getDirection() != null ? forecast.getDirection().name() : "STABLE");
                        data.put("forecastConfidence", forecast.getConfidence() != null ? forecast.getConfidence().name() : "MEDIUM");
                    }
                } catch (Exception e) {
                    log.debug("Forecast lookup skipped during crop selection: {}", e.getMessage());
                }
            }
        }

        String summary = String.format("Grounded agronomic profile for %s (%s, %s). %s",
                cropName,
                !scientificName.isBlank() ? scientificName : "Standard variety",
                category,
                GENERAL_EXTENSION_DISCLAIMER);

        long latency = System.currentTimeMillis() - start;
        return DomainExecutionResult.success(turn.intent(), data, summary, latency);
    }
}
