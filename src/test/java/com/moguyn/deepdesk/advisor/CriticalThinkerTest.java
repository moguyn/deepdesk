package com.moguyn.deepdesk.advisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CriticalThinkerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ToolCallbackProvider toolCallbackProvider;

    private CriticalThinker criticalThinker;

    @BeforeEach
    public void setUp() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        criticalThinker = new CriticalThinker(chatClientBuilder, toolCallbackProvider, objectMapper, 100);
    }

    @Test
    void testGetName() {
        // Act & Assert
        assertEquals("critical-thinker", criticalThinker.getName());
    }

    @Test
    void testAfterWithNullResponse() {
        // Arrange
        AdvisedResponse advisedResponse = null;

        // Act
        AdvisedResponse result = criticalThinker.after(advisedResponse);

        // Assert
        assertSame(advisedResponse, result);
    }

    @Test
    void testAfterWithNullResponseContent() {
        // Arrange
        AdvisedResponse advisedResponse = mock(AdvisedResponse.class);
        when(advisedResponse.response()).thenReturn(null);

        // Act
        AdvisedResponse result = criticalThinker.after(advisedResponse);

        // Assert
        assertSame(advisedResponse, result);
    }

    @Test
    void testAfterWithNullOutput() {
        // Arrange
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(null);
        when(chatResponse.getResult()).thenReturn(generation);

        AdvisedResponse advisedResponse = mock(AdvisedResponse.class);
        when(advisedResponse.response()).thenReturn(chatResponse);

        // Act
        AdvisedResponse result = criticalThinker.after(advisedResponse);

        // Assert
        assertSame(advisedResponse, result);
    }
}
