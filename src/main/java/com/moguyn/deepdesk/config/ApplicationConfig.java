package com.moguyn.deepdesk.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moguyn.deepdesk.advisor.ChatMemoryAdvisor;
import com.moguyn.deepdesk.advisor.ExcessiveContentTruncator;
import com.moguyn.deepdesk.advisor.MaxTokenSizeContenTruncator;
import com.moguyn.deepdesk.chat.ChatRunner;
import com.moguyn.deepdesk.chat.CommandlineChatRunner;
import com.moguyn.deepdesk.dependency.McpDependencyValidator;

import jakarta.annotation.PostConstruct;
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
    public ChatMemoryAdvisor tokenLimitedChatMemoryAdvisor(
            ChatMemory chatMemory,
            ExcessiveContentTruncator<Message> excessiveContentTruncator,
            @Value("${core.llm.history-window-size}") int historyWindowSize) {
        return new ChatMemoryAdvisor(
                chatMemory,
                DEFAULT_CONVERSATION_ID,
                historyWindowSize,
                excessiveContentTruncator);
    }

    @Bean
    public ChatRunner chatRunner(ChatClient chatClient) {
        return new CommandlineChatRunner(chatClient);
    }

    @PostConstruct
    public void dependencyValidation() {
        var validator = new McpDependencyValidator("npx", "uvx");
        validator.verifyDependencies();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
            SyncMcpToolCallbackProvider toolCallbackProvider,
            ChatMemory chatMemory,
            ChatMemoryAdvisor tokenLimitedChatMemoryAdvisor,
            @Value("${core.llm.prompt.system}") String systemPrompt) {
        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .defaultAdvisors(tokenLimitedChatMemoryAdvisor)
                .build();
    }
}
