package com.farmlink.api.llm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-independent response model for LLM interactions.
 * Guarantees explicit success, failure, metadata, and token accounting contracts.
 */
public class LlmResponse {

    private final boolean success;
    private final LlmProviderType provider;
    private final String model;
    private final String content;
    private final LlmUsage usage;
    private final LlmErrorCode errorCode;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    public LlmResponse(boolean success,
                       LlmProviderType provider,
                       String model,
                       String content,
                       LlmUsage usage,
                       LlmErrorCode errorCode,
                       String errorMessage,
                       Map<String, Object> metadata) {
        this.success = success;
        this.provider = provider != null ? provider : LlmProviderType.NONE;
        this.model = model != null ? model : "";
        this.content = content != null ? content : "";
        this.usage = usage != null ? usage : LlmUsage.zero();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<>(metadata)) : Collections.emptyMap();
    }

    public static LlmResponse success(LlmProviderType provider, String model, String content, LlmUsage usage) {
        return new LlmResponse(true, provider, model, content, usage, null, null, Collections.emptyMap());
    }

    public static LlmResponse success(LlmProviderType provider, String model, String content, LlmUsage usage, Map<String, Object> metadata) {
        return new LlmResponse(true, provider, model, content, usage, null, null, metadata);
    }

    public static LlmResponse failure(LlmProviderType provider, LlmErrorCode errorCode, String errorMessage) {
        return new LlmResponse(false, provider, null, null, LlmUsage.zero(), errorCode, errorMessage, Collections.emptyMap());
    }

    public static LlmResponse failure(LlmProviderType provider, LlmErrorCode errorCode, String errorMessage, Map<String, Object> metadata) {
        return new LlmResponse(false, provider, null, null, LlmUsage.zero(), errorCode, errorMessage, metadata);
    }

    public void throwIfFailed() {
        if (!success) {
            throw new LlmProviderException(provider, errorCode, errorMessage);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public LlmProviderType getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getContent() {
        return content;
    }

    public LlmUsage getUsage() {
        return usage;
    }

    public LlmErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmResponse that = (LlmResponse) o;
        return success == that.success &&
                provider == that.provider &&
                Objects.equals(model, that.model) &&
                Objects.equals(content, that.content) &&
                Objects.equals(usage, that.usage) &&
                errorCode == that.errorCode &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, provider, model, content, usage, errorCode, errorMessage);
    }

    @Override
    public String toString() {
        return "LlmResponse{" +
                "success=" + success +
                ", provider=" + provider +
                ", model='" + model + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", usage=" + usage +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

