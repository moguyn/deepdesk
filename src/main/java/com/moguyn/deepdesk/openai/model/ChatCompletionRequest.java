package com.moguyn.deepdesk.openai.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;
    private List<ChatMessage> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private Integer n;

    private boolean stream;

    @JsonProperty("stream_options")
    private StreamOptions streamOptions;

    private List<String> stop;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;

    private String user;

    private Object tools;

    @JsonProperty("tool_choice")
    private Object toolChoice;

    @JsonProperty("response_format")
    private Object responseFormat;

    private Integer seed;
}
