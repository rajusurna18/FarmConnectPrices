package com.farmlink.api.orchestrator.dto;

import com.farmlink.api.router.entity.AgriIntent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the output of a domain grounding execution.
 * Carries grounded data, partial failure warnings, and execution metrics.
 */
public record DomainExecutionResult(
        AgriIntent intent,
        boolean success,
        boolean partialSuccess,
        List<String> warnings,
        Map<String, Object> domainData,
        String summaryText,
        long latencyMs
) {
    public DomainExecutionResult {
        warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
        domainData = domainData != null ? Collections.unmodifiableMap(domainData) : Collections.emptyMap();
    }

    public static DomainExecutionResult success(AgriIntent intent, Map<String, Object> data, String summary, long latencyMs) {
        return new DomainExecutionResult(intent, true, false, Collections.emptyList(), data, summary, latencyMs);
    }

    public static DomainExecutionResult partial(AgriIntent intent, Map<String, Object> data, List<String> warnings, String summary, long latencyMs) {
        return new DomainExecutionResult(intent, true, true, warnings, data, summary, latencyMs);
    }

    public static DomainExecutionResult failure(AgriIntent intent, List<String> warnings, String summary, long latencyMs) {
        return new DomainExecutionResult(intent, false, false, warnings, Collections.emptyMap(), summary, latencyMs);
    }
}
