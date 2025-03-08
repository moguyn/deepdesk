package com.moguyn.deepdesk.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moguyn.deepdesk.chat.ChatService;
import com.moguyn.deepdesk.model.ChatAnswer;
import com.moguyn.deepdesk.model.ChatRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "web")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatAnswer chat(@RequestBody ChatRequest request) {
        return chatService.processChat(request);
    }
}
