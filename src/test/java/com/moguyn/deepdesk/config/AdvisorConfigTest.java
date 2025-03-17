package com.moguyn.deepdesk.config;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
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
@ExtendWith(MockitoExtension.class)
public class AdvisorConfigTest {

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Mock
    private AdvisorService advisorService;

    @Test
    public void testAllAdvisorsEnabled() {
        // Mock builder
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class, Mockito.RETURNS_SELF);
        ChatClient chatClient = mock(ChatClient.class);

        // Setup mock to return the builder and finally return the client
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Setup CoreSettings with all advisors enabled
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""),
                        1000,
                        1000
                ),
                new CoreSettings.Advisors(true));

        // Mock the advisor
        Advisor mockAdvisor = mock(Advisor.class);
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(List.of(mockAdvisor));

        // Tool callback provider
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);

        // Act
        ChatClient result = applicationConfig.chatClient(
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
        // Mock builder
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class, Mockito.RETURNS_SELF);
        ChatClient chatClient = mock(ChatClient.class);

        // Setup mock to return the builder and finally return the client
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Setup CoreSettings with some advisors disabled
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""),
                        1000,
                        1000
                ),
                new CoreSettings.Advisors(false));

        // Mock the advisor service to return an empty list
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(List.of());

        // Tool callback provider
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);

        // Act
        ChatClient result = applicationConfig.chatClient(
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
