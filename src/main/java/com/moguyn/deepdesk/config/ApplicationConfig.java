package com.moguyn.deepdesk.config;

import java.io.Console;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.client.transport.ServerParameters;
import org.springframework.ai.mcp.client.transport.StdioClientTransport;
import org.springframework.ai.mcp.spring.McpFunctionCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration to enable property binding
 */
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {
    // Additional beans can be defined here if needed
    @Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
			List<McpFunctionCallback> functionCallbacks, 
			ConfigurableApplicationContext context) {
				Console console = System.console();

		return _ -> {
                    try (context) {
                        var chatClient = chatClientBuilder
                                .defaultFunctions(functionCallbacks.toArray(McpFunctionCallback[]::new))
                                .build();
                        
                        console.printf("Running predefined questions with AI model responses:\n");
                        
                        String question2 = "Pleses summarize the content of the spring-ai-mcp-overview.txt file and store it a new summary.md as Markdown format?";
                        console.printf("\nQUESTION: " + question2);
                        console.printf("ASSISTANT: " + chatClient.prompt(question2).call().content());
                    }

		};
	}

	@Bean
	public List<McpFunctionCallback> functionCallbacks(McpSyncClient mcpClient) {

		var callbacks = mcpClient.listTools(null)
				.tools()
				.stream()
				.map(tool -> new McpFunctionCallback(mcpClient, tool))
				.toList();
		return callbacks;
	}

	@Bean(destroyMethod = "close")
	public McpSyncClient mcpClient() {

		// based on
		// https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
		var stdioParams = ServerParameters.builder("npx")
				.args("-y", "@modelcontextprotocol/server-filesystem", getDbPath())
				.build();

		var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
				.requestTimeout(Duration.ofSeconds(10)).build();

		var init = mcpClient.initialize();

		System.out.println("MCP Initialized: " + init);

		return mcpClient;

	}

	private static String getDbPath() {
		return Paths.get(System.getProperty("user.dir"), "target").toString();
	}
} 