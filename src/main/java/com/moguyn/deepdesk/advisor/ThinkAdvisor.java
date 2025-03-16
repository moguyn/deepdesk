package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * ThinkService that uses a real LLM for query analysis and planning. This
 * service provides tools for analyzing queries, determining next steps, and
 * verifying context completeness.
 */
@Slf4j
public class ThinkAdvisor extends AbstractChatMemoryAdvisor<ChatMemory> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ToolCallbackProvider toolCallbackProvider;
    private static final String DEFAULT_CONVERSATION_ID = "think-advisor";
    private static final String ACTIONS_KEY = "actions";
    private static final String INITIALLY_ANALYZED_KEY = "initially_analyzed";
    private static final String FORMAT_PROMPT = """
        - Make sure you only access the allowed directories. Don't guess or make up directories.
        - Your response should be in JSON format.
        - The data structure for the JSON should match this Java class: java.util.HashMap
        - Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
        """;
    private static final int MAX_ITERATIONS = 4;

    public ThinkAdvisor(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper, int order) {
        super(chatMemory, DEFAULT_CONVERSATION_ID, 2, true, order);
        this.objectMapper = objectMapper;
        this.toolCallbackProvider = toolCallbackProvider;
        this.chatClient = chatClientBuilder
                .defaultSystem(FORMAT_PROMPT)
                .defaultTools(toolCallbackProvider)
                .build();
    }

    @Override
    public @NonNull
    AdvisedResponse aroundCall(@NonNull AdvisedRequest advisedRequest, @NonNull CallAroundAdvisorChain chain) {
        var newRequest = iterate(advisedRequest, 0);

        AdvisedResponse advisedResponse = chain.nextAroundCall(newRequest);

        this.observeAfter(advisedResponse);

        return advisedResponse;
    }

    private AdvisedRequest iterate(AdvisedRequest advisedRequest, int iteration) {
        if (iteration >= MAX_ITERATIONS) {
            return AdvisedRequest.from(advisedRequest)
                    .userText(advisedRequest.userText() + "\n\n" + "请保持说中文")
                    .build();
        }
        log.debug("query received: {}", advisedRequest.userText());
        ContextVerification contextVerification = verifyContext(new ContextVerificationRequest(
                advisedRequest.adviseContext().toString(),
                advisedRequest.userText(),
                null
        ));
        if (contextVerification.isComplete) {
            return advisedRequest;
        }

        AdvisedRequest.Builder builder = AdvisedRequest
                .from(advisedRequest);
        List<Message> messages = new ArrayList<>(advisedRequest.messages());
        Map<String, Object> ac = new HashMap<>(advisedRequest.adviseContext());
        if (!ac.containsKey(INITIALLY_ANALYZED_KEY)) {
            log.debug("initially analyzing query");
            @SuppressWarnings("deprecation")
            QueryAnalysis queryAnalysis = identifyTools(new AnalysisRequest(
                    advisedRequest.userText(),
                    Stream.of(this.toolCallbackProvider.getToolCallbacks())
                            .map(f -> f.getName())
                            .collect(Collectors.toList()),
                    Stream.of(this.toolCallbackProvider.getToolCallbacks())
                            .map(this::toolMeta)
                            .collect(Collectors.joining(", "))
            ));
            messages = appendAdvice(messages, queryAnalysis);
            builder.userText(queryAnalysis.conciseSummary());
            ac.put(INITIALLY_ANALYZED_KEY, queryAnalysis.toString());
        }

        ac.putAll(Map.of(
                "recommendations", Optional.ofNullable(contextVerification.recommendations()).orElse("No recommendations"),
                "missing_information", Optional.ofNullable(contextVerification.missingInformation()).orElse("No missing information"),
                "ambiguities", Optional.ofNullable(contextVerification.ambiguities()).orElse("No ambiguities"),
                "confidence_level", Optional.ofNullable(contextVerification.confidenceLevel()).orElse("Low")
        ));
        @SuppressWarnings("unchecked")
        List<String> lastActions = (List<String>) ac.getOrDefault(ACTIONS_KEY, new ArrayList<String>());

        NextStepPlan nextStepPlan = nextStep(new NextStepRequest(
                ac.toString(),
                Stream.of(this.toolCallbackProvider.getToolCallbacks())
                        .map(this::toolMeta)
                        .collect(Collectors.toList()),
                lastActions.toString()
        ));

        lastActions.add(nextStepPlan.nextAction());
        ac.put(ACTIONS_KEY, lastActions);
        messages = appendAdvice(messages, nextStepPlan);
        builder.systemText(nextStepPlan.toString());

        var newRequest = builder
                .messages(messages)
                .adviseContext(ac)
                .build();
        return iterate(newRequest, iteration + 1);
    }

    @SuppressWarnings("deprecation")
    private String toolMeta(FunctionCallback f) {
        return f.getName() + "(description: " + f.getDescription() + ", input schema: " + f.getInputTypeSchema() + ")";
    }

    private List<Message> appendAdvice(List<Message> messages, Object... advices) {
        for (Object advice : advices) {
            try {
                messages.add(new SystemMessage(objectMapper.writeValueAsString(advice)));
            } catch (JsonProcessingException e) {
                log.error("Error writing advice to JSON", e);
            }
        }
        return messages;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        log.debug("LLM response: {}", advisedResponse.response());
    }

    private <T> StructuredOutputConverter<T> getOutputConverter(Class<T> type) {
        return new BeanOutputConverter<>(type);
    }

    /**
     * Analyzes a query to determine the skills and tools needed to address it
     * effectively. Uses a real LLM for analysis through Spring AI's ChatClient.
     *
     * @param request The analysis request containing the query and available
     * tools
     * @return An analysis of the query with required skills and relevant tools
     */
    public QueryAnalysis identifyTools(AnalysisRequest request) {
        log.debug("calling identifyTools: {}", request);
        StructuredOutputConverter<QueryAnalysis> outputConverter = getOutputConverter(QueryAnalysis.class);
        // Define the system prompt template
        String promptTemplate = """
            Task: Analyze the given query with accompanying inputs and determine the skills and tools needed to address it effectively.
            
            Query: {query}
            
            Available tools: ```{availableTools}```
            Tools metadata: ```{toolboxMetadata}```
            
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
            4. Any additional considerations that might be important for addressing the query effectively, such as the allowed directories.
            
            Return JSON structure with the following format:
            ```
            {format}
            ```
            """;

        try {
            //  all the LLM using the prompt variable directly
            var response = chatClient.prompt()
                    .user(u -> u.text(promptTemplate)
                    .param("query", request.query())
                    .param("availableTools", request.availableTools())
                    .param("toolboxMetadata", request.toolboxMetadata() != null ? request.toolboxMetadata() : "No metadata available")
                    .param("format", outputConverter.getFormat())
                    )
                    .call()
                    .entity(outputConverter);
            log.debug("query analysis response: {}", response);
            return response;
        } catch (Exception e) {
            // Handle LLM API errors
            return new QueryAnalysis(
                    "Analysis of: " + request.query(),
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
    public NextStepPlan nextStep(NextStepRequest request) {
        log.debug("calling nextStep: {}", request);
        // Define the system prompt template
        String promptTemplate = """
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
            5. If the tool requires a directory, provide the allowed directories.
            Return JSON structure with the following format:
            ```
            {format}
            ```
            """;
        var outputConverter = getOutputConverter(NextStepPlan.class);
        try {
            //  all the LLM using the prompt
            var response = chatClient.prompt()
                    .user(u -> u.text(promptTemplate)
                    .param("context", request.context())
                    .param("availableTools", request.availableTools())
                    .param("previousActions", request.previousActions() != null ? request.previousActions() : "No previous actions")
                    .param("format", outputConverter.getFormat())
                    )
                    .call()
                    .entity(outputConverter);
            log.debug("next step plan response: {}", response);
            return response;
        } catch (Exception e) {
            // Handle LLM API errors
            return new NextStepPlan(
                    "Error determining next step",
                    "Next step could not be determined due to error",
                    "Error occurred: " + e.getMessage(),
                    "Error from LLM: " + e.getMessage(),
                    "Try again with a clearer context or check API connectivity",
                    "No allowed directories"
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
    public ContextVerification verifyContext(ContextVerificationRequest request) {
        log.debug("calling verifyContext: {}", request);
        // Define the system prompt template
        String promptTemplate = """
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
            
            Return JSON structure with the following format:
            ```
            {format}
            ```
            """;
        var outputConverter = getOutputConverter(ContextVerification.class);

        try {
            //  all the LLM using the prompt
            var response = chatClient.prompt()
                    .user(u -> u.text(promptTemplate)
                    .param("context", request.context() != null ? request.context() : "No context provided")
                    .param("objective", request.objective() != null ? request.objective() : "No objective provided")
                    .param("requiredInfo", request.requiredInfo() != null ? request.requiredInfo() : "No specific requirements provided")
                    .param("format", outputConverter.getFormat())
                    )
                    .call()
                    .entity(outputConverter);
            log.debug("context verification response: {}", response);
            return response;
        } catch (Exception e) {
            // Handle LLM API errors
            log.error("Error verifying context", e);
            return new ContextVerification(
                    false,
                    "Error from LLM: " + e.getMessage(),
                    "Unable to verify context due to error",
                    "Try again with a different context format or check API connectivity",
                    "Low"
            );
        }
    }

    /**
     * Request object for the think tool.
     */
    public record AnalysisRequest(String query, List<String> availableTools, String toolboxMetadata) {

        @Override
        public String toString() {
            return "AnalysisRequest{"
                    + "query='" + query + '\''
                    + '}';
        }
    }

    /**
     * Response object for the think tool.
     */
    public record QueryAnalysis(String conciseSummary, String requiredSkills, String relevantTools, String additionalConsiderations) {

    }

    /**
     * Request object for the next step thinking tool.
     */
    public record NextStepRequest(String context, List<String> availableTools, String previousActions) {

        @Override
        public String toString() {
            return "NextStepRequest{"
                    + "context='" + context + '\''
                    + ", previousActions='" + previousActions + '\''
                    + '}';
        }
    }

    /**
     * Response object for the next step thinking tool.
     */
    public record NextStepPlan(String nextAction, String toolsToUse, String expectedOutcome, String reasoning, String alternatives, String allowedDirectories) {

    }

    /**
     * Request object for the context verification tool.
     */
    public record ContextVerificationRequest(String context, String objective, String requiredInfo) {

        @Override
        public String toString() {
            return "ContextVerificationRequest{"
                    + "context='" + context + '\''
                    + ", objective='" + objective + '\''
                    + ", requiredInfo='" + requiredInfo + '\''
                    + '}';
        }
    }

    /**
     * Response object for the context verification tool.
     */
    public record ContextVerification(boolean isComplete, String missingInformation, String ambiguities, String recommendations, String confidenceLevel) {

    }

    @Override
    public @NonNull
    String getName() {
        return "think-advisor";
    }

    @Override
    public int getOrder() {
        return super.getOrder();
    }

    @Override
    public @NonNull
    Flux<AdvisedResponse> aroundStream(@NonNull AdvisedRequest advisedRequest, @NonNull StreamAroundAdvisorChain chain) {
        Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain,
                r -> iterate(r, 0));
        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}
