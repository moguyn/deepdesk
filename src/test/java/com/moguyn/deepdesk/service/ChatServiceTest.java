package com.moguyn.deepdesk.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;

import com.moguyn.deepdesk.model.ChatAnswer;
import com.moguyn.deepdesk.model.ChatMessage;
import com.moguyn.deepdesk.model.ChatRequest;
import com.moguyn.deepdesk.model.ContentItem;

class ChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<String> promptCaptor;

    private ChatService chatService;

    private static final String TEST_MODEL = "gpt-3.5-turbo";
    private static final String TEST_USER_MESSAGE = "Hello, AI!";
    private static final String TEST_AI_RESPONSE = "Hello! How can I help you today?";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        chatService = new ChatService(chatClient);

        // Setup default mock behavior
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(TEST_AI_RESPONSE);
    }

    @Test
    void processChat_shouldReturnValidResponse() {
        // Prepare test data
        ChatMessage message = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(TEST_MODEL, 100, List.of(message));

        // Execute the service method
        ChatAnswer answer = chatService.processChat(request);

        // Verify ChatClient was called with correct prompt
        verify(chatClient).prompt(promptCaptor.capture());
        assertEquals(TEST_USER_MESSAGE, promptCaptor.getValue());

        // Verify the response structure
        assertNotNull(answer);
        assertEquals(TEST_MODEL, answer.getModel());
        assertEquals("assistant", answer.getRole());
        assertEquals("message", answer.getType());
        assertNotNull(answer.getId());
        assertTrue(answer.getId().startsWith("resp_"));

        // Verify content
        assertNotNull(answer.getContent());
        assertEquals(1, answer.getContent().size());
        ContentItem contentItem = answer.getContent().get(0);
        assertEquals(TEST_AI_RESPONSE, contentItem.getText());
        assertEquals("text", contentItem.getType());
    }

    @Test
    void processChat_withNullResponse_shouldHandleGracefully() {
        // Setup mock to return null
        when(responseSpec.content()).thenReturn(null);

        // Prepare test data
        ChatMessage message = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(TEST_MODEL, 100, List.of(message));

        // Execute the service method
        ChatAnswer answer = chatService.processChat(request);

        // Verify response handles null gracefully
        assertNotNull(answer);
        assertNotNull(answer.getContent());
        assertEquals(1, answer.getContent().size());
        ContentItem contentItem = answer.getContent().get(0);
        assertEquals("", contentItem.getText());
        assertEquals("text", contentItem.getType());
    }

    @Test
    void processChat_withMultipleMessages_shouldUseLastMessage() {
        // Prepare test data with multiple messages
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", "You are a helpful assistant."),
                new ChatMessage("user", "What's the weather?"),
                new ChatMessage("assistant", "I don't have real-time weather data."),
                new ChatMessage("user", TEST_USER_MESSAGE)
        );
        ChatRequest request = new ChatRequest(TEST_MODEL, 100, messages);

        // Execute the service method
        chatService.processChat(request);

        // Verify that only the last user message was used for the prompt
        verify(chatClient).prompt(promptCaptor.capture());
        assertEquals(TEST_USER_MESSAGE, promptCaptor.getValue());
    }
}
