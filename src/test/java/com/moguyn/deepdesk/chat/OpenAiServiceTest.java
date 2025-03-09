package com.moguyn.deepdesk.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.test.util.ReflectionTestUtils;

import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;

@ExtendWith(MockitoExtension.class)
class OpenAiServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private TokenCountEstimator tokenCountEstimator;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    @InjectMocks
    private OpenAiService openAiService;

    private final String defaultSystemPrompt = "你是企业级AI助手, 请说中文, 请使用markdown格式输出";

    @BeforeEach
    @SuppressWarnings({"unchecked", "unused"})
    void setup() {
        ReflectionTestUtils.setField(openAiService, "systemPrompt", defaultSystemPrompt);

        // Make these setup mocks lenient to avoid unnecessary stubbing errors
        lenient().when(responseSpec.content()).thenReturn("Default response");
        lenient().when(requestSpec.call()).thenReturn(responseSpec);
        lenient().when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        lenient().when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);

        // Default token count estimation
        lenient().when(tokenCountEstimator.estimate(anyString())).thenReturn(10);
    }

    @Test
    void processChat_shouldAddDefaultSystemPrompt_whenNoSystemMessagePresent() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
        request.setModel("gpt-3.5-turbo");

        when(responseSpec.content()).thenReturn("Hello, how can I help you?");

        // Act
        ChatCompletionResponse response = openAiService.processChat(request);

        // Assert
        verify(chatClient).prompt(promptCaptor.capture());
        Prompt capturedPrompt = promptCaptor.getValue();

        // Verify prompt contains the expected messages (using toString())
        String promptString = capturedPrompt.toString();
        assertTrue(promptString.contains(defaultSystemPrompt));
        assertTrue(promptString.contains("Hello"));

        // Verify response
        assertNotNull(response);
        assertEquals("gpt-3.5-turbo", response.getModel());
        assertEquals(1, response.getChoices().size());
        assertEquals("Hello, how can I help you?", response.getChoices().get(0).getMessage().getContent());
    }

    @Test
    void processChat_shouldUseProvidedSystemPrompt_whenSystemMessagePresent() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "Custom system prompt"));
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);

        when(responseSpec.content()).thenReturn("Hello, how can I help you?");

        // Act
        ChatCompletionResponse response = openAiService.processChat(request);
        String content = response.getChoices().get(0).getMessage().getContent();
        assertNotNull(content);
        assertEquals("Hello, how can I help you?", content);

        // Assert
        verify(chatClient).prompt(promptCaptor.capture());
        Prompt capturedPrompt = promptCaptor.getValue();

        // Verify prompt contains the expected messages
        String promptString = capturedPrompt.toString();
        assertTrue(promptString.contains("Custom system prompt"));
        assertTrue(promptString.contains("Hello"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void processChat_shouldSetConversationId_whenUserProvided() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
        request.setUser("user-123");

        when(responseSpec.content()).thenReturn("Hello, how can I help you?");

        // Act
        openAiService.processChat(request);

        // Assert
        verify(requestSpec).advisors(any(Consumer.class));
    }

    @Test
    void processChat_shouldCalculateTokenUsage() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);

        when(responseSpec.content()).thenReturn("Response text");
        when(tokenCountEstimator.estimate("Hello")).thenReturn(5);
        when(tokenCountEstimator.estimate("Response text")).thenReturn(15);

        // Act
        ChatCompletionResponse response = openAiService.processChat(request);

        // Assert
        assertEquals(5, response.getUsage().getPromptTokens());
        assertEquals(15, response.getUsage().getCompletionTokens());
        assertEquals(20, response.getUsage().getTotalTokens());
    }

    @Test
    void processChat_shouldThrowException_whenStreamIsTrue() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setStream(true);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> openAiService.processChat(request));
        assertEquals("Stream mode should be used with streamChat method", exception.getMessage());
    }

    @Test
    void processChat_shouldThrowException_whenUnsupportedRoleProvided() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("unsupported_role", "Content"));
        request.setMessages(messages);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> openAiService.processChat(request));
        assertEquals("Unsupported role: unsupported_role", exception.getMessage());
    }
}
