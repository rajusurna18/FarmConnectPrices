package com.farmlink.api.llm;

import java.util.*;

/**
 * Provider-independent request model for LLM interactions.
 * Supports single prompt and multi-turn message conversations.
 */
public class LlmRequest {

    private final String systemPrompt;
    private final String prompt;
    private final List<LlmChatMessage> messages;
    private final Double temperature;
    private final Integer maxTokens;
    private final String model;
    private final Map<String, String> metadata;

    private LlmRequest(Builder builder) {
        this.systemPrompt = builder.systemPrompt;
        this.prompt = builder.prompt;
        this.messages = builder.messages != null ? Collections.unmodifiableList(new ArrayList<>(builder.messages)) : Collections.emptyList();
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.model = builder.model;
        this.metadata = builder.metadata != null ? Collections.unmodifiableMap(new HashMap<>(builder.metadata)) : Collections.emptyMap();
    }

    public static LlmRequest of(String prompt) {
        return builder().prompt(prompt).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<LlmChatMessage> getMessages() {
        return messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public String getModel() {
        return model;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "LlmRequest{" +
                "systemPromptLength=" + (systemPrompt != null ? systemPrompt.length() : 0) +
                ", promptLength=" + (prompt != null ? prompt.length() : 0) +
                ", messagesCount=" + messages.size() +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", model='" + model + '\'' +
                '}';
    }

    public static class Builder {
        private String systemPrompt;
        private String prompt;
        private List<LlmChatMessage> messages = new ArrayList<>();
        private Double temperature = 0.2;
        private Integer maxTokens = 1024;
        private String model;
        private Map<String, String> metadata = new HashMap<>();

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder messages(List<LlmChatMessage> messages) {
            this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
            return this;
        }

        public Builder addMessage(LlmChatMessage message) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            if (message != null) {
                this.messages.add(message);
            }
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }

        public Builder addMetadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            if (key != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public LlmRequest build() {
            return new LlmRequest(this);
        }
    }
}

