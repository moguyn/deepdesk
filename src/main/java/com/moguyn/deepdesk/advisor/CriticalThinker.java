package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
public class CriticalThinker extends AbstractAdvisor {

    public static final String QUERY_PLAN_KEY = "queryPlan";
    private final ChatClient chatClient;
    private static final String THINKING_PROMPT = """
        You are a critical thinker. 

        Query:
        {query}
        LLM provided the following answer:
        {answer}

        LLM provided the following plan:
        {plan}


        Instructions:
        1. You are given the answer that the LLM provided as well as the query and the plan.
        2. You need to think about the query, the plan, and the answer and determine if the plan and the answer are correct and optimal.
        3. If the plan is not correct or optimal, you need suggest a new plan.
        4. If the answer is not correct or optimal, you need to provide a new answer.

        Your response should include:
        1. A boolean value indicating if the plan and the answer are correct and optimal.
        2. A new plan if the current plan is not correct or optimal.
        3. A new answer if the current answer is not correct or optimal.
                    
        Present your response in the following JSON format with values in Chinese:
        ```
        {format}
        ```
        """;

    public CriticalThinker(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper, int order) {
        super(objectMapper, order);
        this.chatClient = chatClientBuilder
                .defaultSystem("You act as a critical thinking expert.")
                .defaultTools(toolCallbackProvider)
                .build();
    }

    @SuppressWarnings("null")
    @Override
    protected AdvisedResponse after(AdvisedResponse advisedResponse) {
        log.debug("LLM response to be thought about: {}", advisedResponse);
        if (advisedResponse == null || advisedResponse.response() == null || advisedResponse.response().getResult() == null || advisedResponse.response().getResult().getOutput() == null) {
            return advisedResponse;
        }
        final String answer = advisedResponse.response().getResult().getOutput().getText();
        var plan = (QueryPlan) advisedResponse
                .adviseContext()
                .getOrDefault(QUERY_PLAN_KEY, new QueryPlan("", "", "", "", ""));
        var outputConverter = createOutputConverter(CriticalThinkingResponse.class);

        CriticalThinkingResponse response = this.chatClient.prompt().user(u -> u.text(THINKING_PROMPT)
                .param("query", plan.summary())
                .param("answer", answer)
                .param("plan", plan.toString())
                .param("format", outputConverter.getFormat()))
                .call()
                .entity(outputConverter);
        log.debug("Critical thinking response: {}", response);
        if (response == null || response.isOptimal()) {
            return advisedResponse;
        }
        var originalResonse = advisedResponse.response();
        ChatResponse newResponse;
        if (originalResonse != null) {
            var originalResults = originalResonse.getResults();
            var newResults = new ArrayList<Generation>(originalResults);
            newResults.add(new Generation(new AssistantMessage(response.answer())));
            newResponse = new ChatResponse(newResults, originalResonse.getMetadata());
        } else {
            var newResults = List.of(new Generation(new AssistantMessage(response.answer())));
            newResponse = new ChatResponse(newResults);
        }

        return AdvisedResponse.from(advisedResponse)
                .response(newResponse)
                .build();
    }

    @Override
    public @NonNull
    String getName() {
        return "critical-thinker";
    }

    private record CriticalThinkingResponse(String plan, String answer, boolean isOptimal) {

    }

}
