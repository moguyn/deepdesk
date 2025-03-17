package com.moguyn.deepdesk.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatAnswer(
        List<ContentItem> content,
        String id,
        String model,
        String role,
        @JsonProperty("stop_reason")
        String stopReason,
        @JsonProperty("stop_sequence")
        String stopSequence,
        String type,
        Usage usage) {

    // Builder implementation to replace Lombok @Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<ContentItem> content;
        private String id;
        private String model;
        private String role;
        private String stopReason;
        private String stopSequence;
        private String type;
        private Usage usage;

        public Builder content(List<ContentItem> content) {
            this.content = content;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder stopReason(String stopReason) {
            this.stopReason = stopReason;
            return this;
        }

        public Builder stopSequence(String stopSequence) {
            this.stopSequence = stopSequence;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public ChatAnswer build() {
            return new ChatAnswer(content, id, model, role, stopReason, stopSequence, type, usage);
        }
    }
}
