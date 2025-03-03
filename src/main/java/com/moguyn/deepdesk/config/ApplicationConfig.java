package com.moguyn.deepdesk.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moguyn.deepdesk.mcp.ChatManager;
import com.moguyn.deepdesk.mcp.CommandlineChat;
import com.moguyn.deepdesk.mcp.DependencyValidator;
import com.moguyn.deepdesk.mcp.McpDependencyValidator;
import com.moguyn.deepdesk.mcp.McpManager;
import com.moguyn.deepdesk.mcp.ToolManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Application configuration to enable property binding
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "cli")
    public CommandLineRunner cli(
            ChatManager chatManager,
            ConfigurableApplicationContext context) {
        return args -> {
            try (context) {
                chatManager.start();
            }
        };
    }

    @Bean
    public ToolManager toolManager(CoreSettings core, DependencyValidator dependencyValidator) {
        return new McpManager(core, dependencyValidator);
    }

    @Bean
    public ChatManager chatManager(ChatClient.Builder chatClientBuilder, ToolManager toolManager) {
        return new CommandlineChat(chatClientBuilder, toolManager);
    }

    @Bean
    public DependencyValidator dependencyValidator() {
        return new McpDependencyValidator();
    }

}
