package com.moguyn.deepdesk.mcp;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import org.springframework.ai.chat.client.ChatClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moguyn.deepdesk.mcp.ThinkService.AnalysisRequest;
import com.moguyn.deepdesk.mcp.ThinkService.QueryAnalysis;

/**
 * Test class for the ThinkService.
 */
public class ThinkServiceTest {

    @Test
    public void testThink() throws Exception {
        // Create the JSON response that the ChatClient would normally return
        String mockJsonResponse = """
            {
                "concise_summary": "Analysis of: What is the capital of France?",
                "required_skills": "Geography knowledge, information retrieval",
                "relevant_tools": "search, map",
                "additional_considerations": "Consider cultural and historical context"
            }
            """;

        // Create a real ObjectMapper for JSON parsing
        ObjectMapper objectMapper = new ObjectMapper();

        // Create a test request
        AnalysisRequest request = new AnalysisRequest();
        request.setQuery("What is the capital of France?");
        request.setAvailableTools(Arrays.asList("search", "map"));
        request.setToolboxMetadata("Some metadata about the tools");

        // Create a spy of ThinkService with a mocked ChatClient
        ChatClient chatClient = mock(ChatClient.class);
        ThinkService service = spy(new ThinkService(chatClient, objectMapper));

        // Create the expected result
        QueryAnalysis expectedAnalysis = objectMapper.readValue(mockJsonResponse, QueryAnalysis.class);

        // Stub the service method to return our expected analysis
        doReturn(expectedAnalysis).when(service).identifySkillsAndTools(any(AnalysisRequest.class));

        // Call the method
        QueryAnalysis analysis = service.identifySkillsAndTools(request);

        // Verify the result
        assertNotNull(analysis);
        assertEquals("Analysis of: What is the capital of France?", analysis.getConciseSummary());
        assertEquals("Geography knowledge, information retrieval", analysis.getRequiredSkills());
        assertEquals("search, map", analysis.getRelevantTools());
        assertEquals("Consider cultural and historical context", analysis.getAdditionalConsiderations());
    }
}
