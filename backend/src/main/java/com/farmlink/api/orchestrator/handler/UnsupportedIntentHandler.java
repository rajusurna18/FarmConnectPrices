package com.farmlink.api.orchestrator.handler;

import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles WEATHER_INQUIRY (unsupported domain service) and UNSUPPORTED_OR_UNCLEAR
 * with deterministic fail-safe advisories. Prevents fabricating weather telemetry.
 */
@Component
public class UnsupportedIntentHandler implements IntentExecutionHandler {

    public static final String WEATHER_UNSUPPORTED_STATEMENT =
            "Real-time meteorological telemetry and live rainfall forecasts are not integrated into this system. " +
            "For verified weather observations, localized rainfall forecasts, and agrometeorological advisories, " +
            "please consult the official India Meteorological Department (IMD) / Mausam portal (mausam.imd.gov.in) " +
            "or the Gramin Krishi Mausam Sewa (GKMS) extension advisory.";

    @Override
    public boolean supports(AgriIntent intent) {
        return intent == AgriIntent.WEATHER_INQUIRY || intent == AgriIntent.UNSUPPORTED_OR_UNCLEAR;
    }

    @Override
    public DomainExecutionResult execute(
            RouterTurnResponse turn,
            ExtractedEntitiesDto entities,
            UserDomainRole callerRole,
            String authenticatedUid
    ) {
        long start = System.currentTimeMillis();
        Map<String, Object> data = new LinkedHashMap<>();

        if (turn.intent() == AgriIntent.WEATHER_INQUIRY) {
            data.put("status", "UNSUPPORTED_CAPABILITY");
            data.put("message", WEATHER_UNSUPPORTED_STATEMENT);
            data.put("officialPortal", "https://mausam.imd.gov.in");

            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.success(AgriIntent.WEATHER_INQUIRY, data, WEATHER_UNSUPPORTED_STATEMENT, latency);
        } else {
            data.put("status", "CLARIFICATION_REQUIRED");
            data.put("clarification", turn.clarification());

            String summary = turn.clarification() != null && turn.clarification().prompt() != null
                    ? turn.clarification().prompt()
                    : "Please specify your agricultural inquiry with more details.";

            long latency = System.currentTimeMillis() - start;
            return DomainExecutionResult.success(AgriIntent.UNSUPPORTED_OR_UNCLEAR, data, summary, latency);
        }
    }
}
