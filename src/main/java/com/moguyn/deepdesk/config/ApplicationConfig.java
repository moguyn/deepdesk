package com.moguyn.deepdesk.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
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

import com.moguyn.deepdesk.advisor.ChatMemoryAdvisor;
import com.moguyn.deepdesk.advisor.ContextLimiter;
import com.moguyn.deepdesk.advisor.MaxTokenSizeContentLimiter;
import com.moguyn.deepdesk.chat.ChatRunner;
import com.moguyn.deepdesk.chat.CommandlineChatRunner;
import com.moguyn.deepdesk.dependency.SoftwareDependencyValidator;

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
    public ContextLimiter<Message> contextLimiter(TokenCountEstimator tokenCountEstimator, CoreSettings coreSettings) {
        return new MaxTokenSizeContentLimiter<>(tokenCountEstimator, coreSettings.llm().maxTokens());
    }

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public ChatMemoryAdvisor tokenLimitedChatMemoryAdvisor(
            ChatMemory chatMemory,
            ContextLimiter<Message> contextLimiter,
            CoreSettings coreSettings) {
        return new ChatMemoryAdvisor(
                chatMemory,
                DEFAULT_CONVERSATION_ID,
                coreSettings.llm().historyWindowSize(),
                contextLimiter,
                1000);
    }

    @Bean
    public ChatRunner chatRunner(ChatClient chatClient) {
        return new CommandlineChatRunner(chatClient);
    }

    @PostConstruct
    public void dependencyValidation() {
        var validator = new SoftwareDependencyValidator("npx", "uvx");
        validator.verifyDependencies();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
            ToolCallbackProvider toolCallbackProvider,
            ChatMemoryAdvisor tokenLimitedChatMemoryAdvisor,
            CoreSettings coreSettings,
            @Value("${core.llm.prompt.system}") String systemPrompt) {

        var builder = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolCallbackProvider);

        // Dynamically add advisors based on configuration
        List<Advisor> enabledAdvisors = new ArrayList<>();

        CoreSettings.Advisors advisorSettings = coreSettings.advisors();
        if (advisorSettings != null) {

            if (advisorSettings.isChatMemoryAdvisorEnabled()) {
                log.info("Enabling Chat Memory Advisor");
                enabledAdvisors.add(tokenLimitedChatMemoryAdvisor);
            }
        } else {
            log.warn("No advisor configuration found, enabling all advisors by default");
            enabledAdvisors.add(tokenLimitedChatMemoryAdvisor);
        }

        // Apply the enabled advisors
        if (!enabledAdvisors.isEmpty()) {
            builder.defaultAdvisors(enabledAdvisors.toArray(Advisor[]::new));
        } else {
            log.warn("No advisors enabled, chat client will operate without advisors");
        }

        return builder.build();
    }
}
