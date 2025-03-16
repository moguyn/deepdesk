package com.moguyn.deepdesk.advisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PlanAdvisorTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    
    @Mock
    private ChatClient chatClient;
    
    @Mock
    private ChatMemory chatMemory;
    
    @Mock
    private ToolCallbackProvider toolCallbackProvider;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private PlanAdvisor planAdvisor;
    
    @BeforeEach
    void setUp() {
        // Use lenient() to avoid strict stubbing issues
        lenient().when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        lenient().when(chatClientBuilder.defaultTools(any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        
        planAdvisor = new PlanAdvisor(chatClientBuilder, chatMemory, toolCallbackProvider, objectMapper, 100);
    }
    
    @Test
    void testGetName() {
        assertEquals("plan-advisor", planAdvisor.getName());
    }
    
    @Test
    void testAnalysisRequestToString() {
        // Arrange
        List<String> availableTools = List.of("Tool 1", "Tool 2");
        String query = "test query";
        String context = "test context";
        
        // Act
        PlanAdvisor.AnalysisRequest analysisRequest = new PlanAdvisor.AnalysisRequest(
                query, availableTools, context);
        
        // Assert
        assertNotNull(analysisRequest.toString());
        assertEquals("AnalysisRequest{query='test query'}", analysisRequest.toString());
    }
    
    @Test
    void testQueryPlanCreation() {
        // Arrange
        String summary = "Summary of test query";
        String actionableSteps = "Step 1: Do this\nStep 2: Do that";
        String clarificationQuestions = "Any clarification needed?";
        String additionalConsiderations = "Additional considerations";
        String context = "Context info";
        
        // Act
        PlanAdvisor.QueryPlan queryPlan = new PlanAdvisor.QueryPlan(
                summary, actionableSteps, clarificationQuestions, additionalConsiderations, context);
        
        // Assert
        assertNotNull(queryPlan);
        assertEquals(summary, queryPlan.summary());
        assertEquals(actionableSteps, queryPlan.actionableSteps());
        assertEquals(clarificationQuestions, queryPlan.clarificationQuestions());
        assertEquals(additionalConsiderations, queryPlan.additionalConsiderations());
        assertEquals(context, queryPlan.context());
    }
}
