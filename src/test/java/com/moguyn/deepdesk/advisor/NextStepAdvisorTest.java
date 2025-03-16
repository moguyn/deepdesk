package com.moguyn.deepdesk.advisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moguyn.deepdesk.advisor.NextStepAdvisor.NextStep;
import com.moguyn.deepdesk.advisor.PlanAdvisor.QueryPlan;

@ExtendWith(MockitoExtension.class)
class NextStepAdvisorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ToolCallbackProvider toolCallbackProvider;

    private NextStepAdvisor nextStepAdvisor;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(ToolCallbackProvider[].class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        nextStepAdvisor = new NextStepAdvisor(chatClientBuilder, toolCallbackProvider, objectMapper, 100);
    }

    @Test
    void testGetName() {
        // Act & Assert
        assertEquals("next-step-advisor", nextStepAdvisor.getName());
    }

    @Test
    void testNextStepRecord() {
        // Arrange & Act
        NextStep nextStep = new NextStep(
                "Summary",
                "Tool",
                "Parameters",
                "Question",
                "AllowedDirectories"
        );

        // Assert
        assertEquals("Summary", nextStep.summary());
        assertEquals("Tool", nextStep.tool());
        assertEquals("Parameters", nextStep.parameters());
        assertEquals("Question", nextStep.question());
        assertEquals("AllowedDirectories", nextStep.allowedDirectories());
    }

    @Test
    void testNextStepWithException() {
        // Arrange
        QueryPlan queryPlan = new QueryPlan(
                "Summary of test query",
                "Step 1: Do this\nStep 2: Do that",
                "Any clarification needed?",
                "Additional considerations",
                "Context information"
        );

        // Mock the toolCallbackProvider to return an empty array
        FunctionCallback[] emptyCallbacks = new FunctionCallback[0];
        when(toolCallbackProvider.getToolCallbacks()).thenReturn(emptyCallbacks);

        // Setup the chat client to throw an exception
        when(chatClient.prompt()).thenThrow(new RuntimeException("API Error"));

        // Act
        NextStep result = nextStepAdvisor.nextStep(queryPlan);

        // Assert
        assertNotNull(result);
        assertEquals("Summary of test query", result.summary());
        assertEquals("", result.tool());
        assertEquals("", result.parameters());
        assertEquals("", result.question());
        assertEquals("", result.allowedDirectories());
    }
}
