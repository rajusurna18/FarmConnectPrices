package com.farmlink.api.orchestrator.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the downstream orchestration result attached to a RouterTurnResponse.
 * Contains synthesized natural language text, fallback flag, warnings, and domain data.
 */
public record OrchestrationPayload(
        String synthesizedText,
        boolean fallback,
        List<String> warnings,
        Map<String, Object> domainData
) {
    public OrchestrationPayload {
        warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        domainData = domainData != null ? Collections.unmodifiableMap(domainData) : Collections.emptyMap();
    }

    public static OrchestrationPayload empty() {
        return new OrchestrationPayload("", false, Collections.emptyList(), Collections.emptyMap());
    }

    public static OrchestrationPayload fallback(String fallbackText, List<String> warnings, Map<String, Object> domainData) {
        return new OrchestrationPayload(fallbackText, true, warnings, domainData);
    }
}
