package com.moguyn.deepdesk.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnswer {

    private List<ContentItem> content;
    private String id;
    private String model;
    private String role;

    @JsonProperty("stop_reason")
    private String stopReason;

    @JsonProperty("stop_sequence")
    private String stopSequence;

    private String type;
    private Usage usage;
}
