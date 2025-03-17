package com.moguyn.deepdesk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Usage(
        @JsonProperty("input_tokens")
        Integer inputTokens,
        @JsonProperty("output_tokens")
        Integer outputTokens) {

}
