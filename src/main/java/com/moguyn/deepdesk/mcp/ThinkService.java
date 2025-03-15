package com.moguyn.deepdesk.mcp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ThinkService that uses a real LLM for query analysis.
 */
@Service
public class ThinkService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public ThinkService(@Lazy ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyzes a query to determine the skills and tools needed to address it
     * effectively. Uses a real LLM for analysis through Spring AI's ChatClient.
     *
     * @param request The analysis request containing the query and available
     * tools
     * @return An analysis of the query with required skills and relevant tools
     */
    @Tool(description = "Analyze a query to determine the skills and tools needed to address it effectively using a real LLM")
    public QueryAnalysis identifySkillsAndTools(AnalysisRequest request) {
        // Define the system prompt template
        String systemPromptTemplate = """
            Analyze query: {query}
            
            Tools: {availableTools}
            Tools metadata: {toolboxMetadata}
            
            Instructions:
            1. Carefully read and understand the query and any accompanying inputs.
            2. Identify the main objectives or tasks within the query.
            3. List the specific skills that would be necessary to address the query comprehensively.
            4. Examine the available tools in the toolbox and determine which ones might relevant and useful for addressing the query.
            5. Provide a brief explanation for each skill and tool you've identified, describing how it would contribute to answering the query.

            
            Return JSON structure:
            {
                "concise_summary": "Main points of the query",
                "required_skills": "Skills needed with brief explanation",
                "relevant_tools": "Useful tools with brief justification",
                "additional_considerations": "Any other important factors"
            }
            """;

        // Create the system message from the template
        Message systemMessage = new SystemPromptTemplate(systemPromptTemplate)
                .createMessage(Map.of(
                        "query", request.getQuery(),
                        "availableTools", request.getAvailableTools(),
                        "toolboxMetadata", request.getToolboxMetadata() != null ? request.getToolboxMetadata() : "No metadata available"
                ));

        // Create the user message with the query
        Message userMessage = new UserMessage(request.getQuery());

        // Create the prompt with both messages
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            // Call the LLM using the prompt variable directly
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            // Parse the JSON response into our object
            return objectMapper.readValue(response, QueryAnalysis.class);
        } catch (IOException e) {
            // Handle JSON parsing errors
            return new QueryAnalysis(
                    "Analysis of: " + request.getQuery(),
                    "Error parsing LLM response: " + e.getMessage(),
                    "No relevant tools could be determined due to error",
                    "Consider simplifying the query or checking the LLM output format"
            );
        } catch (RuntimeException e) {
            // Handle LLM API errors
            return new QueryAnalysis(
                    "Analysis of: " + request.getQuery(),
                    "Error from LLM: " + e.getMessage(),
                    "No relevant tools could be determined due to error",
                    "Try again with a different query or check API connectivity"
            );
        }
    }

    /**
     * Request object for the think tool.
     */
    public static class AnalysisRequest {

        @JsonProperty("query")
        private String query;

        @JsonProperty("available_tools")
        private List<String> availableTools;

        @JsonProperty("toolbox_metadata")
        private String toolboxMetadata;

        // Getters and setters
        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<String> getAvailableTools() {
            return availableTools;
        }

        public void setAvailableTools(List<String> availableTools) {
            this.availableTools = availableTools;
        }

        public String getToolboxMetadata() {
            return toolboxMetadata;
        }

        public void setToolboxMetadata(String toolboxMetadata) {
            this.toolboxMetadata = toolboxMetadata;
        }
    }

    /**
     * Response object for the think tool.
     */
    public static class QueryAnalysis {

        @JsonProperty("concise_summary")
        private String conciseSummary;

        @JsonProperty("required_skills")
        private String requiredSkills;

        @JsonProperty("relevant_tools")
        private String relevantTools;

        @JsonProperty("additional_considerations")
        private String additionalConsiderations;

        // Default constructor for Jackson
        public QueryAnalysis() {
        }

        public QueryAnalysis(String conciseSummary, String requiredSkills, String relevantTools, String additionalConsiderations) {
            this.conciseSummary = conciseSummary;
            this.requiredSkills = requiredSkills;
            this.relevantTools = relevantTools;
            this.additionalConsiderations = additionalConsiderations;
        }

        // Getters and setters
        public String getConciseSummary() {
            return conciseSummary;
        }

        public void setConciseSummary(String conciseSummary) {
            this.conciseSummary = conciseSummary;
        }

        public String getRequiredSkills() {
            return requiredSkills;
        }

        public void setRequiredSkills(String requiredSkills) {
            this.requiredSkills = requiredSkills;
        }

        public String getRelevantTools() {
            return relevantTools;
        }

        public void setRelevantTools(String relevantTools) {
            this.relevantTools = relevantTools;
        }

        public String getAdditionalConsiderations() {
            return additionalConsiderations;
        }

        public void setAdditionalConsiderations(String additionalConsiderations) {
            this.additionalConsiderations = additionalConsiderations;
        }
    }
}
