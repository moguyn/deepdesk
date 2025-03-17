package com.moguyn.deepdesk.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChatRequestTest {

    @Test
    void testChatRequestCreation() {
        // Arrange
        String model = "test-model";
        String conversationId = "test-conversation";
        Integer maxTokens = 100;
        List<ChatMessage> messages = List.of(
            new ChatMessage("user", "Hello"),
            new ChatMessage("assistant", "Hi there")
        );

        // Act
        ChatRequest request = new ChatRequest(model, conversationId, maxTokens, messages);

        // Assert
        assertEquals(model, request.model());
        assertEquals(conversationId, request.conversationId());
        assertEquals(maxTokens, request.maxTokens());
        assertEquals(messages, request.messages());
    }

    @Test
    void testChatRequestEquality() {
        // Arrange
        List<ChatMessage> messages1 = List.of(new ChatMessage("user", "Hello"));
        List<ChatMessage> messages2 = List.of(new ChatMessage("user", "Different"));
        
        ChatRequest request1 = new ChatRequest("model1", "conv1", 100, messages1);
        ChatRequest request2 = new ChatRequest("model1", "conv1", 100, messages1);
        ChatRequest request3 = new ChatRequest("model1", "conv1", 100, messages2);

        // Assert
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
    }
}