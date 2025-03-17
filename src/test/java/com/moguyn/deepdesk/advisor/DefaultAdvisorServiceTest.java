package com.moguyn.deepdesk.advisor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import com.moguyn.deepdesk.config.CoreSettings;
import com.moguyn.deepdesk.config.CoreSettings.Advisors;
import com.moguyn.deepdesk.config.CoreSettings.LLM;
import com.moguyn.deepdesk.config.CoreSettings.Prompt;
import com.moguyn.deepdesk.config.CoreSettings.UI;

class DefaultAdvisorServiceTest {

    private DefaultAdvisorService advisorService;
    private ChatMemoryAdvisor mockChatMemoryAdvisor;

    @BeforeEach
    public void setUp() {
        mockChatMemoryAdvisor = mock(ChatMemoryAdvisor.class);
        advisorService = new DefaultAdvisorService(mockChatMemoryAdvisor);
    }

    @Test
    void shouldEnableChatMemoryAdvisorWhenConfigured() {
        // Arrange
        CoreSettings settings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                new Advisors(true)
        );

        // Act
        List<Advisor> enabledAdvisors = advisorService.getEnabledAdvisors(settings);

        // Assert
        assertEquals(1, enabledAdvisors.size());
        assertTrue(enabledAdvisors.contains(mockChatMemoryAdvisor),
                "ChatMemoryAdvisor should be enabled when configured");
    }

    @Test
    void shouldNotEnableChatMemoryAdvisorWhenDisabled() {
        // Arrange
        CoreSettings settings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                new Advisors(false)
        );

        // Act
        List<Advisor> enabledAdvisors = advisorService.getEnabledAdvisors(settings);

        // Assert
        assertTrue(enabledAdvisors.isEmpty(),
                "No advisors should be enabled when configured to be disabled");
    }

    @Test
    void shouldEnableAllAdvisorsWhenNoAdvisorSettings() {
        // Arrange
        CoreSettings settings = new CoreSettings(
                List.of(),
                new UI(""),
                new LLM(new Prompt(""), 1000, 1000),
                null
        );

        // Act
        List<Advisor> enabledAdvisors = advisorService.getEnabledAdvisors(settings);

        // Assert
        assertEquals(1, enabledAdvisors.size());
        assertTrue(enabledAdvisors.contains(mockChatMemoryAdvisor),
                "ChatMemoryAdvisor should be enabled when no advisor settings provided");
    }
}
