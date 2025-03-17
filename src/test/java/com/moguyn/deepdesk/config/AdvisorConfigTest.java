package com.moguyn.deepdesk.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.moguyn.deepdesk.advisor.ChatMemoryAdvisor;
import com.moguyn.deepdesk.config.CoreSettings.LLM;
import com.moguyn.deepdesk.config.CoreSettings.Prompt;
import com.moguyn.deepdesk.config.CoreSettings.UI;

public class AdvisorConfigTest {

    @Test
    public void testAllAdvisorsEnabled() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Mock dependencies
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        ChatMemoryAdvisor chatMemoryAdvisor = mock(ChatMemoryAdvisor.class);
        ChatClient chatClient = mock(ChatClient.class);

        // Setup CoreSettings with all advisors enabled
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""),
                        1000,
                        1000
                ),
                new CoreSettings.Advisors(true));

        // Setup builder mock to capture advisors
        List<Advisor> capturedAdvisors = new ArrayList<>();
        when(chatClientBuilder.defaultSystem(org.mockito.ArgumentMatchers.anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(org.mockito.ArgumentMatchers.any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(org.mockito.ArgumentMatchers.any(Advisor[].class))).thenAnswer(invocation -> {
            Object[] advisorArray = invocation.getArguments();
            capturedAdvisors.addAll(Arrays.asList(advisorArray).stream().map(Advisor.class::cast).collect(Collectors.toList()));
            return chatClientBuilder;
        });
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Act
        ChatClient result = config.chatClient(
                chatClientBuilder,
                toolCallbackProvider,
                chatMemoryAdvisor,
                coreSettings,
                "system prompt"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, capturedAdvisors.size());
    }

    @Test
    public void testSomeAdvisorsDisabled() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Mock dependencies
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        ChatMemoryAdvisor chatMemoryAdvisor = mock(ChatMemoryAdvisor.class);
        ChatClient chatClient = mock(ChatClient.class);

        // Setup CoreSettings with some advisors disabled
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""),
                        1000,
                        1000
                ),
                new CoreSettings.Advisors(false));

        // Setup builder mock to capture advisors
        List<Advisor> capturedAdvisors = new ArrayList<>();
        when(chatClientBuilder.defaultSystem(org.mockito.ArgumentMatchers.anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(org.mockito.ArgumentMatchers.any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(org.mockito.ArgumentMatchers.any(Advisor[].class))).thenAnswer(invocation -> {
            Object[] advisorArray = invocation.getArguments();
            capturedAdvisors.addAll(Arrays.asList(advisorArray).stream().map(Advisor.class::cast).collect(Collectors.toList()));
            return chatClientBuilder;
        });
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Act
        ChatClient result = config.chatClient(
                chatClientBuilder,
                toolCallbackProvider,
                chatMemoryAdvisor,
                coreSettings,
                "system prompt"
        );

        // Assert
        assertNotNull(result);
        assertEquals(0, capturedAdvisors.size());
    }
}
