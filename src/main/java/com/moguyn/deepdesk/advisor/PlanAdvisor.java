package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.chat.client.ChatClient;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * ThinkService that uses a real LLM for query analysis and planning. This
 * service provides tools for analyzing queries, determining next steps, and
 * verifying context completeness.
 */
@Slf4j
public class PlanAdvisor extends AbstractAdvisor {

    public static final String QUERY_PLAN_KEY = "queryPlan";
    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ChatMemory chatMemory;
    private final int CONTEXT_SIZE = 8;

    public PlanAdvisor(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper, int order) {
        super(objectMapper, order);
        this.toolCallbackProvider = toolCallbackProvider;
        this.chatMemory = chatMemory;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider)
                .build();
    }

    @Override
    protected AdvisedRequest before(AdvisedRequest advisedRequest) {
        log.debug("query received: {}", advisedRequest.userText());
        AdvisedRequest.Builder builder = AdvisedRequest
                .from(advisedRequest);
        List<Message> messages = new ArrayList<>(advisedRequest.messages());
        Map<String, Object> ac = new HashMap<>(advisedRequest.adviseContext());
        QueryPlan queryAnalysis = plan(new AnalysisRequest(
                advisedRequest.userText(),
                Stream.of(this.toolCallbackProvider.getToolCallbacks())
                        .map(this::describeTool)
                        .collect(Collectors.toList()),
                getContext(ac)
        ));
        messages = appendAdvice(messages, queryAnalysis, MessageType.SYSTEM);
        builder.userText(queryAnalysis.summary());
        ac.put(QUERY_PLAN_KEY, queryAnalysis);

        var newRequest = builder
                .messages(messages)
                .adviseContext(ac)
                .build();
        return newRequest;
    }

    private String getContext(Map<String, Object> ac) {
        String conversationId = ac
                .getOrDefault(CHAT_MEMORY_CONVERSATION_ID_KEY, "default")
                .toString();
        return this.chatMemory.get(conversationId, CONTEXT_SIZE).stream()
                .map(Message::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Analyzes a query to determine the skills and tools needed to address it
     * effectively. Uses a real LLM for analysis through Spring AI's ChatClient.
     *
     * @param request The analysis request containing the query and available
     * tools
     * @return An analysis of the query with required skills and relevant tools
     */
    protected QueryPlan plan(AnalysisRequest request) {
        log.debug("calling plan: {}", request);
        StructuredOutputConverter<QueryPlan> outputConverter = createOutputConverter(QueryPlan.class);
        // Define the system prompt template
        String promptTemplate = """
            You are a senior project manager.
            Task: Analyze the given query with accompanying inputs and determine the steps (max {maxSteps}) to address it effectively.

            Context: {context}

            Available tools with metadata: {availableTools}

            Query: {query}

            Instructions:
            1. Carefully read and understand the query and any accompanying inputs and context.
            2. Identify the main objectives and rephrase them in a way that is clear and as less ambiguous as possible.
            3. Divide the main objectives into actionable steps, given the available tools and their metadata.
            4. Provide a brief explanation for each step, describing how it would contribute to answering the query.
            5. Identify areas of ambiguity and uncertainty in the query and provide a plan to address them.

            Your response should include:
            1. A clear summary of the query's main points and objectives.
            2. A list of comprehensive steps, with a brief reasoning for each, and how they connect to each other.
            3. A short list of clarification questions (max {maxClarificationQuestions}) that might need to be further addressed, leave blank if none.
            4. Any additional considerations that might be important for addressing the query effectively, leave blank if none.
                        
            Present your response in the following JSON format with values in Chinese:
            ```
            {format}
            ```
            """.trim();

        try {
            //  all the LLM using the prompt variable directly
            var response = chatClient.prompt()
                    .user(u -> u.text(promptTemplate)
                    .param("query", request.query())
                    .param("maxSteps", 5)
                    .param("maxClarificationQuestions", 3)
                    .param("availableTools", request.availableTools())
                    .param("format", outputConverter.getFormat())
                    )
                    .call()
                    .entity(outputConverter);
            log.debug("query analysis response: {}", response);
            return response;
        } catch (Exception e) {
            // Handle LLM API errors
            return new QueryPlan(
                    "Analysis of: " + request.query(),
                    "Error from LLM: " + e.getMessage(),
                    "No relevant tools could be determined due to error",
                    "Try again with a different query or check API connectivity",
                    request.context()
            );
        }
    }

    /**
     * Request object for the think tool.
     */
    public record AnalysisRequest(String query, List<String> availableTools, String context) {

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
    public record QueryPlan(String summary, String actionableSteps, String clarificationQuestions, String additionalConsiderations, String context) {

    }

    @Override
    public @NonNull
    String getName() {
        return "plan-advisor";
    }

}
