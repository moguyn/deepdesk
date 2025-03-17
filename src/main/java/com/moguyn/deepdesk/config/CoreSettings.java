package com.moguyn.deepdesk.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for core settings in application.yaml
 */
@ConfigurationProperties(prefix = "core")
public record CoreSettings(
        @NestedConfigurationProperty
        List<CapabilitySettings> capabilities,
        @NestedConfigurationProperty
        UI ui,
        @NestedConfigurationProperty
        LLM llm,
        @NestedConfigurationProperty
        Advisors advisors) {

    /**
     * Represents capability settings configuration
     */
    public record CapabilitySettings(
            String type,
            Map<String, Object> config) {

    }

    /**
     * Represents UI settings configuration
     */
    public record UI(
            String type) {

    }

    /**
     * Represents LLM settings configuration
     */
    public record LLM(
            Prompt prompt,
            int maxTokens,
            int historyWindowSize) {

    }

    /**
     * Represents prompt settings configuration
     */
    public record Prompt(
            String system) {

    }

    /**
     * Configuration for enabling/disabling advisors
     */
    public record Advisors(
            boolean chatMemoryAdvisorEnabled) {

        /**
         * Provides compatibility with isXxx pattern
         */
        public boolean isChatMemoryAdvisorEnabled() {
            return chatMemoryAdvisorEnabled;
        }
    }
}
