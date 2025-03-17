package com.moguyn.deepdesk.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,
        String content,
        String name,
        @JsonProperty("tool_calls")
        Object toolCalls) {

    // Constructor for basic messages
    public ChatMessage(String role, String content) {
        this(role, content, null, null);
    }
}
