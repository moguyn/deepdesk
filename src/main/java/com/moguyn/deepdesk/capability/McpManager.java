package com.moguyn.deepdesk.capability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.mcp.SyncMcpToolCallback;

import com.moguyn.deepdesk.config.CoreSettings;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class McpManager implements ToolManager {

    private final Collection<CoreSettings.CapabilitySettings> capabilitiesConfig;
    private final Map<String, McpSyncClient> mcpClients = new HashMap<>();
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
        var mcpClient = getCapabilityFactory().createCapability(capability);
        mcpClients.put(capability.getType(), mcpClient);
        return listTools(mcpClient);
    }

    protected McpCapabilityFactory getCapabilityFactory() {
        return capabilityFactory;
    }

    private Collection<SyncMcpToolCallback> listTools(McpSyncClient mcpClient) {
        return mcpClient.listTools(null)
                .tools()
                .stream()
                .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
                .toList();
    }

    @Override
    public void close() throws Exception {
        // Close all MCP clients when the context is closed
        mcpClients.entrySet().forEach(entry -> {
            try (McpSyncClient client = entry.getValue()) {
                client.closeGracefully();
            } catch (Exception e) {
                log.error("Error closing MCP client: {}", entry.getKey());
            }
        });
    }
}
