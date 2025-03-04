package com.moguyn.deepdesk.service;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import com.moguyn.deepdesk.model.ChatAnswer;
import com.moguyn.deepdesk.model.ChatMessage;
import com.moguyn.deepdesk.model.ChatRequest;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @InjectMocks
    private ChatService chatService;

    private static final String TEST_SYSTEM_PROMPT = "You are a helpful AI assistant.";
    private static final String TEST_USER_MESSAGE = "Hello, AI!";
    private static final String TEST_AI_RESPONSE = "Hello! How can I help you today?";
    private static final String TEST_MODEL = "gpt-3.5-turbo";

    @BeforeEach
    public void setUp() {
        // Set the system prompt using reflection since it's loaded from properties
        ReflectionTestUtils.setField(chatService, "systemPrompt", TEST_SYSTEM_PROMPT);
    }

    @Test
    void processChat_shouldReturnValidResponse() {
        // Arrange
        ChatMessage userMessage = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(TEST_MODEL, 4000, Collections.singletonList(userMessage));

        // Mock chat client chain
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(TEST_SYSTEM_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.user(TEST_USER_MESSAGE)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(TEST_AI_RESPONSE);

        // Act
        ChatAnswer result = chatService.processChat(request);

        // Assert
        assertNotNull(result);
        assertEquals("assistant", result.getRole());
        assertEquals(TEST_MODEL, result.getModel());
        assertNotNull(result.getId());
        assertEquals("message", result.getType());
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_AI_RESPONSE, result.getContent().get(0).getText());
        assertEquals("text", result.getContent().get(0).getType());

        // Verify interactions
        verify(chatClient, times(1)).prompt();
        verify(requestSpec, times(1)).system(TEST_SYSTEM_PROMPT);
        verify(requestSpec, times(1)).user(TEST_USER_MESSAGE);
        verify(requestSpec, times(1)).call();
        verify(responseSpec, times(1)).content();
    }

    @Test
    void processChat_withMultipleUserMessages_shouldAddAllUserMessages() {
        // Arrange
        ChatMessage userMessage1 = new ChatMessage("user", "First message");
        ChatMessage userMessage2 = new ChatMessage("user", "Second message");
        ChatMessage assistantMessage = new ChatMessage("assistant", "AI response");
        ChatRequest request = new ChatRequest(
                TEST_MODEL,
                4000,
                Arrays.asList(userMessage1, assistantMessage, userMessage2)
        );

        // Mock chat client chain
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(TEST_SYSTEM_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(TEST_AI_RESPONSE);

        // Act
        ChatAnswer result = chatService.processChat(request);

        // Assert
        assertNotNull(result);

        // Verify interactions
        verify(chatClient, times(1)).prompt();
        verify(requestSpec, times(1)).system(TEST_SYSTEM_PROMPT);
        verify(requestSpec, times(1)).user("First message");
        verify(requestSpec, times(1)).user("Second message");
        verify(requestSpec, times(0)).user("AI response"); // Should not add assistant messages
        verify(requestSpec, times(1)).call();
        verify(responseSpec, times(1)).content();
    }

    @Test
    void processChat_withEmptyResponse_shouldHandleGracefully() {
        // Arrange
        ChatMessage userMessage = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(TEST_MODEL, 4000, Collections.singletonList(userMessage));

        // Mock chat client chain
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(TEST_SYSTEM_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.user(TEST_USER_MESSAGE)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        // Act
        ChatAnswer result = chatService.processChat(request);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getContent().get(0).getText());
    }
}
