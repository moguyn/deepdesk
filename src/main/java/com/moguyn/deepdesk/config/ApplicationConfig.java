package com.moguyn.deepdesk.config;

import java.io.Console;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.moguyn.deepdesk.config.CoreSettings.CapabilityType;

/**
 * Application configuration to enable property binding
 */
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {

	private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
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
	public List<McpFunctionCallback> functionCallbacks(CoreSettings core) {
		List<McpFunctionCallback> allCallbacks = new ArrayList<>();
		
		for (CoreSettings.Capability capability : core.getCapabilities()) {
			if (CapabilityType.FILES.equals(capability.getName())) {
				McpSyncClient mcpClient = filesystemMCP(capability.getPaths());
				var callbacks = mcpClient.listTools(null)
					.tools()
					.stream()
					.map(tool -> new McpFunctionCallback(mcpClient, tool))
					.toList();
				allCallbacks.addAll(callbacks);
			}
			// Add more capabilities as needed
		}
		
		return allCallbacks;
	}

	private McpSyncClient filesystemMCP(List<String> paths) {
		if (paths == null || paths.isEmpty()) {
			throw new IllegalArgumentException("Paths cannot be null or empty");
		}

		// https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
		List<String> args = new ArrayList<>();
		args.add("-y");
		args.add("@modelcontextprotocol/server-filesystem");
		args.addAll(paths);
		var stdioParams = ServerParameters.builder("npx")
				.args(args.toArray(String[]::new))
				.build();

		var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
				.requestTimeout(Duration.ofSeconds(10)).build();

		var init = mcpClient.initialize();

		log.info("MCP Initialized: {}", init);

		return mcpClient;
	}
} 