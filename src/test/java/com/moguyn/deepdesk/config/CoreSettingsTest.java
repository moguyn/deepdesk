package com.moguyn.deepdesk.config;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CoreSettingsTest {

    @Test
    void testCoreSettingsCreation() {
        // Arrange
        CoreSettings.CapabilitySettings capability = new CoreSettings.CapabilitySettings(
                "test-type", 
                Map.of("key", "value")
        );
        
        CoreSettings.UI ui = new CoreSettings.UI("cli");
        CoreSettings.Prompt prompt = new CoreSettings.Prompt("system-prompt");
        CoreSettings.LLM llm = new CoreSettings.LLM(prompt, 2000, 10);
        CoreSettings.Advisors advisors = new CoreSettings.Advisors(true);

        // Act
        CoreSettings settings = new CoreSettings(List.of(capability), ui, llm, advisors);

        // Assert
        assertEquals(1, settings.capabilities().size());
        assertEquals(capability, settings.capabilities().get(0));
        assertEquals(ui, settings.ui());
        assertEquals(llm, settings.llm());
        assertEquals(advisors, settings.advisors());
    }

    @Test
    void testCapabilitySettingsCreation() {
        // Arrange
        String type = "test-type";
        Map<String, Object> config = Map.of("key", "value", "num", 42);

        // Act
        CoreSettings.CapabilitySettings capability = new CoreSettings.CapabilitySettings(type, config);

        // Assert
        assertEquals(type, capability.type());
        assertEquals(config, capability.config());
    }

    @Test
    void testUISettingsCreation() {
        // Arrange
        String type = "cli";

        // Act
        CoreSettings.UI ui = new CoreSettings.UI(type);

        // Assert
        assertEquals(type, ui.type());
    }

    @Test
    void testLLMSettingsCreation() {
        // Arrange
        CoreSettings.Prompt prompt = new CoreSettings.Prompt("system-prompt");
        int maxTokens = 2000;
        int historyWindowSize = 10;

        // Act
        CoreSettings.LLM llm = new CoreSettings.LLM(prompt, maxTokens, historyWindowSize);

        // Assert
        assertEquals(prompt, llm.prompt());
        assertEquals(maxTokens, llm.maxTokens());
        assertEquals(historyWindowSize, llm.historyWindowSize());
    }

    @Test
    void testPromptSettingsCreation() {
        // Arrange
        String system = "system-prompt";

        // Act
        CoreSettings.Prompt prompt = new CoreSettings.Prompt(system);

        // Assert
        assertEquals(system, prompt.system());
    }

    @Test
    void testAdvisorsSettings() {
        // Arrange & Act
        CoreSettings.Advisors advisorsEnabled = new CoreSettings.Advisors(true);
        CoreSettings.Advisors advisorsDisabled = new CoreSettings.Advisors(false);

        // Assert
        assertTrue(advisorsEnabled.isChatMemoryAdvisorEnabled());
        assertTrue(advisorsEnabled.chatMemoryAdvisorEnabled());
        
        assertFalse(advisorsDisabled.isChatMemoryAdvisorEnabled());
        assertFalse(advisorsDisabled.chatMemoryAdvisorEnabled());
    }
}