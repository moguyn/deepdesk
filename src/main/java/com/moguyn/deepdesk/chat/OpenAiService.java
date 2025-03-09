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

import com.moguyn.deepdesk.openai.model.ChatCompletionChunk;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;
import com.moguyn.deepdesk.openai.model.Choice;
import com.moguyn.deepdesk.openai.model.OpenAiUsage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final ChatClient chatClient;
    private final TokenCountEstimator tokenCountEstimator;

    @Value("${core.llm.prompt.system:你是企业级AI助手, 请说中文, 请使用markdown格式输出}")
    private String systemPrompt;

    public ChatCompletionResponse processChat(ChatCompletionRequest request) {
        if (request.isStream()) {
            throw new IllegalArgumentException("Stream mode should be used with streamChat method");
        }

        ChatClient.ChatClientRequestSpec promptResponse = prepareMessages(request);

        // Call the AI model
        var aiResponse = promptResponse.call().content();
        var reply = Optional.ofNullable(aiResponse).orElse("");

        ChatCompletionResponse response = buildResponse(request, reply);

        return response;
    }

    private ChatCompletionResponse buildResponse(ChatCompletionRequest request, String reply) {
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
        choice.setMessage(ChatMessage.builder().role("assistant").content(reply).build());
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

    private ChatClient.ChatClientRequestSpec prepareMessages(ChatCompletionRequest request) {
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

        return promptResponse;
    }

    /**
     * Process a streaming chat request and send chunks of the response as
     * Server-Sent Events
     */
    public Flux<ChatCompletionChunk> streamChat(ChatCompletionRequest request) {
        var promptResponse = prepareMessages(request);
        return promptResponse
                .stream()
                .chatResponse()
                .map(tr -> {
                    // Create a builder with safe defaults
                    var chunkBuilder = ChatCompletionChunk.builder()
                            .id(tr.getMetadata().getId() != null ? tr.getMetadata().getId() : "deepdesk-" + java.util.UUID.randomUUID())
                            .object("chat.completion.chunk")
                            .created(System.currentTimeMillis() / 1000)
                            .model(tr.getMetadata().getModel() != null ? tr.getMetadata().getModel() : request.getModel());

                    // Safely handle usage data if available
                    if (tr.getMetadata().getUsage() != null) {
                        chunkBuilder.usage(OpenAiUsage.builder()
                                .promptTokens(tr.getMetadata().getUsage().getPromptTokens())
                                .completionTokens(tr.getMetadata().getUsage().getCompletionTokens())
                                .totalTokens(tr.getMetadata().getUsage().getTotalTokens())
                                .build());
                    }

                    // Set system fingerprint if needed
                    if (tr.getMetadata() != null) {
                        chunkBuilder.systemFingerprint(tr.getMetadata().toString());
                    }

                    // Safely map results to choices
                    List<ChatCompletionChunk.ChunkChoice> choices = tr.getResults()
                            .stream()
                            .map(g -> {
                                var choiceBuilder = ChatCompletionChunk.ChunkChoice.builder();

                                // Safely extract index or default to 0
                                choiceBuilder.index(g.getMetadata().getOrDefault("index", 0));

                                // Safely create delta message
                                String content = g.getMetadata().getOrDefault("delta", "");
                                choiceBuilder.delta(ChatMessage.builder()
                                        .role("assistant")
                                        .content(content)
                                        .build());

                                // Set finish reason if available
                                choiceBuilder.finishReason(g.getMetadata().getOrDefault("finish_reason", ""));

                                return choiceBuilder.build();
                            })
                            .collect(Collectors.toList());

                    return chunkBuilder.choices(choices).build();
                });
    }

    private int estimateTokenCount(String text) {
        return tokenCountEstimator.estimate(text);
    }

    public List<String> getModels() {
        return List.of("deepdesk");
    }
}
