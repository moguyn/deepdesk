package com.moguyn.deepdesk.config;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moguyn.deepdesk.mcp.McpManager;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Application configuration to enable property binding
 */
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {

    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
    private final List<McpSyncClient> mcpClients = new ArrayList<>();

    @Bean
    @ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "cli")
    public CommandLineRunner cli(
            ChatClient.Builder chatClientBuilder,
            SyncMcpToolCallback[] capabilities,
            ConfigurableApplicationContext context) {
        return _ -> {
            try (context) {
                PrintStream console = System.out;
                var chatClient = chatClientBuilder
                        .defaultSystem("你是企业级AI助手, 请说中文")
                        .defaultTools(capabilities)
                        .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                        .build();
                console.println("\n我是你的AI助手(退出请输入bye或者exit)\n");
                try (Scanner scanner = new Scanner(System.in)) {
                    while (true) {
                        console.print("\n用户: ");
                        String prompt = scanner.nextLine();
                        if ("exit".equalsIgnoreCase(prompt) || "bye".equalsIgnoreCase(prompt)) {
                            break;
                        }
                        console.println("\nAI: "
                                + chatClient.prompt(prompt)
                                        .call()
                                        .content());
                    }
                }
            }
        };
    }

    @PreDestroy
    @SuppressWarnings("unused")
    private void shutdown() {
        // Close all MCP clients when the context is closed
        mcpClients.forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.error("Error closing MCP client", e);
            }
        });
    }

    @PostConstruct
    @SuppressWarnings("unused")
    private void verifyNpxAvailability() {
        try {
            Process process = new ProcessBuilder("which", "npx")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("npx command is not available. Please install Node.js and npm to use this feature.");
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to verify npx availability: " + e.getMessage(), e);
        }
    }

    @Bean
    public SyncMcpToolCallback[] capabilities(CoreSettings core, McpManager mcpManager) {
        var tools = new ArrayList<SyncMcpToolCallback>();

        for (CoreSettings.Capabilities capability : core.getCapabilities()) {
            tools.addAll(getTools(capability, mcpManager));
        }

        return tools.toArray(SyncMcpToolCallback[]::new);
    }

    @Bean
    public McpManager mcpFactory() {
        return new McpManager();
    }

    private Collection<SyncMcpToolCallback> getTools(CoreSettings.Capabilities capability, McpManager mcpManager) {
        switch (capability.getType()) {
            case "files" -> {
                @SuppressWarnings("unchecked")
                var paths = (LinkedHashMap<String, String>) capability.getConfig().get("paths");
                var mcpClient = mcpManager.createFilesystemMCP(paths.values());
                mcpClients.add(mcpClient); // Track the client for cleanup
                return mcpClient.listTools(null)
                        .tools()
                        .stream()
                        .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
                        .toList();
            }
            case "search" -> {
                var mcpClient = mcpManager.createSearchMCP();
                mcpClients.add(mcpClient);
                return mcpClient.listTools(null)
                        .tools()
                        .stream()
                        .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
                        .toList();
            }
            default -> {
                log.warn("Unknown capability type: {}", capability.getType());
                return List.<SyncMcpToolCallback>of();
            }
        }
    }
}
