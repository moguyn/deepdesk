package com.moguyn.deepdesk.controller;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moguyn.deepdesk.chat.OpenAiService;
import com.moguyn.deepdesk.openai.model.ChatCompletionChunk;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;
import com.moguyn.deepdesk.openai.model.Choice;
import com.moguyn.deepdesk.openai.model.OpenAiUsage;

import reactor.core.publisher.Flux;

/**
 * Integration test for the OpenAiChatController.
 */
@WebMvcTest(OpenAiChatController.class)
@ActiveProfiles("test")
@Import(TestConfig.class)
public class OpenAiChatControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OpenAiService openAiService;

    @Captor
    private ArgumentCaptor<ChatCompletionRequest> chatRequestCaptor;

    private MockMvc mockMvc;

    private static final String TEST_MODEL = "gpt-3.5-turbo";
    private static final String TEST_USER_MESSAGE = "Hello, AI!";
    private static final String TEST_AI_RESPONSE = "Hello! How can I help you today?";
    private static final String TEST_RESPONSE_ID = "chatcmpl-123456";
    private static final List<String> TEST_MODELS = Arrays.asList("gpt-3.5-turbo", "gpt-4");

    private ChatCompletionResponse defaultResponse;

    @BeforeEach
    public void setUp() {
        // Reset mock before each test
        reset(openAiService);

        // Configure MockMvc with streaming support
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        // Set up reusable objects
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(TEST_AI_RESPONSE);

        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setMessage(assistantMessage);
        choice.setFinishReason("stop");

        OpenAiUsage usage = new OpenAiUsage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);

        defaultResponse = new ChatCompletionResponse();
        defaultResponse.setId(TEST_RESPONSE_ID);
        defaultResponse.setObject("chat.completion");
        defaultResponse.setCreated(1677858242L);
        defaultResponse.setModel(TEST_MODEL);
        defaultResponse.setChoices(Arrays.asList(choice));
        defaultResponse.setUsage(usage);
    }

    @Test
    void chat_shouldReturnValidResponse() throws Exception {
        // Arrange
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(TEST_USER_MESSAGE);

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(TEST_MODEL);
        request.setMessages(Arrays.asList(message));

        when(openAiService.processChat(any(ChatCompletionRequest.class))).thenReturn(defaultResponse);

        // Act & Assert
        mockMvc.perform(post("/openai/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_RESPONSE_ID))
                .andExpect(jsonPath("$.choices[0].message.content").value(TEST_AI_RESPONSE));

        // Verify the service was called with the correct request
        verify(openAiService).processChat(chatRequestCaptor.capture());
        ChatCompletionRequest capturedRequest = chatRequestCaptor.getValue();
        assertEquals(TEST_MODEL, capturedRequest.getModel());
        assertEquals(1, capturedRequest.getMessages().size());
        assertEquals("user", capturedRequest.getMessages().get(0).getRole());
        assertEquals(TEST_USER_MESSAGE, capturedRequest.getMessages().get(0).getContent());
    }

    @Test
    void chat_shouldReturnStreamResponse_whenStreaming() throws Exception {
        // Arrange
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(TEST_USER_MESSAGE);

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(TEST_MODEL);
        request.setMessages(Arrays.asList(message));
        request.setStream(true);

        ChatCompletionChunk chunk = ChatCompletionChunk.builder()
                .id("chunk-1")
                .object("chat.completion.chunk")
                .created(1677858242L)
                .model(TEST_MODEL)
                .choices(Arrays.asList(ChatCompletionChunk.ChunkChoice.builder()
                        .index(0)
                        .delta(ChatMessage.builder().content("Hello").build())
                        .finishReason("stop")
                        .build()))
                .build();

        when(openAiService.streamChat(any(ChatCompletionRequest.class)))
                .thenReturn(Flux.just(chunk));

        // Act & Assert
        mockMvc.perform(post("/openai/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM));

        // Verify the service was called with the correct request
        verify(openAiService).streamChat(chatRequestCaptor.capture());
        ChatCompletionRequest capturedRequest = chatRequestCaptor.getValue();
        assertEquals(TEST_MODEL, capturedRequest.getModel());
        assertEquals(1, capturedRequest.getMessages().size());
        assertEquals("user", capturedRequest.getMessages().get(0).getRole());
        assertEquals(TEST_USER_MESSAGE, capturedRequest.getMessages().get(0).getContent());
        assertTrue(capturedRequest.isStream());
    }

    @Test
    void chatStream_shouldHandleErrorsGracefully() throws Exception {
        // Arrange
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(TEST_USER_MESSAGE);

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(TEST_MODEL);
        request.setMessages(Arrays.asList(message));
        request.setStream(true);

        // Simulate an error in the service
        RuntimeException testException = new RuntimeException("Test error message");
        when(openAiService.streamChat(any(ChatCompletionRequest.class)))
                .thenReturn(Flux.error(testException));

        // Act & Assert - we're not checking the content type here, just that it completes
        mockMvc.perform(post("/openai/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify the service was called
        verify(openAiService).streamChat(any(ChatCompletionRequest.class));
    }

    @Test
    void chatStream_shouldHandleNonStreamingRequestCorrectly() throws Exception {
        // Arrange
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(TEST_USER_MESSAGE);

        // Create a request with stream=false
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(TEST_MODEL);
        request.setMessages(Arrays.asList(message));
        request.setStream(false);

        // Act & Assert - expect an UnsupportedOperationException
        try {
            mockMvc.perform(post("/openai/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            // Verify the exception is of the expected type
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            assertEquals("We expect stream = true", e.getCause().getMessage());
        }

        // Verify the service was not called
        verify(openAiService, never()).streamChat(any(ChatCompletionRequest.class));
    }

    @Test
    void getModels_shouldReturnAvailableModels() throws Exception {
        // Arrange
        when(openAiService.getModels()).thenReturn(TEST_MODELS);

        // Act & Assert
        mockMvc.perform(get("/openai/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(TEST_MODELS.get(0)))
                .andExpect(jsonPath("$[1]").value(TEST_MODELS.get(1)));
    }

    @Test
    void modelsOptions_shouldReturnCorsHeaders() throws Exception {
        // Act & Assert
        mockMvc.perform(options("/openai/models"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));
    }
}
