package com.moguyn.deepdesk.chat;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
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
        var messages = request
                .getMessages()
                .stream()
                .map(m -> new UserMessage(m.getContent()))
                .toArray(Message[]::new);
        var aiResponse = chatClient.prompt(new Prompt(messages))
                .advisors(ad -> ad.param(CHAT_MEMORY_CONVERSATION_ID_KEY, request.getConversationId()))
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
