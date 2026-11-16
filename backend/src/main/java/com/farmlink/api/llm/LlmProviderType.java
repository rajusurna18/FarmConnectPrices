package com.farmlink.api.llm;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supported LLM Provider Types in FarmLink AI.
 */
public enum LlmProviderType {
    GEMINI,
    MOCK,
    NONE;

    private static final Set<String> VALID_NAMES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    /**
     * Case-insensitive resolution of LlmProviderType.
     * Defaults to NONE if value is null or blank.
     * Throws IllegalArgumentException for unsupported provider strings.
     */
    public static LlmProviderType fromString(String value) {
        if (value == null || value.trim().isEmpty() || "NONE".equalsIgnoreCase(value.trim())) {
            return NONE;
        }
        String normalized = value.trim().toUpperCase();
        if (!VALID_NAMES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported LLM provider: '" + value +
                    "'. Supported providers are: GEMINI, MOCK, NONE.");
        }
        return LlmProviderType.valueOf(normalized);
    }
}

