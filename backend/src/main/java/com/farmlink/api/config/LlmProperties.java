package com.farmlink.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration properties for the LLM Provider architecture.
 * Safe by default: Gemini integration is disabled, no API key is required.
 */
@Configuration
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /**
     * Selected active LLM provider. Options: NONE, GEMINI, MOCK.
     */
    private String provider = "NONE";

    /**
     * Google Gemini provider configuration.
     */
    private GeminiProperties gemini = new GeminiProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public GeminiProperties getGemini() {
        return gemini;
    }

    public void setGemini(GeminiProperties gemini) {
        this.gemini = gemini;
    }

    @Override
    public String toString() {
        return "LlmProperties{" +
                "provider='" + provider + '\'' +
                ", gemini=" + gemini +
                '}';
    }

    public static class GeminiProperties {
        private boolean enabled = false;
        private String model = "";
        private String apiKey = "";
        private int timeoutSeconds = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        @Override
        public String toString() {
            return "GeminiProperties{" +
                    "enabled=" + enabled +
                    ", model='" + model + '\'' +
                    ", apiKey='" + ((apiKey != null && !apiKey.isBlank()) ? "******" : "<not set>") + '\'' +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }
    }
}

