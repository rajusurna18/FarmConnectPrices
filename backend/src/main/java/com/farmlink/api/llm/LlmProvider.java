package com.farmlink.api.llm;

/**
 * Core interface isolating LLM provider implementations from domain services.
 * Implementations must provide explicit success/failure handling without leaking SDK-specific types.
 */
public interface LlmProvider {

    /**
     * Identifies the provider type.
     */
    LlmProviderType getType();

    /**
     * Indicates whether the provider is currently enabled in configuration.
     */
    boolean isEnabled();

    /**
     * Executes generation against the provider with structured request and response contracts.
     * Must return controlled failure responses or exceptions rather than fabricating outputs.
     */
    LlmResponse generate(LlmRequest request);
}

