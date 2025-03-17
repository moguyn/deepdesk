package com.moguyn.deepdesk.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Choice(
        Integer index,
        ChatMessage message,
        @JsonProperty("finish_reason")
        String finishReason,
        Object logprobs) {

}
