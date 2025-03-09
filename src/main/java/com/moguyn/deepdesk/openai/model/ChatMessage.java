package com.moguyn.deepdesk.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private String role;
    private String content;
    private String name;

    @JsonProperty("tool_calls")
    private Object toolCalls;

    // Constructor for basic messages
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
