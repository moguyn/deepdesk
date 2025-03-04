package com.moguyn.deepdesk.service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moguyn.deepdesk.model.ChatAnswer;
import com.moguyn.deepdesk.model.ChatRequest;
import com.moguyn.deepdesk.model.ContentItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    @Value("${core.llm.prompt.system}")
    private String systemPrompt;

    public ChatAnswer processChat(ChatRequest request) {
        // Build the prompt with the ChatClient's fluent API
        var promptBuilder = chatClient.prompt();

        // Add system prompt
        promptBuilder.system(systemPrompt);

        // Add user messages - assuming we're processing the last user message
        request.getMessages().forEach(message -> {
            if ("user".equals(message.getRole())) {
                promptBuilder.user(message.getContent());
            }
        });

        var aiResponse = promptBuilder.call();

        var reply = Optional.ofNullable(aiResponse.content()).orElse("");

        return ChatAnswer.builder()
                .content(Collections.singletonList(new ContentItem(reply, "text")))
                .id("resp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .model(request.getModel())
                .role("assistant")
                .type("message")
                .build();
    }
}
