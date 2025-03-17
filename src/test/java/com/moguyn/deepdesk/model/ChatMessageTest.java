package com.moguyn.deepdesk.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    void testChatMessageCreation() {
        // Arrange
        String role = "user";
        String content = "Hello, how can you help me?";

        // Act
        ChatMessage message = new ChatMessage(role, content);

        // Assert
        assertEquals(role, message.role());
        assertEquals(content, message.content());
    }

    @Test
    void testChatMessageEquality() {
        // Arrange
        ChatMessage message1 = new ChatMessage("user", "Hello");
        ChatMessage message2 = new ChatMessage("user", "Hello");
        ChatMessage message3 = new ChatMessage("assistant", "Hello");
        ChatMessage message4 = new ChatMessage("user", "Different");

        // Assert
        assertEquals(message1, message2);
        assertNotEquals(message1, message3);
        assertNotEquals(message1, message4);
    }
}