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
 * ThinkService that uses a real LLM for query analysis and planning. This
 * service provides tools for analyzing queries, determining next steps, and
 * verifying context completeness.
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
            Task: Analyze the given query with accompanying inputs and determine the skills and tools needed to address it effectively.
            
            Query: {query}
            
            Available tools: {availableTools}
            Tools metadata: {toolboxMetadata}
            
            Instructions:
            1. Carefully read and understand the query and any accompanying inputs.
            2. Identify the main objectives or tasks within the query.
            3. List the specific skills that would be necessary to address the query comprehensively.
            4. Examine the available tools in the toolbox and determine which ones might be relevant and useful for addressing the query.
            5. Provide a brief explanation for each skill and tool you've identified, describing how it would contribute to answering the query.
            
            Your response should include:
            1. A concise summary of the query's main points and objectives.
            2. A list of required skills, with a brief explanation for each.
            3. A list of relevant tools from the toolbox, with a brief explanation of how each tool would be utilized.
            4. Any additional considerations that might be important for addressing the query effectively.
            
            Return JSON structure with the following fields:
            - concise_summary: Main points of the query
            - required_skills: Skills needed with brief explanation
            - relevant_tools: Useful tools with brief justification
            - additional_considerations: Any other important factors
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
            // Handle IO errors
            return new QueryAnalysis(
                    "Analysis of: " + request.getQuery(),
                    "Error processing response: " + e.getMessage(),
                    "No relevant tools could be determined due to error",
                    "Consider checking system connectivity or LLM service status"
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
     * Determines the next step to take based on the current context and
     * previous actions. Uses a real LLM to generate a thoughtful next step in
     * the problem-solving process.
     *
     * @param request The next step request containing current context and
     * available tools
     * @return A next step plan with actions and reasoning
     */
    @Tool(description = "Determine the optimal next step to address the given query based on the provided analysis, available tools, and previous steps taken")
    public NextStepPlan nextStep(NextStepRequest request) {
        // Define the system prompt template
        String systemPromptTemplate = """
            Task: Determine the optimal next step to address the given query based on the provided analysis, available tools, and previous steps taken.
            
            Context: {context}
            
            Available Tools: {availableTools}
            Previous Actions: {previousActions}
            
            Instructions:
            1. Analyze the context thoroughly, including the query, its analysis, available tools, and previous steps taken.
            2. Determine the most appropriate next step by considering:
               - Key objectives from the context
               - Capabilities of available tools
               - Logical progression of problem-solving
               - Outcomes from previous steps
            3. Select the tool best suited for the next step.
            4. Formulate a specific, achievable action for the selected tool that maximizes progress towards addressing the query.
            
            Return JSON structure with the following fields:
            - next_action: Specific action to take next
            - tools_to_use: Tool(s) recommended for this action
            - expected_outcome: What this step should accomplish
            - reasoning: Why this is the appropriate next step
            - alternatives: Other possible approaches (if any)
            """;

        // Create the system message from the template
        Message systemMessage = new SystemPromptTemplate(systemPromptTemplate)
                .createMessage(Map.of(
                        "context", request.getContext(),
                        "availableTools", request.getAvailableTools(),
                        "previousActions", request.getPreviousActions() != null ? request.getPreviousActions() : "No previous actions"
                ));

        // Create the user message
        Message userMessage = new UserMessage("Generate the next step based on the current context.");

        // Create the prompt with both messages
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            // Call the LLM using the prompt
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            // Parse the JSON response into our object
            return objectMapper.readValue(response, NextStepPlan.class);
        } catch (IOException e) {
            // Handle IO errors
            return new NextStepPlan(
                    "Error determining next step",
                    "No tools could be determined due to error",
                    "Error occurred: " + e.getMessage(),
                    "Error processing response: " + e.getMessage(),
                    "Consider checking system connectivity or LLM service status"
            );
        } catch (RuntimeException e) {
            // Handle LLM API errors
            return new NextStepPlan(
                    "Error determining next step",
                    "No tools could be determined due to error",
                    "Error occurred: " + e.getMessage(),
                    "Error from LLM: " + e.getMessage(),
                    "Try again with a clearer context or check API connectivity"
            );
        }
    }

    /**
     * Verifies if the current context is sufficient to proceed with the task.
     * Uses a real LLM to identify any missing information or clarifications
     * needed.
     *
     * @param request The context verification request containing current
     * context
     * @return A context verification result indicating completeness and any
     * missing information
     */
    @Tool(description = "Thoroughly evaluate the completeness and accuracy of the memory for fulfilling the given query, considering the potential need for additional step.")
    public ContextVerification verifyContext(ContextVerificationRequest request) {
        // Define the system prompt template
        String systemPromptTemplate = """
            Task: Thoroughly evaluate the completeness and accuracy of the context for fulfilling the given objective.
            
            Context to verify: {context}
            
            Task objective: {objective}
            Required information: {requiredInfo}
            
            Detailed Instructions:
            1. Carefully analyze the provided context against the task objective.
            2. Determine if all necessary information is present to proceed with the task.
            3. Identify any missing or unclear information that would be needed.
            4. Suggest specific clarifications or additional details that would improve the context.
            5. Assess the overall quality and completeness of the context.
            
            Critical Evaluation (address each point explicitly):
            a) Completeness: Does the context fully address all aspects of the objective?
               - Identify any parts of the objective that remain unaddressed.
            b) Inconsistencies: Are there any contradictions or conflicts in the information provided?
               - If yes, explain the inconsistencies and suggest how they might be resolved.
            c) Ambiguities: Are there any unclear or ambiguous elements that need clarification?
               - Point out specific ambiguities and suggest how they could be clarified.
            
            Return JSON structure with the following fields:
            - is_complete: true/false
            - missing_information: Details on what information is missing, if any
            - ambiguities: Any unclear or ambiguous aspects that need clarification
            - recommendations: Suggestions for improving the context
            - confidence_level: High/Medium/Low assessment of context quality
            """;

        // Create the system message from the template
        Message systemMessage = new SystemPromptTemplate(systemPromptTemplate)
                .createMessage(Map.of(
                        "context", request.getContext(),
                        "objective", request.getObjective(),
                        "requiredInfo", request.getRequiredInfo() != null ? request.getRequiredInfo() : "No specific requirements provided"
                ));

        // Create the user message
        Message userMessage = new UserMessage("Thoroughly evaluate the completeness and accuracy of the context for fulfilling the given objective.");

        // Create the prompt with both messages
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            // Call the LLM using the prompt
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            // Parse the JSON response into our object
            return objectMapper.readValue(response, ContextVerification.class);
        } catch (IOException e) {
            // Handle IO errors
            return new ContextVerification(
                    false,
                    "Error processing response: " + e.getMessage(),
                    "Unable to verify ambiguities due to error",
                    "Consider checking system connectivity or LLM service status",
                    "Low"
            );
        } catch (RuntimeException e) {
            // Handle LLM API errors
            return new ContextVerification(
                    false,
                    "Error from LLM: " + e.getMessage(),
                    "Unable to verify ambiguities due to error",
                    "Try again with a different context format or check API connectivity",
                    "Low"
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

    /**
     * Request object for the next step thinking tool.
     */
    public static class NextStepRequest {

        @JsonProperty("context")
        private String context;

        @JsonProperty("available_tools")
        private List<String> availableTools;

        @JsonProperty("previous_actions")
        private String previousActions;

        // Getters and setters
        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public List<String> getAvailableTools() {
            return availableTools;
        }

        public void setAvailableTools(List<String> availableTools) {
            this.availableTools = availableTools;
        }

        public String getPreviousActions() {
            return previousActions;
        }

        public void setPreviousActions(String previousActions) {
            this.previousActions = previousActions;
        }
    }

    /**
     * Response object for the next step thinking tool.
     */
    public static class NextStepPlan {

        @JsonProperty("next_action")
        private String nextAction;

        @JsonProperty("tools_to_use")
        private String toolsToUse;

        @JsonProperty("expected_outcome")
        private String expectedOutcome;

        @JsonProperty("reasoning")
        private String reasoning;

        @JsonProperty("alternatives")
        private String alternatives;

        // Default constructor for Jackson
        public NextStepPlan() {
        }

        public NextStepPlan(String nextAction, String toolsToUse, String expectedOutcome, String reasoning, String alternatives) {
            this.nextAction = nextAction;
            this.toolsToUse = toolsToUse;
            this.expectedOutcome = expectedOutcome;
            this.reasoning = reasoning;
            this.alternatives = alternatives;
        }

        // Getters and setters
        public String getNextAction() {
            return nextAction;
        }

        public void setNextAction(String nextAction) {
            this.nextAction = nextAction;
        }

        public String getToolsToUse() {
            return toolsToUse;
        }

        public void setToolsToUse(String toolsToUse) {
            this.toolsToUse = toolsToUse;
        }

        public String getExpectedOutcome() {
            return expectedOutcome;
        }

        public void setExpectedOutcome(String expectedOutcome) {
            this.expectedOutcome = expectedOutcome;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public String getAlternatives() {
            return alternatives;
        }

        public void setAlternatives(String alternatives) {
            this.alternatives = alternatives;
        }
    }

    /**
     * Request object for the context verification tool.
     */
    public static class ContextVerificationRequest {

        @JsonProperty("context")
        private String context;

        @JsonProperty("objective")
        private String objective;

        @JsonProperty("required_info")
        private String requiredInfo;

        // Getters and setters
        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public String getRequiredInfo() {
            return requiredInfo;
        }

        public void setRequiredInfo(String requiredInfo) {
            this.requiredInfo = requiredInfo;
        }
    }

    /**
     * Response object for the context verification tool.
     */
    public static class ContextVerification {

        @JsonProperty("is_complete")
        private boolean isComplete;

        @JsonProperty("missing_information")
        private String missingInformation;

        @JsonProperty("ambiguities")
        private String ambiguities;

        @JsonProperty("recommendations")
        private String recommendations;

        @JsonProperty("confidence_level")
        private String confidenceLevel;

        // Default constructor for Jackson
        public ContextVerification() {
        }

        public ContextVerification(boolean isComplete, String missingInformation, String ambiguities, String recommendations, String confidenceLevel) {
            this.isComplete = isComplete;
            this.missingInformation = missingInformation;
            this.ambiguities = ambiguities;
            this.recommendations = recommendations;
            this.confidenceLevel = confidenceLevel;
        }

        // Getters and setters
        public boolean isComplete() {
            return isComplete;
        }

        public void setComplete(boolean complete) {
            isComplete = complete;
        }

        public String getMissingInformation() {
            return missingInformation;
        }

        public void setMissingInformation(String missingInformation) {
            this.missingInformation = missingInformation;
        }

        public String getAmbiguities() {
            return ambiguities;
        }

        public void setAmbiguities(String ambiguities) {
            this.ambiguities = ambiguities;
        }

        public String getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(String recommendations) {
            this.recommendations = recommendations;
        }

        public String getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(String confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }
    }
}
