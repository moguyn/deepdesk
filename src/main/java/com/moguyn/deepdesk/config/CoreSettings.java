package com.moguyn.deepdesk.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for core settings in application.yaml
 */
@ConfigurationProperties(prefix = "core")
public class CoreSettings {

    @NestedConfigurationProperty
    private List<CapabilitySettings> capabilities;

    @NestedConfigurationProperty
    private UI ui;

    @NestedConfigurationProperty
    private LLM llm;

    @NestedConfigurationProperty
    private Advisors advisors;

    public LLM getLlm() {
        return llm;
    }

    public void setLlm(LLM llm) {
        this.llm = llm;
    }

    public List<CapabilitySettings> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<CapabilitySettings> capabilities) {
        this.capabilities = capabilities;
    }

    public UI getUi() {
        return ui;
    }

    public void setUi(UI ui) {
        this.ui = ui;
    }

    public Advisors getAdvisors() {
        return advisors;
    }

    public void setAdvisors(Advisors advisors) {
        this.advisors = advisors;
    }

    public static class CapabilitySettings {

        private String type;
        private Map<String, Object> config;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

    /**
     * Represents IO settings configuration
     */
    public static class UI {

        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class LLM {

        private Prompt prompt;
        private int maxTokens;
        private int historyWindowSize;

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getHistoryWindowSize() {
            return historyWindowSize;
        }

        public void setHistoryWindowSize(int historyWindowSize) {
            this.historyWindowSize = historyWindowSize;
        }

        public Prompt getPrompt() {
            return prompt;
        }

        public void setPrompt(Prompt prompt) {
            this.prompt = prompt;
        }
    }

    public static class Prompt {

        private String system;

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }
    }

    /**
     * Configuration for enabling/disabling advisors
     */
    public static class Advisors {

        private boolean chatMemoryAdvisorEnabled = true;

        public boolean isChatMemoryAdvisorEnabled() {
            return chatMemoryAdvisorEnabled;
        }

        public void setChatMemoryAdvisorEnabled(boolean chatMemoryAdvisorEnabled) {
            this.chatMemoryAdvisorEnabled = chatMemoryAdvisorEnabled;
        }
    }
}
