package com.farmlink.api.llm;

import java.util.Objects;

/**
 * Represents an individual chat message in an LLM conversation.
 */
public class LlmChatMessage {

    private final String role;
    private final String content;

    public LlmChatMessage(String role, String content) {
        this.role = role != null ? role.trim().toLowerCase() : "user";
        this.content = content != null ? content : "";
    }

    public static LlmChatMessage user(String content) {
        return new LlmChatMessage("user", content);
    }

    public static LlmChatMessage assistant(String content) {
        return new LlmChatMessage("assistant", content);
    }

    public static LlmChatMessage system(String content) {
        return new LlmChatMessage("system", content);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmChatMessage that = (LlmChatMessage) o;
        return Objects.equals(role, that.role) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }

    @Override
    public String toString() {
        return "LlmChatMessage{" +
                "role='" + role + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }
}

