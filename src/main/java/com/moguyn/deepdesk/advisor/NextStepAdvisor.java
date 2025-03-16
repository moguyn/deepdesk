package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moguyn.deepdesk.advisor.PlanAdvisor.QueryPlan;

import lombok.extern.slf4j.Slf4j;

/**
 * ThinkService that uses a real LLM for query analysis and planning. This
 * service provides tools for analyzing queries, determining next steps, and
 * verifying context completeness.
 */
@Slf4j
public class NextStepAdvisor extends AbstractAdvisor {

    public static final String NEXT_STEP_KEY = "nextStep";
    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public NextStepAdvisor(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper, int order) {
        super(objectMapper, order);
        this.toolCallbackProvider = toolCallbackProvider;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolCallbackProvider)
                .build();
    }

    @Override
    protected AdvisedRequest advise(AdvisedRequest advisedRequest) {
        log.debug("query received: {}", advisedRequest.userText());
        AdvisedRequest.Builder builder = AdvisedRequest
                .from(advisedRequest);
        List<Message> messages = new ArrayList<>(advisedRequest.messages());
        Map<String, Object> ac = new HashMap<>(advisedRequest.adviseContext());
        QueryPlan queryPlan = (QueryPlan) ac.get(PlanAdvisor.QUERY_PLAN_KEY);
        if (queryPlan == null) {
            queryPlan = new QueryPlan(
                    advisedRequest.systemText(),
                    "",
                    "",
                    ""
            );
        }

        NextStep nextStep = nextStep(queryPlan);
        messages = appendAdvice(messages, nextStep, MessageType.SYSTEM);
        builder.userText(nextStep.summary());
        ac.put(NEXT_STEP_KEY, nextStep);

        var newRequest = builder
                .messages(messages)
                .adviseContext(ac)
                .build();
        return newRequest;
    }

    /**
     * Analyzes a query to determine the skills and tools needed to address it
     * effectively. Uses a real LLM for analysis through Spring AI's ChatClient.
     *
     * @param request The analysis request containing the query and available
     * tools
     * @return An analysis of the query with required skills and relevant tools
     */
    protected NextStep nextStep(QueryPlan queryPlan) {
        log.debug("calling next step: {}", queryPlan);
        StructuredOutputConverter<NextStep> outputConverter = createOutputConverter(NextStep.class);
        // Define the system prompt template
        String promptTemplate = """
            Task: Analyze the given query and context provided by your project manager and determine the next most important step in order to answer the query.

            Available tools with metadata: 
            ```
            {availableTools}
            ```

            Query: {query}

            Context: 
             - actionable steps: {actionableSteps}
             - clarification questions: {clarificationQuestions}
             - additional considerations: {additionalConsiderations}

            Instructions:
            1. Identify the most important next step to take, among the actionable steps and clarification questions to ask.
            2. Provide a brief explanation for the next step, describing how it would contribute to answering the query.
            3. If it's an action to take, provide the tool to use and the parameters to pass to the tool.
            4. If it's a clarification question, provide the question to ask, make it clear and concise.
            5. If the list of allowed directories is empty, tell the user that the action is not possible and ask for clarification.

            Your response should include:
            1. A clear and comprehensive summary of the next step to take, it's either an action to take or a clarification question to ask. Leave blank for fields that are not applicable.
            2. The tool to use, if applicable.
            3. The parameters to pass to the tool, if applicable.
            4. The question to ask, if applicable.
            5. The allowed directories
           
                        
            Present your response in the following JSON format with values in Chinese:
            ```
            {format}
            ```
            """.trim();

        try {
            //  all the LLM using the prompt variable directly
            String availableTools = Stream.of(toolCallbackProvider.getToolCallbacks())
                    .map(this::describeTool)
                    .collect(Collectors.joining("\n"));
            var response = chatClient.prompt()
                    .user(u -> u.text(promptTemplate)
                    .param("query", queryPlan.summary())
                    .param("availableTools", availableTools)
                    .param("actionableSteps", queryPlan.actionableSteps())
                    .param("clarificationQuestions", queryPlan.clarificationQuestions())
                    .param("additionalConsiderations", queryPlan.additionalConsiderations())
                    .param("format", outputConverter.getFormat())
                    )
                    .call()
                    .entity(outputConverter);
            log.debug("next step response: {}", response);
            return response;
        } catch (Exception e) {
            // Handle LLM API errors
            return new NextStep(queryPlan.summary(), "", "", "", "");
        }
    }

    /**
     * Response object for the think tool.
     */
    public record NextStep(String summary, String tool, String parameters, String question, String allowedDirectories) {

    }

    @Override
    public @NonNull
    String getName() {
        return "next-step-advisor";
    }

}
