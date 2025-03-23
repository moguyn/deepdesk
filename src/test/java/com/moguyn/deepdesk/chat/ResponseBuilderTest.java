package com.moguyn.deepdesk.chat;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;

@ExtendWith(MockitoExtension.class)
public class ResponseBuilderTest {

    @Mock
    private TokenCountEstimator tokenCountEstimator;

    private ResponseBuilder responseBuilder;

    @BeforeEach
    public void setUp() {
        responseBuilder = new ResponseBuilder(tokenCountEstimator);
    }

    @Test
    void shouldBuildResponseCorrectly() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("gpt-4");
        request.setMessages(List.of(
                ChatMessage.builder().role("user").content("What is AI?").build()
        ));

        String reply = "AI is artificial intelligence.";

        // Mock token counts
        when(tokenCountEstimator.estimate("What is AI?")).thenReturn(5);
        when(tokenCountEstimator.estimate("AI is artificial intelligence.")).thenReturn(8);

        // When
        ChatCompletionResponse response = responseBuilder.buildResponse(request, reply);

        // Then
        assertNotNull(response);
        assertEquals("chat.completion", response.getObject());
        assertEquals("gpt-4", response.getModel());
        assertEquals(1, response.getChoices().size());
        assertEquals("assistant", response.getChoices().get(0).message().role());
        assertEquals("AI is artificial intelligence.", response.getChoices().get(0).message().content());
        assertEquals("stop", response.getChoices().get(0).finishReason());

        // Verify usage stats
        assertEquals(5, response.getUsage().promptTokens());
        assertEquals(8, response.getUsage().completionTokens());
        assertEquals(13, response.getUsage().totalTokens());
    }

    @Test
    void shouldHandleEmptyReply() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel("gpt-4");
        request.setMessages(List.of(
                ChatMessage.builder().role("user").content("Hello").build()
        ));

        String reply = "";

        // Mock token counts
        when(tokenCountEstimator.estimate("Hello")).thenReturn(2);
        when(tokenCountEstimator.estimate("")).thenReturn(0);

        // When
        ChatCompletionResponse response = responseBuilder.buildResponse(request, reply);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getChoices().size());
        assertEquals("", response.getChoices().get(0).message().content());

        // Verify usage stats
        assertEquals(2, response.getUsage().promptTokens());
        assertEquals(0, response.getUsage().completionTokens());
        assertEquals(2, response.getUsage().totalTokens());
    }
}
