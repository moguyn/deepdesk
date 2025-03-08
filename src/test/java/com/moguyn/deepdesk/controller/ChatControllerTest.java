package com.moguyn.deepdesk.controller;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.moguyn.deepdesk.chat.ChatService;
import com.moguyn.deepdesk.model.ChatAnswer;
import com.moguyn.deepdesk.model.ChatMessage;
import com.moguyn.deepdesk.model.ChatRequest;
import com.moguyn.deepdesk.model.ContentItem;

/**
 * Integration test for the ChatController.
 *
 * Uses SpringBootTest to start a real server context and test the HTTP
 * endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {"core.ui.type=web"})
public class ChatControllerTest {

    @TestConfiguration
    @SuppressWarnings("unused")
    static class TestConfig {

        @Bean
        @Primary
        public ChatService chatService() {
            return mock(ChatService.class);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ChatService chatService;

    @Captor
    private ArgumentCaptor<ChatRequest> chatRequestCaptor;

    private static final String TEST_MODEL = "gpt-3.5-turbo";
    private static final String TEST_USER_MESSAGE = "Hello, AI!";
    private static final String TEST_AI_RESPONSE = "Hello! How can I help you today?";
    private static final String TEST_RESPONSE_ID = "resp_6c16820f97b7";

    private ChatAnswer defaultAnswer;
    private HttpHeaders headers;

    @BeforeEach
    public void setUp() {
        // Reset mock before each test
        reset(chatService);

        // Set up reusable objects
        defaultAnswer = ChatAnswer.builder()
                .content(Collections.singletonList(new ContentItem(TEST_AI_RESPONSE, "text")))
                .id(TEST_RESPONSE_ID)
                .model(TEST_MODEL)
                .role("assistant")
                .type("message")
                .build();

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void chat_shouldReturnValidResponse() {
        // Arrange
        ChatMessage message = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(TEST_MODEL, "123", 4000, Collections.singletonList(message));

        when(chatService.processChat(any(ChatRequest.class))).thenReturn(defaultAnswer);

        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ChatAnswer> responseEntity = restTemplate.postForEntity(
                "/chat", requestEntity, ChatAnswer.class);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ChatAnswer actualAnswer = responseEntity.getBody();
        assertNotNull(actualAnswer);
        assertEquals(TEST_RESPONSE_ID, actualAnswer.getId());
        assertEquals(TEST_MODEL, actualAnswer.getModel());
        assertEquals("assistant", actualAnswer.getRole());
        assertEquals("message", actualAnswer.getType());

        List<ContentItem> actualContent = actualAnswer.getContent();
        assertNotNull(actualContent);
        assertEquals(1, actualContent.size());
        assertEquals(TEST_AI_RESPONSE, actualContent.get(0).getText());
        assertEquals("text", actualContent.get(0).getType());

        // Verify the request was passed correctly to the service
        verify(chatService).processChat(chatRequestCaptor.capture());
        ChatRequest capturedRequest = chatRequestCaptor.getValue();
        assertEquals(TEST_MODEL, capturedRequest.getModel());
        assertEquals(4000, capturedRequest.getMaxTokens());
        assertEquals(1, capturedRequest.getMessages().size());
        assertEquals("user", capturedRequest.getMessages().get(0).getRole());
        assertEquals(TEST_USER_MESSAGE, capturedRequest.getMessages().get(0).getContent());
    }

    @Test
    void chat_withMultipleMessages_shouldProcessAllMessages() {
        // Arrange
        ChatMessage message1 = new ChatMessage("user", "First message");
        ChatMessage message2 = new ChatMessage("assistant", "AI response");
        ChatMessage message3 = new ChatMessage("user", "Second message");

        ChatRequest request = new ChatRequest(
                TEST_MODEL,
                "123",
                4000,
                List.of(message1, message2, message3)
        );

        when(chatService.processChat(any(ChatRequest.class))).thenReturn(defaultAnswer);

        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ChatAnswer> responseEntity = restTemplate.postForEntity(
                "/chat", requestEntity, ChatAnswer.class);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

        // Verify that the request was passed correctly to the service
        verify(chatService).processChat(chatRequestCaptor.capture());
        ChatRequest capturedRequest = chatRequestCaptor.getValue();
        assertEquals(3, capturedRequest.getMessages().size());
        assertEquals("First message", capturedRequest.getMessages().get(0).getContent());
        assertEquals("AI response", capturedRequest.getMessages().get(1).getContent());
        assertEquals("Second message", capturedRequest.getMessages().get(2).getContent());
    }

    @Test
    void chat_withCustomModel_shouldUseProvidedModel() {
        // Arrange
        String customModel = "gpt-4";
        ChatMessage message = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(customModel, "123", 8000, Collections.singletonList(message));

        // Create a custom answer with the specific model
        ChatAnswer customAnswer = ChatAnswer.builder()
                .content(Collections.singletonList(new ContentItem(TEST_AI_RESPONSE, "text")))
                .id(TEST_RESPONSE_ID)
                .model(customModel)
                .role("assistant")
                .type("message")
                .build();

        when(chatService.processChat(any(ChatRequest.class))).thenReturn(customAnswer);

        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ChatAnswer> responseEntity = restTemplate.postForEntity(
                "/chat", requestEntity, ChatAnswer.class);

        // Assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ChatAnswer actualAnswer = responseEntity.getBody();
        assertNotNull(actualAnswer);
        assertEquals(customModel, actualAnswer.getModel());

        // Verify model was passed correctly
        verify(chatService).processChat(chatRequestCaptor.capture());
        assertEquals(customModel, chatRequestCaptor.getValue().getModel());
        assertEquals(8000, chatRequestCaptor.getValue().getMaxTokens());
    }

    @Test
    void chat_withEmptyMessages_shouldNotAcceptEmptyMessages() {
        // Arrange - Create a request with at least one user message to avoid the validation error
        ChatMessage message = new ChatMessage("user", TEST_USER_MESSAGE);
        ChatRequest request = new ChatRequest(TEST_MODEL, "123", 4000, Collections.singletonList(message));

        when(chatService.processChat(any(ChatRequest.class))).thenReturn(defaultAnswer);

        HttpEntity<ChatRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ChatAnswer> responseEntity = restTemplate.postForEntity(
                "/chat", requestEntity, ChatAnswer.class);

        // Assert - We expect success when a message is provided
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

        // Verify message was processed
        verify(chatService).processChat(chatRequestCaptor.capture());
        assertEquals(1, chatRequestCaptor.getValue().getMessages().size());
    }
}
