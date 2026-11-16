package com.farmlink.api.llm;

import com.farmlink.api.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gemini LLM Provider Adapter Foundation.
 * Architectural foundation for Google Gemini integration.
 * Disabled by default; makes zero outbound network requests and initializes no live SDK clients.
 */
@Component
public class GeminiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmProvider.class);

    private final LlmProperties llmProperties;

    public GeminiLlmProvider(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    @Override
    public LlmProviderType getType() {
        return LlmProviderType.GEMINI;
    }

    @Override
    public boolean isEnabled() {
        return llmProperties != null
                && llmProperties.getGemini() != null
                && llmProperties.getGemini().isEnabled();
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (request == null) {
            return LlmResponse.failure(
                    LlmProviderType.GEMINI,
                    LlmErrorCode.INVALID_REQUEST,
                    "LLM request cannot be null."
            );
        }

        // Safe logging: Never log full prompt content or API keys
        int promptLength = (request.getPrompt() != null) ? request.getPrompt().length() : 0;
        int messagesCount = (request.getMessages() != null) ? request.getMessages().size() : 0;
        log.info("GeminiLlmProvider invoked. Enabled: {}, Prompt length: {}, Messages count: {}",
                isEnabled(), promptLength, messagesCount);

        // 1. Verify provider is enabled in configuration
        if (!isEnabled()) {
            log.warn("Gemini LLM Provider is disabled in configuration (app.llm.gemini.enabled=false).");
            return LlmResponse.failure(
                    LlmProviderType.GEMINI,
                    LlmErrorCode.PROVIDER_DISABLED,
                    "Gemini provider integration is disabled. Enable 'app.llm.gemini.enabled' and configure credentials."
            );
        }

        // 2. Verify model is explicitly configured if enabled
        String model = llmProperties.getGemini().getModel();
        if (model == null || model.trim().isEmpty()) {
            log.warn("Gemini LLM Provider is enabled but missing configured model (APP_LLM_GEMINI_MODEL).");
            return LlmResponse.failure(
                    LlmProviderType.GEMINI,
                    LlmErrorCode.MISSING_CONFIGURATION,
                    "Gemini model is not configured. Configure 'app.llm.gemini.model' (or APP_LLM_GEMINI_MODEL) to activate Gemini provider."
            );
        }

        // 3. Verify API key is configured if enabled
        String apiKey = llmProperties.getGemini().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini LLM Provider is enabled but missing GEMINI_API_KEY.");
            return LlmResponse.failure(
                    LlmProviderType.GEMINI,
                    LlmErrorCode.MISSING_CONFIGURATION,
                    "Gemini API key is not configured. Configure GEMINI_API_KEY to activate Gemini provider."
            );
        }

        // 4. Unit 1 Architectural Foundation: Live transport client is intentionally not initialized.
        // Controlled failure returned; never fabricate responses and never make outbound HTTP calls.
        log.warn("Gemini live HTTP transport is not initialized in Unit 1 foundation.");
        return LlmResponse.failure(
                LlmProviderType.GEMINI,
                LlmErrorCode.PROVIDER_UNAVAILABLE,
                "Gemini live transport client is not initialized in Unit 1 foundation."
        );
    }
}

