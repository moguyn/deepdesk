package com.moguyn.deepdesk.chat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import com.moguyn.deepdesk.openai.model.ChatCompletionChunk;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final ChatClient chatClient;
    private final MessageConverter messageConverter;
    private final ResponseBuilder responseBuilder;

    /**
     * Process a non-streaming chat request and return a complete response
     */
    public ChatCompletionResponse processChat(ChatCompletionRequest request) {
        if (request.isStream()) {
            throw new IllegalArgumentException("Stream mode should be used with streamChat method");
        }

        // Prepare the prompt with messages
        Prompt prompt = messageConverter.createPrompt(request);
        
        // Configure chat client request spec
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt(prompt);
        
        // Set conversation ID if user is provided
        if (request.getUser() != null) {
            promptSpec = promptSpec.advisors(ad
                    -> ad.param(CHAT_MEMORY_CONVERSATION_ID_KEY, request.getUser()));
        }

        // Call the AI model
        var aiResponse = promptSpec.call().content();
        var reply = Optional.ofNullable(aiResponse).orElse("");

        // Build the response
        return responseBuilder.buildResponse(request, reply);
    }

    /**
     * Process a streaming chat request and send chunks of the response as Server-Sent Events
     */
    public Flux<ChatCompletionChunk> streamChat(ChatCompletionRequest request) {
        // Prepare the prompt with messages
        Prompt prompt = messageConverter.createPrompt(request);
        
        // Configure chat client request spec
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt(prompt);
        
        // Set conversation ID if user is provided
        if (request.getUser() != null) {
            promptSpec = promptSpec.advisors(ad
                    -> ad.param(CHAT_MEMORY_CONVERSATION_ID_KEY, request.getUser()));
        }
        
        var chunkId = "deepdesk-" + java.util.UUID.randomUUID();
        var systemFingerprint = "fp_" + java.util.UUID.randomUUID().toString();
        var created = System.currentTimeMillis() / 1000;
        
        return promptSpec
                .stream()
                .chatResponse()
                .map(tr -> {
                    // Create a builder with safe defaults
                    var chunkBuilder = ChatCompletionChunk.builder()
                            .id(tr.getMetadata().getId() != null ? tr.getMetadata().getId() : chunkId)
                            .object("chat.completion.chunk")
                            .created(created)
                            .model(tr.getMetadata().getModel() != null ? tr.getMetadata().getModel() : request.getModel());

                    // Set system fingerprint if needed
                    if (tr.getMetadata() != null) {
                        chunkBuilder.systemFingerprint(systemFingerprint);
                    }

                    // Safely map results to choices
                    List<ChatCompletionChunk.ChunkChoice> choices = tr.getResults()
                            .stream()
                            .map(g -> {
                                var choiceBuilder = ChatCompletionChunk.ChunkChoice.builder();

                                // Safely extract index or default to 0
                                choiceBuilder.index(g.getMetadata().getOrDefault("index", 0));

                                // Safely create delta message
                                var content = g.getOutput().getText();
                                choiceBuilder.delta(ChatMessage.builder()
                                        .role(content != null && content.isEmpty() ? "assistant" : null)
                                        .content(content)
                                        .build());

                                // Set finish reason if available
                                choiceBuilder
                                        .logprobs(g.getMetadata().get("logprobs"))
                                        .finishReason(content == null ? "stop" : null);

                                return choiceBuilder.build();
                            })
                            .collect(Collectors.toList());

                    return chunkBuilder.choices(choices).build();
                });
    }

    /**
     * Return a list of available model options
     */
    public List<String> getModels() {
        return List.of("deepdesk");
    }
}