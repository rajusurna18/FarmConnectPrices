package com.farmlink.api.llm;

import com.farmlink.api.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiLlmProviderTest {

    private LlmProperties properties;
    private GeminiLlmProvider geminiProvider;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        geminiProvider = new GeminiLlmProvider(properties);
    }

    @Test
    @DisplayName("Gemini provider identifies as GEMINI provider type")
    void testGeminiProviderType() {
        assertEquals(LlmProviderType.GEMINI, geminiProvider.getType());
    }

    @Test
    @DisplayName("Gemini provider has safe defaults: disabled, empty model, empty API key")
    void testGeminiSafeDefaults() {
        assertFalse(geminiProvider.isEnabled());
        assertFalse(properties.getGemini().isEnabled());
        assertEquals("", properties.getGemini().getModel());
        assertEquals("", properties.getGemini().getApiKey());
        assertEquals(30, properties.getGemini().getTimeoutSeconds());
    }

    @Test
    @DisplayName("Gemini provider returns controlled error when integration is disabled")
    void testGeminiDisabledControlledError() {
        properties.getGemini().setEnabled(false);

        LlmRequest request = LlmRequest.of("What is the current modal price of wheat?");
        LlmResponse response = geminiProvider.generate(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.GEMINI, response.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_DISABLED, response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("disabled"));
        assertEquals("", response.getContent());
    }

    @Test
    @DisplayName("Gemini provider returns controlled error when enabled but model is not configured")
    void testGeminiEnabledWithoutModel() {
        properties.getGemini().setEnabled(true);
        properties.getGemini().setModel(""); // No model configured
        properties.getGemini().setApiKey("dummy-key");

        assertTrue(geminiProvider.isEnabled());

        LlmRequest request = LlmRequest.of("Explain crop pest management");
        LlmResponse response = geminiProvider.generate(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.GEMINI, response.getProvider());
        assertEquals(LlmErrorCode.MISSING_CONFIGURATION, response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("model is not configured"));
        assertEquals("", response.getContent());
    }

    @Test
    @DisplayName("Gemini provider returns controlled error when enabled with model but missing API key")
    void testGeminiEnabledWithoutApiKey() {
        properties.getGemini().setEnabled(true);
        properties.getGemini().setModel("gemini-1.5-flash");
        properties.getGemini().setApiKey(""); // Missing key

        assertTrue(geminiProvider.isEnabled());

        LlmRequest request = LlmRequest.of("Explain crop pest management");
        LlmResponse response = geminiProvider.generate(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.GEMINI, response.getProvider());
        assertEquals(LlmErrorCode.MISSING_CONFIGURATION, response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("GEMINI_API_KEY"));
        assertEquals("", response.getContent());
    }

    @Test
    @DisplayName("Gemini provider foundation returns controlled unavailable error without live transport or fabrication")
    void testGeminiFoundationDoesNotFabricateOrMakeOutboundCalls() {
        properties.getGemini().setEnabled(true);
        properties.getGemini().setModel("gemini-1.5-flash");
        properties.getGemini().setApiKey("dummy-test-key-not-for-live-calls");

        assertTrue(geminiProvider.isEnabled());

        LlmRequest request = LlmRequest.of("Hello Gemini");
        LlmResponse response = geminiProvider.generate(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.GEMINI, response.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_UNAVAILABLE, response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("Unit 1 foundation"));
        // Confirm no fabricated response is returned
        assertEquals("", response.getContent());
    }

    @Test
    @DisplayName("Gemini provider handles null request safely")
    void testGeminiNullRequest() {
        LlmResponse response = geminiProvider.generate(null);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmErrorCode.INVALID_REQUEST, response.getErrorCode());
        assertEquals("LLM request cannot be null.", response.getErrorMessage());
    }

    @Test
    @DisplayName("LlmProperties masks API key in toString to prevent credential leakage in logs")
    void testApiKeyMaskingInToString() {
        properties.getGemini().setApiKey("AIzaSySecret1234567890");
        String stringRep = properties.toString();

        assertFalse(stringRep.contains("AIzaSySecret1234567890"));
        assertTrue(stringRep.contains("******"));

        properties.getGemini().setApiKey("");
        assertTrue(properties.toString().contains("<not set>"));
    }
}
