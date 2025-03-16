package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
class ChatMemoryAdvisorTest {

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ContextLimiter<Message> contextLimiter;

    @Mock
    private ChatModel chatModel;

    private ChatMemoryAdvisor chatMemoryAdvisor;
    private static final String DEFAULT_CONVERSATION_ID = "test-conversation";
    private static final int CHAT_HISTORY_WINDOW_SIZE = 10;

    @BeforeEach
    void setUp() {
        chatMemoryAdvisor = new ChatMemoryAdvisor(
                chatMemory,
                DEFAULT_CONVERSATION_ID,
                CHAT_HISTORY_WINDOW_SIZE,
                contextLimiter,
                100
        );
    }

    @Test
    void testBefore() {
        // Arrange
        Map<String, Object> adviseContext = new HashMap<>();
        adviseContext.put("key", "value");

        AdvisedRequest request = AdvisedRequest.builder()
                .userText("test query")
                .adviseContext(adviseContext)
                .chatModel(chatModel)
                .build();

        List<Message> memoryMessages = new ArrayList<>();
        memoryMessages.add(new UserMessage("Previous message 1"));
        memoryMessages.add(new UserMessage("Previous message 2"));

        when(chatMemory.get(anyString(), anyInt())).thenReturn(memoryMessages);

        List<Message> combinedMessages = new ArrayList<>();
        combinedMessages.addAll(request.messages());
        combinedMessages.addAll(memoryMessages);

        when(contextLimiter.truncate(any())).thenReturn(combinedMessages);

        // Act
        AdvisedRequest result = chatMemoryAdvisor.before(request);

        // Assert
        assertNotNull(result);
        assertEquals(combinedMessages.size(), result.messages().size());
        verify(chatMemory).add(anyString(), any(UserMessage.class));
    }

    @Test
    void testBuilder() {
        // Act
        ChatMemoryAdvisor.Builder builder = ChatMemoryAdvisor.builder(chatMemory);

        // Assert
        assertNotNull(builder);
    }
}
