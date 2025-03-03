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

    @Override
    @SuppressWarnings("unchecked")
    public McpSyncClient createCapability(CoreSettings.CapabilitySettings capabilitySettings) {
        return switch (capabilitySettings.getType()) {
            case "files" ->
                createFilesystem(
                ((LinkedHashMap<String, String>) capabilitySettings.getConfig().get("paths")).values());
            case "search" ->
                createSearch();
            case "everything" ->
                createEverything();
            default ->
                throw new IllegalArgumentException("Unknown capability type: " + capabilitySettings.getType());
        };
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

        return createAndInitializeClient(stdioParams);
    }

    private McpSyncClient createSearch() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search
        var params = ServerParameters.builder(NPX)
                .args("-y", "@modelcontextprotocol/server-brave-search")
                .build();

        return createAndInitializeClient(params);
    }

    private McpSyncClient createEverything() {
        // https://github.com/modelcontextprotocol/servers/tree/main/src/everything
        var params = ServerParameters.builder(NPX)
                .args("-y", "@modelcontextprotocol/server-everything")
                .build();

        return createAndInitializeClient(params);
    }

    private McpSyncClient createAndInitializeClient(ServerParameters params) {
        var mcpClient = McpClient.sync(new StdioClientTransport(params))
                .requestTimeout(REQUEST_TIMEOUT).build();

        var init = mcpClient.initialize();
        log.info("MCP Initialized: {}", init);
        return mcpClient;
    }
}
