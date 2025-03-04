package com.moguyn.deepdesk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usage {

    @JsonProperty("input_tokens")
    private Integer inputTokens;

    @JsonProperty("output_tokens")
    private Integer outputTokens;
}
