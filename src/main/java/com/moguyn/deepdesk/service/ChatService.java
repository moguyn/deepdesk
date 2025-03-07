package com.moguyn.deepdesk.service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.moguyn.deepdesk.model.ChatAnswer;
import com.moguyn.deepdesk.model.ChatRequest;
import com.moguyn.deepdesk.model.ContentItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    public ChatAnswer processChat(ChatRequest request) {
        String lastPrompt = request.getMessages().get(request.getMessages().size() - 1).getContent();
        var aiResponse = chatClient.prompt(lastPrompt)
                .call()
                .content();

        var reply = Optional.ofNullable(aiResponse).orElse("");

        return ChatAnswer.builder()
                .content(Collections.singletonList(new ContentItem(reply, "text")))
                .id("resp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .model(request.getModel())
                .role("assistant")
                .type("message")
                .build();
    }
}
