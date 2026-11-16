package com.farmlink.api.llm;

/**
 * Explicit disabled LLM provider representing the unconfigured/NONE provider state.
 * Prevents unexplained null references and provides a controlled, predictable failure response.
 */
public class DisabledLlmProvider implements LlmProvider {

    public static final DisabledLlmProvider INSTANCE = new DisabledLlmProvider();

    @Override
    public LlmProviderType getType() {
        return LlmProviderType.NONE;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        return LlmResponse.failure(
                LlmProviderType.NONE,
                LlmErrorCode.PROVIDER_DISABLED,
                "No active LLM provider is configured (app.llm.provider=NONE). Configure a valid provider (e.g. GEMINI) to enable LLM operations."
        );
    }
}

