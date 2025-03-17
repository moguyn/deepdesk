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
 * Tests for the ChatClient creation in ApplicationConfig, using a mocked
 * AdvisorService.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationConfigChatClientTest {

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Mock
    private AdvisorService advisorService;

    @Test
    void shouldCreateChatClientWithAdvisors() {
        // Mock builder
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class, Mockito.RETURNS_SELF);
        ChatClient chatClient = mock(ChatClient.class);

        // Setup mock to return the builder and finally return the client
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Core settings
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                new CoreSettings.Advisors(true)
        );

        // Mock advisor
        Advisor mockAdvisor = mock(Advisor.class);
        List<Advisor> mockAdvisors = List.of(mockAdvisor);
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(mockAdvisors);

        // Tool callback provider
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);

        // Act - use ReflectionTestUtils to invoke the method directly with our completely mocked objects
        ChatClient result = applicationConfig.chatClient(
                chatClientBuilder, // This won't be null and will work with any method call
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
        // Mock builder
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class, Mockito.RETURNS_SELF);
        ChatClient chatClient = mock(ChatClient.class);

        // Setup mock to return the builder and finally return the client
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Core settings
        CoreSettings coreSettings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                new CoreSettings.Advisors(false)
        );

        // Mock empty advisors list
        when(advisorService.getEnabledAdvisors(any(CoreSettings.class))).thenReturn(List.of());

        // Tool callback provider
        ToolCallbackProvider toolCallbackProvider = mock(ToolCallbackProvider.class);

        // Act - use ReflectionTestUtils to invoke the method directly with our completely mocked objects
        ChatClient result = applicationConfig.chatClient(
                chatClientBuilder, // This won't be null and will work with any method call
                toolCallbackProvider,
                advisorService,
                coreSettings,
                "system prompt"
        );

        // Assert
        assertNotNull(result);
    }
}
