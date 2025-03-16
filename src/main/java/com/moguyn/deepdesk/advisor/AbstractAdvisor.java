package com.moguyn.deepdesk.advisor;

import java.util.List;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.model.function.FunctionCallback;
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
public abstract class AbstractAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final ObjectMapper objectMapper;
    private final int order;
    public static final String SYSTEM_PROMPT = """
        - Make sure you only access the allowed directories. Don't guess or make up directories.
        - Your response should be in JSON format.
        - The data structure for the JSON should match this Java class: java.util.HashMap
        - Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
        """;

    public AbstractAdvisor(ObjectMapper objectMapper, int order) {
        this.order = order;
        this.objectMapper = objectMapper;
    }

    @Override
    public @NonNull
    AdvisedResponse aroundCall(@NonNull AdvisedRequest advisedRequest, @NonNull CallAroundAdvisorChain chain) {
        var newRequest = before(advisedRequest);

        AdvisedResponse advisedResponse = chain.nextAroundCall(newRequest);

        return this.after(advisedResponse);
    }

    protected AdvisedRequest before(AdvisedRequest advisedRequest) {
        return advisedRequest;
    }

    protected AdvisedResponse after(AdvisedResponse advisedResponse) {
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    @SuppressWarnings("deprecation")
    protected String describeTool(FunctionCallback f) {
        return f.getName() + "(description: " + f.getDescription() + ", input schema: " + f.getInputTypeSchema() + ")";
    }

    protected List<Message> appendAdvice(List<Message> messages, Object advice, MessageType messageType) {
        try {
            switch (messageType) {
                case USER ->
                    messages.add(new UserMessage(objectMapper.writeValueAsString(advice)));
                case ASSISTANT ->
                    messages.add(new AssistantMessage(objectMapper.writeValueAsString(advice)));
                case SYSTEM ->
                    messages.add(new SystemMessage(objectMapper.writeValueAsString(advice)));
                default ->
                    messages.add(new UserMessage(objectMapper.writeValueAsString(advice)));
            }
        } catch (JsonProcessingException e) {
            log.error("Error writing advice to JSON", e);
        }
        return messages;
    }

    protected void observeAfter(AdvisedResponse advisedResponse) {
    }

    protected <T> StructuredOutputConverter<T> createOutputConverter(Class<T> type) {
        return new BeanOutputConverter<>(type);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public @NonNull
    Flux<AdvisedResponse> aroundStream(@NonNull AdvisedRequest advisedRequest, @NonNull StreamAroundAdvisorChain chain) {
        AdvisedRequest advise = before(advisedRequest);
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advise).map(this::after);
        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}
