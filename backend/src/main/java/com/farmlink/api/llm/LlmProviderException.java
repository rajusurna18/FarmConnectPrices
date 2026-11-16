package com.farmlink.api.llm;

/**
 * Exception thrown when an LLM provider operation fails unrecoverably.
 */
public class LlmProviderException extends RuntimeException {

    private final LlmProviderType provider;
    private final LlmErrorCode errorCode;

    public LlmProviderException(LlmProviderType provider, LlmErrorCode errorCode, String message) {
        super(message);
        this.provider = provider != null ? provider : LlmProviderType.NONE;
        this.errorCode = errorCode != null ? errorCode : LlmErrorCode.EXECUTION_FAILED;
    }

    public LlmProviderException(LlmProviderType provider, LlmErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider != null ? provider : LlmProviderType.NONE;
        this.errorCode = errorCode != null ? errorCode : LlmErrorCode.EXECUTION_FAILED;
    }

    public LlmProviderType getProvider() {
        return provider;
    }

    public LlmErrorCode getErrorCode() {
        return errorCode;
    }
}

