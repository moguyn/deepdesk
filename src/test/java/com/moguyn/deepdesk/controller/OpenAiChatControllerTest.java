package com.moguyn.deepdesk.controller;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moguyn.deepdesk.chat.OpenAiService;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;
import com.moguyn.deepdesk.openai.model.Choice;
import com.moguyn.deepdesk.openai.model.OpenAiUsage;

/**
 * Integration test for the OpenAiChatController.
 */
@WebMvcTest(OpenAiChatController.class)
@ActiveProfiles("test")
public class OpenAiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OpenAiService openAiService;

    @Captor
    private ArgumentCaptor<ChatCompletionRequest> chatRequestCaptor;

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

        // Act
        MvcResult result = mockMvc.perform(post("/openai/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        ChatCompletionResponse actualResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatCompletionResponse.class);

        assertNotNull(actualResponse);
        assertEquals(TEST_RESPONSE_ID, actualResponse.getId());
        assertEquals(TEST_MODEL, actualResponse.getModel());
        assertEquals(1, actualResponse.getChoices().size());
        assertEquals(TEST_AI_RESPONSE, actualResponse.getChoices().get(0).getMessage().getContent());

        // Verify the service was called with the correct request
        verify(openAiService).processChat(chatRequestCaptor.capture());
        ChatCompletionRequest capturedRequest = chatRequestCaptor.getValue();
        assertEquals(TEST_MODEL, capturedRequest.getModel());
        assertEquals(1, capturedRequest.getMessages().size());
        assertEquals("user", capturedRequest.getMessages().get(0).getRole());
        assertEquals(TEST_USER_MESSAGE, capturedRequest.getMessages().get(0).getContent());
    }

    @Test
    void getModels_shouldReturnAvailableModels() throws Exception {
        // Arrange
        when(openAiService.getModels()).thenReturn(TEST_MODELS);

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/openai/models"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<String> actualModels = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                List.class);

        assertEquals(TEST_MODELS.size(), actualModels.size());
        assertEquals(TEST_MODELS.get(0), actualModels.get(0));
        assertEquals(TEST_MODELS.get(1), actualModels.get(1));
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
