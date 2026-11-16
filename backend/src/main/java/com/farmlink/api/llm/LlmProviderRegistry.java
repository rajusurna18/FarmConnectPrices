package com.farmlink.api.llm;

import com.farmlink.api.config.LlmProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and orchestrator for LLM Provider selection and lifecycle enforcement.
 * Enforces production safeguards, isolates test-only mocks, and prevents silent fallback.
 */
@Service
public class LlmProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderRegistry.class);

    private final LlmProperties properties;
    private final Environment environment;
    private final Map<LlmProviderType, LlmProvider> providers = new ConcurrentHashMap<>();
    private final DisabledLlmProvider disabledProvider = DisabledLlmProvider.INSTANCE;

    public LlmProviderRegistry(LlmProperties properties,
                               List<LlmProvider> providerList,
                               Environment environment) {
        this.properties = properties;
        this.environment = environment;
        if (providerList != null) {
            for (LlmProvider provider : providerList) {
                providers.put(provider.getType(), provider);
            }
        }
    }

    @PostConstruct
    public void validateEnvironmentAndConfiguration() {
        String configuredProviderStr = properties != null ? properties.getProvider() : null;
        log.info("Initializing LLM Provider Architecture. Configured provider: {}", configuredProviderStr);

        LlmProviderType selectedType = resolveSelectedProviderType();

        // Production safeguard: Never allow MOCK provider in production environment
        if (selectedType == LlmProviderType.MOCK && isProductionEnvironment()) {
            String errorMsg = "CRITICAL CONFIGURATION ERROR: Production environment MUST NOT use MOCK LLM provider! Startup aborted.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * Resolves the configured provider type.
     * Prevents normal application configurations from selecting the test-only MOCK provider.
     * Throws IllegalArgumentException if MOCK or an unsupported provider string is configured.
     */
    public LlmProviderType resolveSelectedProviderType() {
        String providerName = properties != null ? properties.getProvider() : null;
        if (providerName == null || providerName.trim().isEmpty() || "NONE".equalsIgnoreCase(providerName.trim())) {
            return LlmProviderType.NONE;
        }

        String normalized = providerName.trim().toUpperCase();
        if ("MOCK".equals(normalized)) {
            throw new IllegalArgumentException("The MOCK LLM provider is strictly test-only and cannot be configured in application runtime. Supported application providers are: NONE, GEMINI.");
        }

        try {
            return LlmProviderType.fromString(providerName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported LLM provider configured: '" + providerName +
                    "'. Supported providers are: GEMINI, NONE.", e);
        }
    }

    /**
     * Retrieves the actively configured LLM provider bean.
     * If provider is NONE, returns explicit DisabledLlmProvider (never null).
     * Throws IllegalStateException if configured provider has no registered bean.
     * NEVER silently falls back to MOCK when another provider is selected.
     */
    public LlmProvider getActiveProvider() {
        LlmProviderType selected = resolveSelectedProviderType();
        if (selected == LlmProviderType.NONE) {
            log.info("No active LLM provider configured (app.llm.provider=NONE). Returning explicit DisabledLlmProvider.");
            return disabledProvider;
        }

        LlmProvider provider = providers.get(selected);
        if (provider == null) {
            throw new IllegalStateException("No LLM provider bean registered for type: " + selected);
        }
        return provider;
    }

    /**
     * Returns an Optional containing the active LLM provider if configured and enabled,
     * or Optional.empty() if unconfigured, disabled, or set to NONE.
     */
    public Optional<LlmProvider> findActiveProvider() {
        LlmProvider provider = getActiveProvider();
        return (provider != null && provider.isEnabled()) ? Optional.of(provider) : Optional.empty();
    }

    /**
     * Retrieves a provider by explicit type.
     */
    public LlmProvider getProvider(LlmProviderType type) {
        if (type == LlmProviderType.NONE) {
            return disabledProvider;
        }
        return providers.get(type);
    }

    public boolean isProductionEnvironment() {
        if (environment == null) return false;
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
