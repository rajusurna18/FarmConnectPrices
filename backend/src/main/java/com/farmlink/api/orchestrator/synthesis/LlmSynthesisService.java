package com.farmlink.api.orchestrator.synthesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmlink.api.llm.*;
import com.farmlink.api.orchestrator.dto.DomainExecutionResult;
import com.farmlink.api.orchestrator.dto.OrchestrationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Synthesizes natural-language responses from grounded domain execution results.
 * Strictly checks Unit 1 provider availability: if disabled or NONE, completely bypasses LLM
 * and uses DeterministicFallbackFormatter for zero-hallucination Markdown rendering.
 */
@Service
public class LlmSynthesisService {

    private static final Logger log = LoggerFactory.getLogger(LlmSynthesisService.class);
    public static final long LLM_CALL_TIMEOUT_MS = 4500L;

    private final LlmProviderRegistry providerRegistry;
    private final DeterministicFallbackFormatter fallbackFormatter;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public LlmSynthesisService(
            LlmProviderRegistry providerRegistry,
            DeterministicFallbackFormatter fallbackFormatter,
            ObjectMapper objectMapper,
            @Qualifier("orchestratorTaskExecutor") Executor executor
    ) {
        this.providerRegistry = providerRegistry;
        this.fallbackFormatter = fallbackFormatter;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    public OrchestrationPayload synthesize(DomainExecutionResult domainResult, String userQuery) {
        if (domainResult == null) {
            String fallbackText = "Unable to process agricultural query.";
            return OrchestrationPayload.fallback(fallbackText, List.of("Domain result was null"), Map.of());
        }

        LlmProvider provider = providerRegistry != null ? providerRegistry.getActiveProvider() : null;

        // 1. Mandatory Bypass: If provider is disabled, NONE, or null, bypass LLM completely
        if (provider == null || !provider.isEnabled() || provider.getType() == LlmProviderType.NONE) {
            log.debug("LLM provider is disabled or NONE. Using deterministic fallback formatter.");
            String formattedMarkdown = fallbackFormatter.format(domainResult, userQuery);
            return OrchestrationPayload.fallback(formattedMarkdown, domainResult.warnings(), domainResult.domainData());
        }

        // 2. Active LLM Provider: Construct strictly bounded prompt
        String systemPrompt = buildSystemPrompt(domainResult);
        String userPromptContent = buildUserPrompt(domainResult, userQuery);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt(systemPrompt)
                .prompt(userPromptContent)
                .temperature(0.2) // Low temperature for factual precision
                .maxTokens(1000)
                .build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<LlmResponse> future = null;

        try {
            future = CompletableFuture.supplyAsync(
                    () -> provider.generate(request),
                    executor
            );
            LlmResponse response = future.get(LLM_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response != null && response.isSuccess() && response.getContent() != null && !response.getContent().isBlank()) {
                long latency = System.currentTimeMillis() - startTime;
                log.debug("LLM synthesis succeeded in {} ms via provider {}", latency, provider.getType());
                String rawContent = response.getContent().trim();
                String safeContent = rawContent.length() > 4000 ? rawContent.substring(0, 4000) : rawContent;
                return new OrchestrationPayload(
                        safeContent,
                        false,
                        domainResult.warnings(),
                        domainResult.domainData()
                );
            } else {
                String errorMsg = response != null ? response.getErrorMessage() : "Empty response from LLM";
                log.warn("LLM synthesis failed: {}. Falling back to deterministic formatter.", errorMsg);
                List<String> combinedWarnings = new ArrayList<>(domainResult.warnings());
                combinedWarnings.add("LLM synthesis unavailable: " + errorMsg);
                String formatted = fallbackFormatter.format(domainResult, userQuery);
                return OrchestrationPayload.fallback(formatted, combinedWarnings, domainResult.domainData());
            }
        } catch (RejectedExecutionException ree) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("LLM synthesis rejected by saturated task executor after {} ms: {}. Falling back to deterministic formatter.", latency, ree.getMessage());
            List<String> combinedWarnings = new ArrayList<>(domainResult.warnings());
            combinedWarnings.add("LLM synthesis thread pool saturated; using deterministic response.");
            String formatted = fallbackFormatter.format(domainResult, userQuery);
            return OrchestrationPayload.fallback(formatted, combinedWarnings, domainResult.domainData());
        } catch (TimeoutException te) {
            if (future != null) {
                future.cancel(true);
            }
            long latency = System.currentTimeMillis() - startTime;
            log.warn("LLM synthesis timed out after {} ms. Falling back to deterministic formatter.", latency);
            List<String> combinedWarnings = new ArrayList<>(domainResult.warnings());
            combinedWarnings.add("LLM synthesis timed out after " + LLM_CALL_TIMEOUT_MS + " ms");
            String formatted = fallbackFormatter.format(domainResult, userQuery);
            return OrchestrationPayload.fallback(formatted, combinedWarnings, domainResult.domainData());
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("LLM synthesis error after {} ms: {}. Falling back to deterministic formatter.", latency, e.getMessage());
            List<String> combinedWarnings = new ArrayList<>(domainResult.warnings());
            combinedWarnings.add("LLM synthesis error: " + e.getMessage());
            String formatted = fallbackFormatter.format(domainResult, userQuery);
            return OrchestrationPayload.fallback(formatted, combinedWarnings, domainResult.domainData());
        }
    }

    private String buildSystemPrompt(DomainExecutionResult result) {
        return "You are FarmLink AI, an authoritative, helpful, and safety-oriented agricultural advisory assistant.\n\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. Answer based ONLY on the verified data provided in the grounding context.\n" +
                "2. Do NOT fabricate or invent prices, mandi names, dates, chemical dosages, or weather forecasts not present in the verified context.\n" +
                "3. If any data is missing or marked unavailable, state the omission clearly without hallucinating.\n" +
                "4. For agronomic, pest, and disease inquiries: provide standard extension best practices and remind the user to consult local Krishi Vigyan Kendra (KVK) extension officers for chemical dosage prescriptions.\n" +
                "5. Never reveal or refer to private farm economic details of other farmers.\n" +
                "6. Format your response cleanly using GitHub-flavored Markdown.";
    }

    private String buildUserPrompt(DomainExecutionResult result, String query) {
        String dataJson;
        try {
            dataJson = objectMapper.writeValueAsString(result.domainData());
        } catch (Exception e) {
            dataJson = result.domainData().toString();
        }

        return "USER INQUIRY: \"" + query + "\"\n\n" +
                "VERIFIED GROUNDED CONTEXT FROM FARMLINK DOMAIN SERVICES:\n" +
                "- Intent: " + result.intent() + "\n" +
                "- Summary: " + result.summaryText() + "\n" +
                "- Domain Facts (JSON): " + dataJson + "\n" +
                (!result.warnings().isEmpty() ? "- Known Data Omissions: " + String.join("; ", result.warnings()) + "\n" : "") +
                "\nPlease provide a clear, helpful response addressing the user's question using only the verified facts above.";
    }
}
