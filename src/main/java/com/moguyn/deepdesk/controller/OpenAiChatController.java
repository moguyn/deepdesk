package com.moguyn.deepdesk.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.moguyn.deepdesk.openai.model.ChatMessage;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/openai")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "web")
public class OpenAiChatController {

    private final OpenAiService openAiService;
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatController.class);

    @PostMapping(path = "/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chat(@RequestBody ChatCompletionRequest request) {
        if (!request.isStream()) {
            ChatCompletionResponse response = openAiService.processChat(request);
            return ResponseEntity.ok(response);
        } else {
            return openAiService.streamChat(request)
            .onErrorResume(e -> {
                // Log the error
                log.error("Error in chat stream: {}", e.getMessage(), e);

                // Return an error chunk
                ChatCompletionChunk errorChunk = ChatCompletionChunk.builder()
                        .id("error-" + java.util.UUID.randomUUID())
                        .object("chat.completion.chunk")
                        .created(System.currentTimeMillis() / 1000)
                        .model(request.getModel())
                        .choices(List.of(
                                ChatCompletionChunk.ChunkChoice.builder()
                                        .index(0)
                                        .delta(ChatMessage.builder()
                                                .role("assistant")
                                                .content("An error occurred: " + e.getMessage())
                                                .build())
                                        .finishReason("error")
                                        .build()
                        ))
                        .build();
                return Flux.just(errorChunk);
            });
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
