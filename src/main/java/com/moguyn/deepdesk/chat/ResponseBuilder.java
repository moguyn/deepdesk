package com.moguyn.deepdesk.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.stereotype.Component;

import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;
import com.moguyn.deepdesk.openai.model.ChatMessage;
import com.moguyn.deepdesk.openai.model.Choice;
import com.moguyn.deepdesk.openai.model.OpenAiUsage;

import lombok.RequiredArgsConstructor;

/**
 * Responsible for building response objects in OpenAI-compatible format
 */
@Component
@RequiredArgsConstructor
public class ResponseBuilder {

    private final TokenCountEstimator tokenCountEstimator;

    /**
     * Builds a complete ChatCompletionResponse from a request and reply content
     */
    public ChatCompletionResponse buildResponse(ChatCompletionRequest request, String reply) {
        // Estimate token usage
        int promptTokens = estimatePromptTokens(request);
        int completionTokens = estimateTokenCount(reply);
        int totalTokens = promptTokens + completionTokens;

        // Create a choice with the AI response
        Choice choice = new Choice(
                0,
                ChatMessage.of("assistant", reply),
                "stop",
                null);

        // Create OpenAI usage information
        OpenAiUsage usage = new OpenAiUsage(promptTokens, completionTokens, totalTokens);

        // Build and return the complete response
        return new ChatCompletionResponse(
                generateResponseId(),
                "chat.completion",
                System.currentTimeMillis() / 1000,
                request.getModel(),
                null, // system fingerprint
                List.of(choice),
                usage
        );
    }

    /**
     * Estimates tokens for the entire prompt from a request
     */
    private int estimatePromptTokens(ChatCompletionRequest request) {
        return estimateTokenCount(request.getMessages().stream()
                .map(ChatMessage::content)
                .collect(Collectors.joining()));
    }

    /**
     * Estimates token count for a text string
     */
    private int estimateTokenCount(String text) {
        return tokenCountEstimator.estimate(text);
    }

    /**
     * Generates a unique response ID
     */
    private String generateResponseId() {
        return "deepdesk-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
