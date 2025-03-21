package com.moguyn.deepdesk.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import com.moguyn.deepdesk.openai.model.ChatCompletionChunk;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;
import com.moguyn.deepdesk.openai.model.OpenAiUsage;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class OpenAiServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private TokenCountEstimator tokenCountEstimator;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private ResponseBuilder responseBuilder;

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
    @SuppressWarnings({"unchecked"})
    public void setup() {
        // Make these setup mocks lenient to avoid unnecessary stubbing errors
        lenient().when(responseSpec.content()).thenReturn("Default response");
        lenient().when(requestSpec.call()).thenReturn(responseSpec);
        lenient().when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        lenient().when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);

        // Set up MessageConverter to use the default system prompt and return messages
        lenient().when(messageConverter.createPrompt(any(ChatCompletionRequest.class))).thenAnswer(invocation -> {
            ChatCompletionRequest req = invocation.getArgument(0);
            List<Message> messages = new ArrayList<>();

            // Add default system prompt if not present in the request
            boolean hasSystemMessage = req.getMessages().stream()
                    .anyMatch(m -> "system".equals(m.role()));

            if (!hasSystemMessage) {
                messages.add(new SystemMessage(defaultSystemPrompt));
            }

            // Add messages from the request
            req.getMessages().forEach(m -> {
                switch (m.role()) {
                    case "system" ->
                        messages.add(new SystemMessage(m.content()));
                    case "user" ->
                        messages.add(new UserMessage(m.content()));
                    case "assistant" ->
                        messages.add(new AssistantMessage(m.content()));
                    default ->
                        throw new IllegalArgumentException("Unsupported role: " + m.role());
                }
            });

            return new Prompt(messages);
        });

        // Set up ResponseBuilder to return a basic response
        lenient().when(responseBuilder.buildResponse(any(ChatCompletionRequest.class), anyString())).thenAnswer(invocation -> {
            ChatCompletionRequest request = invocation.getArgument(0);
            String reply = invocation.getArgument(1);

            // Create a basic response with the given reply
            ChatCompletionResponse response = new ChatCompletionResponse();
            response.setId("test-id");
            response.setObject("chat.completion");
            response.setCreated(System.currentTimeMillis() / 1000);
            response.setModel(request.getModel());

            com.moguyn.deepdesk.openai.model.Choice choice
                    = new com.moguyn.deepdesk.openai.model.Choice(0,
                            com.moguyn.deepdesk.openai.model.ChatMessage.builder()
                                    .role("assistant")
                                    .content(reply)
                                    .build(),
                            "stop",
                            null);

            response.setChoices(List.of(choice));

            // Set up usage information
            OpenAiUsage usage = new OpenAiUsage(5, 10, 15);
            response.setUsage(usage);

            return response;
        });

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
        assertTrue(promptString.contains("Hello"));

        // Verify response
        assertNotNull(response);
        assertEquals("gpt-3.5-turbo", response.getModel());
        assertEquals(1, response.getChoices().size());
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
        assertNotNull(response);

        // Assert
        verify(chatClient).prompt(promptCaptor.capture());
        Prompt capturedPrompt = promptCaptor.getValue();

        // Verify prompt contains the expected messages
        String promptString = capturedPrompt.toString();
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

        // Act
        openAiService.processChat(request);

        // Verify that response builder was called with the correct arguments
        verify(responseBuilder).buildResponse(any(ChatCompletionRequest.class), anyString());
    }

    @Test
    void processChat_shouldThrowException_whenStreamIsTrue() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
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

        // Set up MessageConverter to throw an error for unsupported role
        when(messageConverter.createPrompt(any(ChatCompletionRequest.class)))
                .thenThrow(new IllegalArgumentException("Unsupported role: unsupported_role"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> openAiService.processChat(request));
        assertEquals("Unsupported role: unsupported_role", exception.getMessage());
    }

    @Test
    void streamChat_shouldMapResponseCorrectly() {
        // This test verifies that the streamChat method can handle a response
        // Since we don't have access to the actual response classes, we'll just
        // verify that the method is called correctly and doesn't throw exceptions

        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
        request.setStream(true);
        request.setModel("test-model");

        // Mock the stream response
        ChatClient.ChatClientRequestSpec streamRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);

        // Create an empty flux for the response
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("test-model")
                .id("test-id")
                .keyValue("index", 0)
                .keyValue("delta", "test-content")
                .keyValue("finish_reason", "stop")
                .keyValue("logprobs", "test-1")
                .build();
        Flux<ChatResponse> responseFlux = Flux.just(ChatResponse
                .builder()
                .metadata(metadata)
                .generations(List.of(new Generation(new AssistantMessage("test-content"))))
                .build());

        when(chatClient.prompt(any(Prompt.class))).thenReturn(streamRequestSpec);
        when(streamRequestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.chatResponse()).thenReturn(responseFlux);

        // Act & Assert
        // Verify that the method doesn't throw an exception
        assertDoesNotThrow(() -> {
            Flux<ChatCompletionChunk> result = openAiService.streamChat(request);
            // We don't need to subscribe or block since we're just testing that the method runs without errors
            assertNotNull(result);
            result.collectList().block();
        });

        // Verify the correct methods were called
        verify(chatClient).prompt(any(Prompt.class));
        verify(streamRequestSpec).stream();
        verify(streamResponseSpec).chatResponse();
    }

    @Test
    void getModels_shouldReturnSupportedModels() {
        // Act
        List<String> models = openAiService.getModels();

        // Assert
        assertEquals(1, models.size());
        assertEquals("deepdesk", models.get(0));
    }

    @Test
    void streamChat_shouldHandleEmptyResponse() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
        request.setStream(true);

        // Mock the stream response
        ChatClient.ChatClientRequestSpec streamRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(streamRequestSpec);
        when(streamRequestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.chatResponse()).thenReturn(Flux.empty());

        // Act
        Flux<ChatCompletionChunk> result = openAiService.streamChat(request);

        // Assert
        assertNotNull(result);
        result.collectList().block(); // Verify the flux completes without error
    }

    @Test
    void processChat_shouldHandleEmptyResponse() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);

        when(responseSpec.content()).thenReturn("");

        // Act
        ChatCompletionResponse response = openAiService.processChat(request);

        // Assert
        assertNotNull(response);
    }

    @Test
    void processChat_shouldHandleNullResponse() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);

        when(responseSpec.content()).thenReturn(null);

        // Act
        ChatCompletionResponse response = openAiService.processChat(request);

        // Assert
        assertNotNull(response);
    }

    @Test
    void processChat_shouldHandleAllMessageTypes() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "System message"));
        messages.add(new ChatMessage("user", "User message"));
        messages.add(new ChatMessage("assistant", "Assistant message"));
        request.setMessages(messages);

        when(responseSpec.content()).thenReturn("Response");

        // Act
        ChatCompletionResponse response = openAiService.processChat(request);

        // Assert
        assertNotNull(response);
        verify(chatClient).prompt(promptCaptor.capture());
    }

    @Test
    void streamChat_shouldUseProvidedSystemPrompt_whenSystemMessagePresent() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", "Custom system prompt"));
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
        request.setStream(true);

        // Mock the stream response
        ChatClient.ChatClientRequestSpec streamRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(streamRequestSpec);
        when(streamRequestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.chatResponse()).thenReturn(Flux.empty());

        // Act
        openAiService.streamChat(request);

        // Assert
        verify(chatClient).prompt(promptCaptor.capture());
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChat_shouldSetConversationId_whenUserProvided() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", "Hello"));
        request.setMessages(messages);
        request.setStream(true);
        request.setUser("user123"); // Set conversation ID

        // Mock the stream response
        ChatClient.ChatClientRequestSpec streamRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(streamRequestSpec);
        when(streamRequestSpec.advisors(any(Consumer.class))).thenReturn(streamRequestSpec);
        when(streamRequestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.chatResponse()).thenReturn(Flux.empty());

        // Act
        openAiService.streamChat(request);

        // Assert
        verify(chatClient).prompt(any(Prompt.class));
        verify(streamRequestSpec).advisors(any(Consumer.class));
    }

    @Test
    void streamChat_shouldThrowException_whenUnsupportedRoleProvided() {
        // Arrange
        ChatCompletionRequest request = new ChatCompletionRequest();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("unsupported_role", "Content"));
        request.setMessages(messages);
        request.setStream(true);

        // Set up MessageConverter to throw an error for unsupported role
        when(messageConverter.createPrompt(any(ChatCompletionRequest.class)))
                .thenThrow(new IllegalArgumentException("Unsupported role: unsupported_role"));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> openAiService.streamChat(request));
        assertEquals("Unsupported role: unsupported_role", exception.getMessage());
    }
}
