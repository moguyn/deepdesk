package com.moguyn.deepdesk.chat;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import com.moguyn.deepdesk.openai.model.ChatCompletionRequest;
import com.moguyn.deepdesk.openai.model.ChatMessage;

public class MessageConverterTest {

    private MessageConverter messageConverter;
    private final String testSystemPrompt = "You are a test assistant";

    @BeforeEach
    public void setUp() {
        messageConverter = new MessageConverter();
        ReflectionTestUtils.setField(messageConverter, "defaultSystemPrompt", testSystemPrompt);
    }

    @Test
    void shouldConvertOpenAiMessagesToSpringMessages() {
        // Given
        List<ChatMessage> openAiMessages = List.of(
                ChatMessage.builder().role("user").content("Hello").build(),
                ChatMessage.builder().role("assistant").content("Hi there").build()
        );

        // When
        List<Message> result = messageConverter.toSpringMessages(openAiMessages);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.get(0) instanceof SystemMessage);
        SystemMessage sysMsg = (SystemMessage) result.get(0);
        assertEquals(testSystemPrompt, sysMsg.getText());
        assertTrue(result.get(1) instanceof UserMessage);
        UserMessage userMsg = (UserMessage) result.get(1);
        assertEquals("Hello", userMsg.getText());
        assertTrue(result.get(2) instanceof AssistantMessage);
        AssistantMessage assistantMsg = (AssistantMessage) result.get(2);
        assertEquals("Hi there", assistantMsg.getText());
    }

    @Test
    void shouldNotAddSystemMessageIfAlreadyPresent() {
        // Given
        List<ChatMessage> openAiMessages = List.of(
                ChatMessage.builder().role("system").content("Custom system prompt").build(),
                ChatMessage.builder().role("user").content("Hello").build()
        );

        // When
        List<Message> result = messageConverter.toSpringMessages(openAiMessages);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof SystemMessage);
        SystemMessage sysMsg = (SystemMessage) result.get(0);
        assertEquals("Custom system prompt", sysMsg.getText());
        assertTrue(result.get(1) instanceof UserMessage);
        UserMessage userMsg = (UserMessage) result.get(1);
        assertEquals("Hello", userMsg.getText());
    }

    @Test
    void shouldThrowExceptionForUnsupportedRole() {
        // Given
        List<ChatMessage> openAiMessages = List.of(
                ChatMessage.builder().role("unsupported").content("Bad role").build()
        );

        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            messageConverter.toSpringMessages(openAiMessages);
        });
        assertEquals("Unsupported role: unsupported", exception.getMessage());
    }

    @Test
    void shouldCreatePromptFromRequest() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessages(List.of(
                ChatMessage.builder().role("user").content("Hello").build()
        ));

        // When
        Prompt result = messageConverter.createPrompt(request);

        // Then
        List<Message> messages = result.getInstructions();
        assertEquals(2, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertTrue(messages.get(1) instanceof UserMessage);
        UserMessage userMsg = (UserMessage) messages.get(1);
        assertEquals("Hello", userMsg.getText());
    }
}
