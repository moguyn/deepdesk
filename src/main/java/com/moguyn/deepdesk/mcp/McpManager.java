package com.moguyn.deepdesk.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.ai.mcp.SyncMcpToolCallback;

import com.moguyn.deepdesk.config.CoreSettings;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class McpManager {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    public McpSyncClient createFilesystemMCP(Collection<String> paths) {
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
                .requestTimeout(REQUEST_TIMEOUT).build();

        var init = mcpClient.initialize();

        log.info("Filesystem MCP Initialized: {}", init);

        return mcpClient;
    }

    public Collection<SyncMcpToolCallback> listTools(McpSyncClient mcpClient) {
        return mcpClient.listTools(null)
                .tools()
                .stream()
                .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
                .toList();
    }

    public McpSyncClient createSearchMCP() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search
        var params = ServerParameters.builder("npx")
                .args("-y", "@modelcontextprotocol/server-brave-search")
                .build();

        var mcpClient = McpClient.sync(new StdioClientTransport(params))
                .requestTimeout(REQUEST_TIMEOUT).build();

        var init = mcpClient.initialize();

        log.info("Search MCP Initialized: {}", init);

        return mcpClient;
    }

    public interface McpCallback {

        void onMcpClient(McpSyncClient mcpClient);
    }

    public Collection<SyncMcpToolCallback> collectTools(CoreSettings.Capabilities capability, McpCallback callback) {
        switch (capability.getType()) {
            case "files" -> {
                @SuppressWarnings("unchecked")
                var paths = (LinkedHashMap<String, String>) capability.getConfig().get("paths");
                var mcpClient = createFilesystemMCP(paths.values());
                callback.onMcpClient(mcpClient);
                return mcpClient.listTools(null)
                        .tools()
                        .stream()
                        .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
                        .toList();
            }
            case "search" -> {
                var mcpClient = createSearchMCP();
                callback.onMcpClient(mcpClient);
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
