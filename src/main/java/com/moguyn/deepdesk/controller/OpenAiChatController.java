package com.moguyn.deepdesk.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.moguyn.deepdesk.chat.OpenAiService;
import com.moguyn.deepdesk.openai.model.ChatCompletionChunk;
import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatCompletionResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/openai")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "web")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OpenAiChatController {

    private final OpenAiService openAiService;

    @PostMapping(path = "/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public ResponseEntity<?> chat(@RequestBody ChatCompletionRequest request) {
        if (!request.isStream()) {
            ChatCompletionResponse response = openAiService.processChat(request);
            return ResponseEntity.ok(response);
        } else {
            Flux<ChatCompletionChunk> streamResponse = openAiService.streamChat(request);
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(streamResponse);
        }
    }

    @GetMapping(path = "/models")
    public ResponseEntity<?> models() {
        return ResponseEntity.ok(openAiService.getModels());
    }

    @RequestMapping(path = "/models", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> modelsOptions() {
        return ResponseEntity
                .ok()
                .header("Allow", "GET, OPTIONS")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }
}
