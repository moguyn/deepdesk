package com.moguyn.deepdesk.model;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatAnswerTest {

    @Test
    void testChatAnswerCreation() {
        // Arrange
        List<ContentItem> content = List.of(new ContentItem("Hello world", "text"));
        String id = "test-id";
        String model = "test-model";
        String role = "assistant";
        String stopReason = "end_turn";
        String stopSequence = null;
        String type = "text";
        Usage usage = new Usage(10, 20);

        // Act
        ChatAnswer answer = new ChatAnswer(content, id, model, role, stopReason, stopSequence, type, usage);

        // Assert
        assertEquals(content, answer.content());
        assertEquals(id, answer.id());
        assertEquals(model, answer.model());
        assertEquals(role, answer.role());
        assertEquals(stopReason, answer.stopReason());
        assertEquals(stopSequence, answer.stopSequence());
        assertEquals(type, answer.type());
        assertEquals(usage, answer.usage());
    }

    @Test
    void testChatAnswerBuilder() {
        // Arrange
        List<ContentItem> content = List.of(new ContentItem("Hello world", "text"));
        String id = "test-id";
        String model = "test-model";
        String role = "assistant";
        String stopReason = "end_turn";
        String stopSequence = null;
        String type = "text";
        Usage usage = new Usage(10, 20);

        // Act
        ChatAnswer answer = ChatAnswer.builder()
                .content(content)
                .id(id)
                .model(model)
                .role(role)
                .stopReason(stopReason)
                .stopSequence(stopSequence)
                .type(type)
                .usage(usage)
                .build();

        // Assert
        assertEquals(content, answer.content());
        assertEquals(id, answer.id());
        assertEquals(model, answer.model());
        assertEquals(role, answer.role());
        assertEquals(stopReason, answer.stopReason());
        assertEquals(stopSequence, answer.stopSequence());
        assertEquals(type, answer.type());
        assertEquals(usage, answer.usage());
    }
}