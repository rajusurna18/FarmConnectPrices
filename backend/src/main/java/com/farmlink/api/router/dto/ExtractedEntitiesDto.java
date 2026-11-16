package com.farmlink.api.router.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extracted agricultural entities from the user query.
 */
public record ExtractedEntitiesDto(
        List<String> crops,
        List<String> locations,
        List<String> mandis,
        List<String> quantities,
        Map<String, Object> attributes
) {
    public static ExtractedEntitiesDto empty() {
        return new ExtractedEntitiesDto(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap()
        );
    }
}

