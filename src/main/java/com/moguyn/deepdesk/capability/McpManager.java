package com.moguyn.deepdesk.capability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.ai.mcp.SyncMcpToolCallback;

import com.moguyn.deepdesk.config.CoreSettings;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class McpManager implements ToolManager {

    private final Collection<CoreSettings.CapabilitySettings> capabilitiesConfig;
    private final List<McpSyncClient> mcpClients = new ArrayList<>();
    private final McpCapabilityFactory capabilityFactory;

    public McpManager(Collection<CoreSettings.CapabilitySettings> capabilities, DependencyValidator dependencyValidator) {
        this.capabilitiesConfig = capabilities;
        this.capabilityFactory = new McpCapabilityFactory();
        dependencyValidator.verifyDependencies();
    }

    @Override
    public SyncMcpToolCallback[] loadTools() {
        var tools = new ArrayList<SyncMcpToolCallback>();

        for (CoreSettings.CapabilitySettings capability : capabilitiesConfig) {
            tools.addAll(collectTools(capability));
        }

        return tools.toArray(SyncMcpToolCallback[]::new);
    }

    private Collection<SyncMcpToolCallback> collectTools(CoreSettings.CapabilitySettings capability) {
        var mcpClient = capabilityFactory.createCapability(capability);
        mcpClients.add(mcpClient);
        return listTools(mcpClient);
    }

    private Collection<SyncMcpToolCallback> listTools(McpSyncClient mcpClient) {
        return mcpClient.listTools(null)
                .tools()
                .stream()
                .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
                .toList();
    }

    @Override
    public void shutdown() {
        // Close all MCP clients when the context is closed
        mcpClients.forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.error("Error closing MCP client", e);
            }
        });
    }
}
