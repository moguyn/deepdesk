package com.moguyn.deepdesk.openai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Choice {

    private Integer index;

    private ChatMessage message;

    @JsonProperty("finish_reason")
    private String finishReason;

    private Object logprobs;
}
