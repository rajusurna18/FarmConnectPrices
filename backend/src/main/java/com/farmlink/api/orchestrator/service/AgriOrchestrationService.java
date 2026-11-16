package com.farmlink.api.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.dto.OrchestrationPayload;
import com.farmlink.api.orchestrator.engine.DomainGroundingEngine;
import com.farmlink.api.orchestrator.synthesis.DeterministicFallbackFormatter;
import com.farmlink.api.orchestrator.synthesis.LlmSynthesisService;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.security.UserDomainRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Core orchestration service for FarmLink AI Unit 3.
 * Coordinates domain grounding, LLM synthesis / deterministic fallback,
 * and enforces payload watermark bounds to comply with Redis Lua storage limits.
 */
@Service
public class AgriOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AgriOrchestrationService.class);

    // 48 KB safety watermark (Redis hard ceiling in Lua scripts is 65,536 bytes)
    public static final int MAX_PAYLOAD_WATERMARK_BYTES = 49152;
    public static final long ORCHESTRATION_BUDGET_MS = 9000L;
    public static final long MIN_LLM_BUDGET_MS = 4500L;

    private final DomainGroundingEngine groundingEngine;
    private final LlmSynthesisService synthesisService;
    private final DeterministicFallbackFormatter fallbackFormatter;
    private final ObjectMapper objectMapper;

    public AgriOrchestrationService(
            DomainGroundingEngine groundingEngine,
            LlmSynthesisService synthesisService,
            DeterministicFallbackFormatter fallbackFormatter,
            ObjectMapper objectMapper
    ) {
        this.groundingEngine = groundingEngine;
        this.synthesisService = synthesisService;
        this.fallbackFormatter = fallbackFormatter;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes downstream orchestration for an upstream routing turn.
     *
     * @param turn the upstream routing decision
     * @param query the user's natural language inquiry
     * @param callerRole the independently verified user domain role
     * @param authenticatedUid the authenticated Firebase caller UID
     * @return an orchestrated RouterTurnResponse containing synthesized text and domain facts
     */
    public RouterTurnResponse orchestrate(
            RouterTurnResponse turn,
            String query,
            UserDomainRole callerRole,
            String authenticatedUid
    ) {
        long start = System.currentTimeMillis();

        // 1. Bypass check: If clarification is required or intent is unsupported, skip grounding
        if (turn.clarification() != null && turn.clarification().required()) {
            return turn;
        }
        if (turn.intent() == AgriIntent.UNSUPPORTED_OR_UNCLEAR) {
            return turn;
        }

        // 2. Domain Grounding
        DomainExecutionResult domainResult = groundingEngine.ground(
                turn,
                turn.entities(),
                callerRole,
                authenticatedUid
        );

        // 3. Special handling for WEATHER_INQUIRY: must bypass LLM synthesis completely
        OrchestrationPayload payload;
        if (turn.intent() == AgriIntent.WEATHER_INQUIRY) {
            String formattedWeather = fallbackFormatter.format(domainResult, query);
            payload = OrchestrationPayload.fallback(
                    formattedWeather,
                    domainResult.warnings(),
                    domainResult.domainData()
            );
        } else {
            // 4. Elapsed-time budget check: ensure sufficient deadline remaining before attempting LLM synthesis
            long elapsed = System.currentTimeMillis() - start;
            long remainingMs = ORCHESTRATION_BUDGET_MS - elapsed;

            if (remainingMs < MIN_LLM_BUDGET_MS) {
                log.warn("Insufficient turn budget remaining ({} ms elapsed, {} ms remaining of {} ms). Skipping LLM synthesis and using deterministic fallback.",
                        elapsed, remainingMs, ORCHESTRATION_BUDGET_MS);
                List<String> updatedWarnings = new ArrayList<>(domainResult.warnings());
                updatedWarnings.add("Turn deadline budget depleted (" + elapsed + " ms elapsed); deterministic fallback used.");
                String formattedFallback = fallbackFormatter.format(domainResult, query);
                payload = OrchestrationPayload.fallback(
                        formattedFallback,
                        updatedWarnings,
                        domainResult.domainData()
                );
            } else {
                // Sufficient budget remaining: invoke LLM synthesis
                payload = synthesisService.synthesize(domainResult, query);
            }
        }

        // 5. Bounded Payload Size Safeguard
        payload = enforcePayloadSizeLimit(payload);

        long totalLatency = System.currentTimeMillis() - start;
        log.debug("Turn {} orchestration completed in {} ms (fallback={})",
                turn.turnIndex(), totalLatency, payload.fallback());

        return new RouterTurnResponse(
                turn.sessionId(),
                turn.turnIndex(),
                turn.intent(),
                turn.confidence(),
                turn.confidenceBand(),
                turn.entities(),
                turn.clarification(),
                turn.authorized(),
                false, // newly orchestrated, not cached yet
                payload
        );
    }

    private OrchestrationPayload enforcePayloadSizeLimit(OrchestrationPayload payload) {
        if (payload == null) {
            return OrchestrationPayload.empty();
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            if (bytes.length <= MAX_PAYLOAD_WATERMARK_BYTES) {
                return payload;
            }

            log.warn("Orchestration payload size ({} bytes) exceeded safety watermark ({} bytes). Pruning domainData.",
                    bytes.length, MAX_PAYLOAD_WATERMARK_BYTES);

            List<String> updatedWarnings = new ArrayList<>(payload.warnings());
            updatedWarnings.add("Detailed domain facts pruned to remain within turn storage bounds.");

            // Truncate domainData to a high-level summary map
            Map<String, Object> prunedData = new LinkedHashMap<>();
            prunedData.put("pruned", true);
            prunedData.put("originalSizeBytes", bytes.length);

            return new OrchestrationPayload(
                    payload.synthesizedText(),
                    payload.fallback(),
                    updatedWarnings,
                    prunedData
            );
        } catch (Exception e) {
            log.error("Failed to inspect orchestration payload size: {}", e.getMessage());
            return payload;
        }
    }
}
