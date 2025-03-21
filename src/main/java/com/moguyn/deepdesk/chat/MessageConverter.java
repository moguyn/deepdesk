package com.moguyn.deepdesk.chat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatMessage;

/**
 * Handles conversion between OpenAI message format and Spring AI message format
 */
@Component
public class MessageConverter {

    @Value("${core.llm.prompt.system:你是企业级AI助手, 请说中文, 请使用markdown格式输出}")
    private String defaultSystemPrompt;

    /**
     * Converts OpenAI chat messages to Spring AI messages,
     * adding a default system prompt if none is present
     */
    public List<Message> toSpringMessages(List<ChatMessage> openAiMessages) {
        List<Message> messages = new ArrayList<>();

        // Check if a system message is already present
        boolean hasSystemMessage = openAiMessages.stream()
                .anyMatch(m -> "system".equals(m.role()));

        // Add default system message if needed
        if (!hasSystemMessage) {
            messages.add(new SystemMessage(defaultSystemPrompt));
        }

        // Convert and add messages from the request
        openAiMessages.forEach(m -> {
            switch (m.role()) {
                case "system" ->
                    messages.add(new SystemMessage(m.content()));
                case "user" ->
                    messages.add(new UserMessage(m.content()));
                case "assistant" ->
                    messages.add(new AssistantMessage(m.content()));
                default ->
                    throw new IllegalArgumentException("Unsupported role: " + m.role());
            }
        });

        return messages;
    }

    /**
     * Creates a Spring AI Prompt from a ChatCompletionRequest
     */
    public Prompt createPrompt(ChatCompletionRequest request) {
        List<Message> messages = toSpringMessages(request.getMessages());
        return new Prompt(messages);
    }
}