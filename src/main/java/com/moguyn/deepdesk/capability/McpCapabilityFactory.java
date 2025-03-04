package com.moguyn.deepdesk.capability;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.moguyn.deepdesk.config.CoreSettings;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class McpCapabilityFactory implements CapabililtyFactory {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String NPX = "npx";
    private static final String UVX = "uvx";

    @Override
    @SuppressWarnings("unchecked")
    public McpSyncClient createCapability(CoreSettings.CapabilitySettings capabilitySettings) {
        return switch (capabilitySettings.getType()) {
            case "files" ->
                createFilesystem(
                ((LinkedHashMap<String, String>) capabilitySettings.getConfig().get("paths")).values());
            case "search" ->
                createSearch();
            case "dummy" ->
                createDummy();
            case "fetch" ->
                createFetch();
            case "browser" ->
                createBrowser();
            default ->
                throw new IllegalArgumentException("Unknown capability type: " + capabilitySettings.getType());
        };
    }

    private McpSyncClient createBrowser() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/puppeteer
        var params = ServerParameters.builder(NPX)
                .args("-y", "@modelcontextprotocol/server-puppeteer")
                .build();

        return createAndInitialize(params);
    }

    private McpSyncClient createFilesystem(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("Paths cannot be null or empty");
        }

        // https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
        List<String> args = new ArrayList<>();
        args.add("-y");
        args.add("@modelcontextprotocol/server-filesystem");
        args.addAll(paths);
        var stdioParams = ServerParameters.builder(NPX)
                .args(args.toArray(String[]::new))
                .build();

        return createAndInitialize(stdioParams);
    }

    private McpSyncClient createSearch() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search
        var params = ServerParameters.builder(NPX)
                .args("-y", "@modelcontextprotocol/server-brave-search")
                .build();

        return createAndInitialize(params);
    }

    private McpSyncClient createDummy() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/everything
        var params = ServerParameters.builder(NPX)
                .args("-y", "@modelcontextprotocol/server-everything")
                .build();

        return createAndInitialize(params);
    }

    private McpSyncClient createFetch() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/fetch
        var params = ServerParameters.builder(UVX)
                .args("mcp-server-fetch")
                .build();

        return createAndInitialize(params);
    }

    private McpSyncClient createAndInitialize(ServerParameters params) {
        try {
            var mcpClient = McpClient.sync(new StdioClientTransport(params))
                    .requestTimeout(REQUEST_TIMEOUT).build();

            var init = mcpClient.initialize();
            log.info("MCP Initialized: {}", init);
            return mcpClient;
        } catch (Exception e) {
            log.error("Error initializing MCP client: {}", e.getMessage());
            throw new IllegalStateException("Failed to initialize MCP client", e);
        }
    }
}
