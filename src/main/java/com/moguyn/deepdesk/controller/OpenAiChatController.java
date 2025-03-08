package com.moguyn.deepdesk.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.moguyn.deepdesk.chat.OpenAiService;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/openai/chat")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "web")
public class OpenAiChatController {

    private final OpenAiService openAiService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatCompletionRequest request) {
        // Check if streaming is requested
        if (Boolean.TRUE.equals(request.getStream())) {
            // Return a streaming response
            SseEmitter emitter = openAiService.streamChat(request);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        } else {
            // Return a regular response
            ChatCompletionResponse response = openAiService.processChat(request);
            return ResponseEntity.ok(response);
        }
    }
}
