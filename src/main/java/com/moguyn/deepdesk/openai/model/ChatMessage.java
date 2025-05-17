package com.moguyn.deepdesk.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,
        String content,
        String name,
        @JsonProperty("tool_calls")
        Object toolCalls) {

    public static ChatMessage of(String role, String content) {
        return new ChatMessage(role, content, null, null);
    }
}
