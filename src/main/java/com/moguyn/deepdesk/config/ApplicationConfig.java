package com.moguyn.deepdesk.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moguyn.deepdesk.advisor.ChatMemoryAdvisor;
import com.moguyn.deepdesk.advisor.ContextLimiter;
import com.moguyn.deepdesk.advisor.CriticalThinker;
import com.moguyn.deepdesk.advisor.MaxTokenSizeContentLimiter;
import com.moguyn.deepdesk.advisor.NextStepAdvisor;
import com.moguyn.deepdesk.advisor.PlanAdvisor;
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
    public ContextLimiter<Message> contextLimiter(TokenCountEstimator tokenCountEstimator, @Value("${core.llm.max-tokens}") int maxTokens) {
        return new MaxTokenSizeContentLimiter<>(tokenCountEstimator, maxTokens);
    }

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public ChatMemoryAdvisor tokenLimitedChatMemoryAdvisor(
            ChatMemory chatMemory,
            ContextLimiter<Message> contextLimiter,
            @Value("${core.llm.history-window-size}") int historyWindowSize) {
        return new ChatMemoryAdvisor(
                chatMemory,
                DEFAULT_CONVERSATION_ID,
                historyWindowSize,
                contextLimiter,
                1000);
    }

    @Bean
    public PlanAdvisor planAdvisor(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper, ChatMemory chatMemory) {
        return new PlanAdvisor(chatClientBuilder, chatMemory, toolCallbackProvider, objectMapper, 10);
    }

    @Bean
    public NextStepAdvisor nextStepAdvisor(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper) {
        return new NextStepAdvisor(chatClientBuilder, toolCallbackProvider, objectMapper, 20);
    }

    @Bean
    public CriticalThinker criticalThinker(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper) {
        return new CriticalThinker(chatClientBuilder, toolCallbackProvider, objectMapper, 30);
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
            ToolCallbackProvider toolCallbackProvider,
            PlanAdvisor planAdvisor,
            NextStepAdvisor nextStepAdvisor,
            CriticalThinker criticalThinker,
            ChatMemoryAdvisor tokenLimitedChatMemoryAdvisor,
            @Value("${core.llm.prompt.system}") String systemPrompt) {

        var chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolCallbackProvider)
                .defaultAdvisors(planAdvisor, nextStepAdvisor, criticalThinker, tokenLimitedChatMemoryAdvisor)
                .build();

        return chatClient;
    }
}
