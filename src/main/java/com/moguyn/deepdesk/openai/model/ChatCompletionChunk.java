package com.moguyn.deepdesk.openai.model;

import java.util.List;

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
public class ChatCompletionChunk {

    private String id;
    private String object;
    private Long created;
    private String model;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    private List<ChunkChoice> choices;
    private OpenAiUsage usage;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChunkChoice {

        private Integer index;

        private ChatMessage delta;

        @JsonProperty("finish_reason")
        private String finishReason;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object logprobs;
    }
}
