package com.moguyn.deepdesk.config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;

import com.moguyn.deepdesk.advisor.AdvisorService;
import com.moguyn.deepdesk.config.CoreSettings.LLM;
import com.moguyn.deepdesk.config.CoreSettings.Prompt;
import com.moguyn.deepdesk.config.CoreSettings.UI;

/**
 * Tests for the ChatClient creation in ApplicationConfig, using a mocked
 * AdvisorService.
 */
class ApplicationConfigChatClientTest {

    @Test
    void shouldCreateChatClientWithAdvisors() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Mock dependencies
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        AdvisorService advisorService = mock(AdvisorService.class);
        ChatClient chatClient = mock(ChatClient.class);

        // Configure mocks
        when(chatClientBuilder.defaultSystem(any(String.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(Class.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any(Advisor[].class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Setup core settings
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                new CoreSettings.Advisors(true)
        );

        // Mock advisor service to return one advisor
        List<Advisor> mockAdvisors = new ArrayList<>();
        mockAdvisors.add(mock(Advisor.class));
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(mockAdvisors);

        // Act
        ChatClient result = config.chatClient(
                chatClientBuilder,
                toolCallbackProvider,
                advisorService,
                coreSettings,
                "system prompt"
        );

        // Assert
        assertNotNull(result);
    }

    @Test
    void shouldCreateChatClientWithoutAdvisors() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Mock dependencies
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        AdvisorService advisorService = mock(AdvisorService.class);
        ChatClient chatClient = mock(ChatClient.class);

        // Configure mocks
        when(chatClientBuilder.defaultSystem(any(String.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(Class.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Setup core settings
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                new CoreSettings.Advisors(false)
        );

        // Mock advisor service to return empty list
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(List.of());

        // Act
        ChatClient result = config.chatClient(
                chatClientBuilder,
                toolCallbackProvider,
                advisorService,
                coreSettings,
                "system prompt"
        );

        // Assert
        assertNotNull(result);
    }
}
