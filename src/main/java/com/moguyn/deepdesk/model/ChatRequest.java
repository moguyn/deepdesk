package com.moguyn.deepdesk.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String model;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private List<ChatMessage> messages;
}
