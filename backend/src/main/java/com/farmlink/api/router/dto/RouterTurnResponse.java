package com.farmlink.api.router.dto;

import com.farmlink.api.orchestrator.dto.OrchestrationPayload;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.entity.ConfidenceBand;

public record RouterTurnResponse(
        String sessionId,
        long turnIndex,
        AgriIntent intent,
        double confidence,
        ConfidenceBand confidenceBand,
        ExtractedEntitiesDto entities,
        ClarificationDetails clarification,
        boolean authorized,
        boolean cached,
        OrchestrationPayload orchestration
) {
    /**
     * Backward-compatible 9-argument constructor preserving existing Unit 2 calls and tests.
     */
    public RouterTurnResponse(
            String sessionId,
            long turnIndex,
            AgriIntent intent,
            double confidence,
            ConfidenceBand confidenceBand,
            ExtractedEntitiesDto entities,
            ClarificationDetails clarification,
            boolean authorized,
            boolean cached
    ) {
        this(sessionId, turnIndex, intent, confidence, confidenceBand, entities, clarification, authorized, cached, null);
    }
}

