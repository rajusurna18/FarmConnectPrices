package com.farmlink.api.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deterministic Mock LLM Provider for Unit Testing and Safe Offline Development.
 * Strictly test-scoped; not registered as a production Spring component.
 */
public class MockLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(MockLlmProvider.class);

    private String deterministicResponse = "Deterministic mock LLM response for test evaluation.";

    public MockLlmProvider() {
    }

    public MockLlmProvider(String deterministicResponse) {
        this.deterministicResponse = deterministicResponse;
    }

    @Override
    public LlmProviderType getType() {
        return LlmProviderType.MOCK;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (request == null) {
            return LlmResponse.failure(
                    LlmProviderType.MOCK,
                    LlmErrorCode.INVALID_REQUEST,
                    "LLM request cannot be null."
            );
        }

        log.debug("MockLlmProvider returning deterministic test response.");
        String content = deterministicResponse;

        // Support deterministic echo mode for tests
        if (request.getPrompt() != null && request.getPrompt().startsWith("ECHO:")) {
            content = request.getPrompt().substring(5).trim();
        }

        int promptLen = request.getPrompt() != null ? request.getPrompt().length() : 0;
        int compLen = content != null ? content.length() : 0;
        LlmUsage usage = new LlmUsage(Math.max(1, promptLen / 4), Math.max(1, compLen / 4), Math.max(2, (promptLen + compLen) / 4));

        return LlmResponse.success(
                LlmProviderType.MOCK,
                "mock-deterministic-v1",
                content,
                usage
        );
    }

    public String getDeterministicResponse() {
        return deterministicResponse;
    }

    public void setDeterministicResponse(String deterministicResponse) {
        this.deterministicResponse = deterministicResponse;
    }
}

