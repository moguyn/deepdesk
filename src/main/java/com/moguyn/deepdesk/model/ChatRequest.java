package com.moguyn.deepdesk.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequest(
        String model,
        @JsonProperty("conversation_id")
        String conversationId,
        @JsonProperty("max_tokens")
        Integer maxTokens,
        List<ChatMessage> messages) {

}
