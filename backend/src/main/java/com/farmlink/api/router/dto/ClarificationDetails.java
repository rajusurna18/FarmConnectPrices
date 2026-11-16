package com.farmlink.api.router.dto;

import com.farmlink.api.router.entity.AgriIntent;

import java.util.Collections;
import java.util.List;

/**
 * Details when intent clarification is required.
 */
public record ClarificationDetails(
        boolean required,
        String prompt,
        List<AgriIntent> candidateIntents,
        List<String> missingEntities
) {
    public static ClarificationDetails notRequired() {
        return new ClarificationDetails(
                false,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public static ClarificationDetails of(String prompt, List<AgriIntent> candidateIntents, List<String> missingEntities) {
        return new ClarificationDetails(
                true,
                prompt,
                candidateIntents != null ? candidateIntents : Collections.emptyList(),
                missingEntities != null ? missingEntities : Collections.emptyList()
        );
    }
}

