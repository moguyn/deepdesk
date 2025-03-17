package com.moguyn.deepdesk.config;

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
 * Tests for the advisor configuration in ApplicationConfig. These tests
 * complement the more focused AdvisorService tests.
 */
public class AdvisorConfigTest {

    @Test
    public void testAllAdvisorsEnabled() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Mock dependencies
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        AdvisorService advisorService = mock(AdvisorService.class);
        ChatClient chatClient = mock(ChatClient.class);
        Advisor mockAdvisor = mock(Advisor.class);

        // Setup CoreSettings with all advisors enabled
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""),
                        1000,
                        1000
                ),
                new CoreSettings.Advisors(true));

        // Setup mocked chain
        when(chatClientBuilder.defaultSystem(any(String.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(Class.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any(Advisor[].class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Mock the advisor service to return an advisor
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(List.of(mockAdvisor));

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
    public void testSomeAdvisorsDisabled() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Mock dependencies
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);
        AdvisorService advisorService = mock(AdvisorService.class);
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

        // Setup mocked chain
        when(chatClientBuilder.defaultSystem(any(String.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(ToolCallbackProvider.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(Class.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Mock the advisor service to return an empty list
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
