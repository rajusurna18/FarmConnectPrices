package com.farmlink.api.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmProviderContractTest {

    private MockLlmProvider mockProvider;

    @BeforeEach
    void setUp() {
        mockProvider = new MockLlmProvider("Default deterministic response");
    }

    @Test
    @DisplayName("LlmProvider interface contract: Mock provider basic properties")
    void testMockProviderBasicProperties() {
        assertEquals(LlmProviderType.MOCK, mockProvider.getType());
        assertTrue(mockProvider.isEnabled());
    }

    @Test
    @DisplayName("LlmProvider interface contract: Structured request building and immutability")
    void testLlmRequestBuilderAndImmutability() {
        LlmChatMessage sysMsg = LlmChatMessage.system("You are an agricultural assistant.");
        LlmChatMessage userMsg = LlmChatMessage.user("What is the mandi price for wheat?");

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("You are an agricultural assistant.")
                .prompt("What is the mandi price for wheat?")
                .messages(List.of(sysMsg, userMsg))
                .temperature(0.5)
                .maxTokens(512)
                .model("test-model")
                .addMetadata("correlationId", "req-12345")
                .build();

        assertEquals("You are an agricultural assistant.", request.getSystemPrompt());
        assertEquals("What is the mandi price for wheat?", request.getPrompt());
        assertEquals(2, request.getMessages().size());
        assertEquals(0.5, request.getTemperature());
        assertEquals(512, request.getMaxTokens());
        assertEquals("test-model", request.getModel());
        assertEquals("req-12345", request.getMetadata().get("correlationId"));

        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> request.getMessages().add(LlmChatMessage.user("Extra")));
        assertThrows(UnsupportedOperationException.class, () -> request.getMetadata().put("new", "val"));
    }

    @Test
    @DisplayName("LlmProvider interface contract: Structured response success and failure handling")
    void testLlmResponseContracts() {
        // Success contract
        LlmUsage usage = new LlmUsage(15, 25, 40);
        LlmResponse successResp = LlmResponse.success(LlmProviderType.MOCK, "mock-v1", "Test content", usage);

        assertTrue(successResp.isSuccess());
        assertEquals(LlmProviderType.MOCK, successResp.getProvider());
        assertEquals("mock-v1", successResp.getModel());
        assertEquals("Test content", successResp.getContent());
        assertEquals(usage, successResp.getUsage());
        assertNull(successResp.getErrorCode());
        assertEquals("", successResp.getErrorMessage());
        assertDoesNotThrow(successResp::throwIfFailed);

        // Failure contract
        LlmResponse failureResp = LlmResponse.failure(LlmProviderType.GEMINI, LlmErrorCode.PROVIDER_DISABLED, "Provider is disabled");

        assertFalse(failureResp.isSuccess());
        assertEquals(LlmProviderType.GEMINI, failureResp.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_DISABLED, failureResp.getErrorCode());
        assertEquals("Provider is disabled", failureResp.getErrorMessage());
        assertEquals("", failureResp.getContent());

        LlmProviderException ex = assertThrows(LlmProviderException.class, failureResp::throwIfFailed);
        assertEquals(LlmProviderType.GEMINI, ex.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_DISABLED, ex.getErrorCode());
        assertEquals("Provider is disabled", ex.getMessage());
    }

    @Test
    @DisplayName("Mock provider deterministic behavior and echo mode")
    void testMockProviderDeterministicBehavior() {
        LlmRequest request = LlmRequest.of("Explain crop rotation");
        LlmResponse response = mockProvider.generate(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(LlmProviderType.MOCK, response.getProvider());
        assertEquals("Default deterministic response", response.getContent());
        assertNotNull(response.getUsage());
        assertTrue(response.getUsage().getTotalTokens() > 0);

        // Test echo mode
        LlmRequest echoRequest = LlmRequest.of("ECHO: Tomato price is 2500");
        LlmResponse echoResponse = mockProvider.generate(echoRequest);
        assertTrue(echoResponse.isSuccess());
        assertEquals("Tomato price is 2500", echoResponse.getContent());

        // Test custom deterministic response
        mockProvider.setDeterministicResponse("Custom test output");
        LlmResponse customResponse = mockProvider.generate(request);
        assertEquals("Custom test output", customResponse.getContent());
    }

    @Test
    @DisplayName("DisabledLlmProvider interface contract: NONE provider explicit failure response")
    void testDisabledLlmProviderContract() {
        DisabledLlmProvider disabled = DisabledLlmProvider.INSTANCE;
        assertEquals(LlmProviderType.NONE, disabled.getType());
        assertFalse(disabled.isEnabled());

        LlmRequest request = LlmRequest.of("Test query");
        LlmResponse response = disabled.generate(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmProviderType.NONE, response.getProvider());
        assertEquals(LlmErrorCode.PROVIDER_DISABLED, response.getErrorCode());
        assertTrue(response.getErrorMessage().contains("app.llm.provider=NONE"));
    }

    @Test
    @DisplayName("Mock provider handles null request safely")
    void testMockProviderNullRequest() {
        LlmResponse response = mockProvider.generate(null);
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(LlmErrorCode.INVALID_REQUEST, response.getErrorCode());
        assertEquals("LLM request cannot be null.", response.getErrorMessage());
    }
}

