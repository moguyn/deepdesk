package com.moguyn.deepdesk.config;

import java.io.Console;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.spring.McpFunctionCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moguyn.deepdesk.mcp.FilesystemMCP;

import jakarta.annotation.PreDestroy;

/**
 * Application configuration to enable property binding
 */
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {

    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
    private final List<McpSyncClient> mcpClients = new ArrayList<>();
    private final FilesystemMCP filesystemMCP = new FilesystemMCP();

    // Additional beans can be defined here if needed
    @Bean
    public CommandLineRunner cli(ChatClient.Builder chatClientBuilder,
            List<McpFunctionCallback> capabilities,
            ConfigurableApplicationContext context) {

        return _ -> {
            try (context) {
                Console console = System.console();
                var chatClient = chatClientBuilder
                        .defaultFunctions(capabilities.toArray(McpFunctionCallback[]::new))
                        .build();

                console.printf("Running predefined questions with AI model responses:\n");

                String question2 = "Pleses summarize the content of the spring-ai-mcp-overview.txt file and store it a new summary.md as Markdown format?";
                console.printf("\nQUESTION: " + question2);
                console.printf("ASSISTANT: " + chatClient.prompt(question2).call().content());
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

    @Bean
    public List<McpFunctionCallback> functionCallbacks(CoreSettings core) {
        List<McpFunctionCallback> allCallbacks = new ArrayList<>();

        for (CoreSettings.Capabilities capability : core.getCapabilities()) {
            addCapability(allCallbacks, capability);
        }

        return allCallbacks;
    }

    private void addCapability(List<McpFunctionCallback> allCallbacks, CoreSettings.Capabilities capability) {
        switch (capability.getType()) {
            case "files" -> {
                @SuppressWarnings("unchecked")
                var paths = (LinkedHashMap<String, String>) capability.getConfig().get("paths");
                McpSyncClient mcpClient = filesystemMCP.createClient(paths.values());
                mcpClients.add(mcpClient); // Track the client for cleanup
                var callbacks = mcpClient.listTools(null)
                        .tools()
                        .stream()
                        .map(tool -> new McpFunctionCallback(mcpClient, tool))
                        .toList();
                allCallbacks.addAll(callbacks);
            }
            default -> {
                log.warn("Unknown capability type: {}", capability.getType());
            }
        }
    }
}
