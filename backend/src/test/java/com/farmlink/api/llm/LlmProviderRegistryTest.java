package com.farmlink.api.llm;

import com.farmlink.api.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LlmProviderRegistryTest {

    private LlmProperties properties;
    private MockEnvironment environment;
    private GeminiLlmProvider geminiProvider;
    private MockLlmProvider mockProvider;
    private LlmProviderRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        environment = new MockEnvironment();
        geminiProvider = new GeminiLlmProvider(properties);
        mockProvider = new MockLlmProvider();
        registry = new LlmProviderRegistry(properties, List.of(geminiProvider, mockProvider), environment);
    }

    @Test
    @DisplayName("Default configuration resolves to NONE and returns explicit DisabledLlmProvider without null pointer paths")
    void testDefaultNoneProviderReturnsExplicitDisabledProvider() {
        assertEquals(LlmProviderType.NONE, registry.resolveSelectedProviderType());

        // Must never return null
        LlmProvider active = registry.getActiveProvider();
        assertNotNull(active);
        assertEquals(LlmProviderType.NONE, active.getType());
        assertFalse(active.isEnabled());

        // Downstream generate returns controlled failure response
        LlmRequest request = LlmRequest.of("Any query");
        LlmResponse response = active.generate(request);
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.NONE, response.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_DISABLED, response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("app.llm.provider=NONE"));

        // findActiveProvider returns Optional.empty()
        Optional<LlmProvider> optionalProvider = registry.findActiveProvider();
        assertTrue(optionalProvider.isEmpty());
    }

    @Test
    @DisplayName("Resolves GEMINI provider when configured")
    void testResolveGeminiProvider() {
        properties.setProvider("GEMINI");
        assertEquals(LlmProviderType.GEMINI, registry.resolveSelectedProviderType());
        LlmProvider active = registry.getActiveProvider();
        assertNotNull(active);
        assertEquals(LlmProviderType.GEMINI, active.getType());
    }

    @Test
    @DisplayName("Application configuration selecting MOCK provider is strictly rejected as test-only")
    void testMockProviderRejectedInApplicationConfig() {
        properties.setProvider("MOCK");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.resolveSelectedProviderType());
        assertTrue(ex.getMessage().contains("MOCK LLM provider is strictly test-only"));
    }

    @Test
    @DisplayName("Production safeguard: MOCK provider cannot be selected in production environment")
    void testProductionSafeguardBlocksMockProvider() {
        properties.setProvider("MOCK");
        environment.setActiveProfiles("prod");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.validateEnvironmentAndConfiguration());
        assertTrue(ex.getMessage().contains("MOCK LLM provider is strictly test-only"));
    }

    @Test
    @DisplayName("Unsupported provider configuration throws IllegalArgumentException")
    void testUnsupportedProviderConfiguration() {
        properties.setProvider("CLAUDE_OR_OPENAI");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.resolveSelectedProviderType());
        assertTrue(ex.getMessage().contains("Unsupported LLM provider configured"));
        assertTrue(ex.getMessage().contains("CLAUDE_OR_OPENAI"));
    }

    @Test
    @DisplayName("Gemini selected while disabled does NOT silently fall back to MOCK or Disabled provider")
    void testGeminiDisabledNeverSilentlyFallsBackToMock() {
        properties.setProvider("GEMINI");
        properties.getGemini().setEnabled(false); // explicitly disabled

        // Active provider must be Gemini, NOT Mock or Disabled
        LlmProvider active = registry.getActiveProvider();
        assertNotNull(active);
        assertEquals(LlmProviderType.GEMINI, active.getType());

        // Calling generate must return controlled error from Gemini, NOT mock response
        LlmRequest request = LlmRequest.of("Test query");
        LlmResponse response = active.generate(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.GEMINI, response.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_DISABLED, response.getErrorCode());
        assertNotEquals("Deterministic mock LLM response for test evaluation.", response.getContent());
    }

    @Test
    @DisplayName("Get provider by explicit type")
    void testGetProviderByType() {
        assertSame(geminiProvider, registry.getProvider(LlmProviderType.GEMINI));
        assertSame(mockProvider, registry.getProvider(LlmProviderType.MOCK));
        assertSame(DisabledLlmProvider.INSTANCE, registry.getProvider(LlmProviderType.NONE));
    }
}
