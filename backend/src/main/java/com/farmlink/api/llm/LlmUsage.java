package com.farmlink.api.llm;

import java.util.Objects;

/**
 * Encapsulates token usage statistics for an LLM response.
 */
public class LlmUsage {

    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;

    public LlmUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public static LlmUsage zero() {
        return new LlmUsage(0, 0, 0);
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmUsage llmUsage = (LlmUsage) o;
        return promptTokens == llmUsage.promptTokens &&
                completionTokens == llmUsage.completionTokens &&
                totalTokens == llmUsage.totalTokens;
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptTokens, completionTokens, totalTokens);
    }

    @Override
    public String toString() {
        return "LlmUsage{" +
                "promptTokens=" + promptTokens +
                ", completionTokens=" + completionTokens +
                ", totalTokens=" + totalTokens +
                '}';
    }
}

