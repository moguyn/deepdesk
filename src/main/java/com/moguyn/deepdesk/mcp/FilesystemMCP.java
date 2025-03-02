package com.moguyn.deepdesk.mcp;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.client.transport.ServerParameters;
import org.springframework.ai.mcp.client.transport.StdioClientTransport;

public class FilesystemMCP {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMCP.class);

    public McpSyncClient createClient(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("Paths cannot be null or empty");
        }

        verifyNpxAvailability();

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
}
