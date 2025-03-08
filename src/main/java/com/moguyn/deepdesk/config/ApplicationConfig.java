package com.moguyn.deepdesk.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moguyn.deepdesk.advisor.ExcessiveContentTruncator;
import com.moguyn.deepdesk.advisor.MaxTokenSizeContenTruncator;
import com.moguyn.deepdesk.advisor.TokenLimitedChatMemoryAdvisor;
import com.moguyn.deepdesk.capability.CapabililtyFactory;
import com.moguyn.deepdesk.capability.DependencyValidator;
import com.moguyn.deepdesk.capability.McpCapabilityFactory;
import com.moguyn.deepdesk.capability.McpDependencyValidator;
import com.moguyn.deepdesk.capability.McpManager;
import com.moguyn.deepdesk.capability.ToolManager;
import com.moguyn.deepdesk.chat.ChatRunner;
import com.moguyn.deepdesk.chat.CommandlineChatRunner;

import lombok.extern.slf4j.Slf4j;

/**
 * Application configuration to enable property binding
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {

    /**
     * Default conversation ID to use for chat memory.
     */
    private static final String DEFAULT_CONVERSATION_ID = "deepdesk-conversation";

    @Bean
    @ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "cli")
    public CommandLineRunner cli(
            ChatRunner chatManager,
            ConfigurableApplicationContext context) {
        return args -> {
            try (context) {
                chatManager.run(args);
            }
        };
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public ExcessiveContentTruncator<Message> excessiveContentTruncator(@Value("${core.llm.max-tokens}") int maxTokens) {
        return new MaxTokenSizeContenTruncator<>(new JTokkitTokenCountEstimator(), maxTokens);
    }

    @Bean
    public TokenLimitedChatMemoryAdvisor tokenLimitedChatMemoryAdvisor(
            ChatMemory chatMemory,
            ExcessiveContentTruncator<Message> excessiveContentTruncator,
            @Value("${core.llm.history-window-size}") int historyWindowSize) {
        return new TokenLimitedChatMemoryAdvisor(
                chatMemory,
                DEFAULT_CONVERSATION_ID,
                historyWindowSize,
                excessiveContentTruncator);
    }

    @Bean
    public ToolManager toolManager(CoreSettings core, DependencyValidator dependencyValidator) {
        return new McpManager(core.getCapabilities(), dependencyValidator);
    }

    @Bean
    public ChatRunner chatRunner(ChatClient chatClient, ToolManager toolManager) {
        return new CommandlineChatRunner(chatClient, toolManager);
    }

    @Bean
    public DependencyValidator dependencyValidator() {
        return new McpDependencyValidator("npx", "uvx");
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
            ToolManager toolManager,
            ChatMemory chatMemory,
            TokenLimitedChatMemoryAdvisor tokenLimitedChatMemoryAdvisor,
            @Value("${core.llm.prompt.system}") String systemPrompt) {
        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolManager.loadTools())
                .defaultAdvisors(tokenLimitedChatMemoryAdvisor)
                .build();
    }

    @Bean
    public CapabililtyFactory capabililtyFactory() {
        return new McpCapabilityFactory();
    }

}
