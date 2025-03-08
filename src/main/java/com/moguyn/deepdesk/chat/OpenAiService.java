package com.moguyn.deepdesk.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;
import com.moguyn.deepdesk.openai.model.Choice;
import com.moguyn.deepdesk.openai.model.OpenAiUsage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final ChatClient chatClient;
    private final TokenCountEstimator tokenCountEstimator;

    @Value("${core.llm.prompt.system:你是企业级AI助手, 请说中文, 请使用markdown格式输出}")
    private String systemPrompt;

    public ChatCompletionResponse processChat(ChatCompletionRequest request) {
        // Return streaming response if stream is true
        if (Boolean.TRUE.equals(request.getStream())) {
            throw new IllegalArgumentException("Stream mode should be used with streamChat method");
        }

        // Convert OpenAI messages to Spring AI messages
        List<Message> messages = new ArrayList<>();

        // Add system message if not present in the request
        boolean hasSystemMessage = request.getMessages().stream()
                .anyMatch(m -> "system".equals(m.getRole()));

        if (!hasSystemMessage) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // Add messages from the request
        request.getMessages().forEach(m -> {
            switch (m.getRole()) {
                case "system" ->
                    messages.add(new SystemMessage(m.getContent()));
                case "user" ->
                    messages.add(new UserMessage(m.getContent()));
                case "assistant" ->
                    messages.add(new AssistantMessage(m.getContent()));
                default ->
                    throw new IllegalArgumentException("Unsupported role: " + m.getRole());
            }
        });

        // Create prompt and set up conversation ID if available
        var promptResponse = chatClient.prompt(new Prompt(messages));

        if (request.getUser() != null) {
            promptResponse = promptResponse.advisors(ad
                    -> ad.param(CHAT_MEMORY_CONVERSATION_ID_KEY, request.getUser()));
        }

        // Call the AI model
        var aiResponse = promptResponse.call().content();
        var reply = Optional.ofNullable(aiResponse).orElse("");

        // Estimate token usage (this is approximate)
        int promptTokens = estimateTokenCount(request.getMessages().stream()
                .map(ChatMessage::getContent)
                .collect(Collectors.joining()));
        int completionTokens = estimateTokenCount(reply);

        // Build OpenAI-style response
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("deepdesk-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(request.getModel());

        // Create a choice with the AI response
        Choice choice = new Choice();
        choice.setIndex(0);
        choice.setMessage(new ChatMessage("assistant", reply));
        choice.setFinishReason("stop");

        response.setChoices(List.of(choice));

        // Add usage information
        OpenAiUsage usage = new OpenAiUsage();
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(promptTokens + completionTokens);
        response.setUsage(usage);

        return response;
    }

    /**
     * Process a streaming chat request and send chunks of the response as
     * Server-Sent Events
     */
    public SseEmitter streamChat(ChatCompletionRequest request) {
        throw new IllegalArgumentException("Stream mode is not supported yet!");
    }

    private int estimateTokenCount(String text) {
        return tokenCountEstimator.estimate(text);
    }
}
