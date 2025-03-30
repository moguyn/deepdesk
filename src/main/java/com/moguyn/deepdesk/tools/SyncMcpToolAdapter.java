package com.moguyn.deepdesk.tools;

import java.util.Map;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.lang.NonNull;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyncMcpToolAdapter implements ToolCallback {

    private final McpSyncClient mcpClient;
    private final Tool tool;

    public SyncMcpToolAdapter(McpSyncClient mcpClient, Tool tool) {
        this.mcpClient = mcpClient;
        this.tool = tool;
    }

    @NonNull
    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name(this.tool.name())
                .description(this.tool.description())
                .inputSchema(ModelOptionsUtils.toJsonString(this.tool.inputSchema()))
                .build();
    }

    @NonNull
    @Override
    @SuppressWarnings("UseSpecificCatch")
    public String call(String functionInput) {
        log.debug("Calling tool: {}", functionInput);
        Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
        try {
            CallToolResult response = this.mcpClient.callTool(
                    new CallToolRequest(this.tool.name(), arguments));

            if (response.isError() != null && response.isError()) {
                // Throw a ToolExecutionException for consistent handling
                throw new ToolExecutionException(
                        getToolDefinition(),
                        new RuntimeException(response.content().toString())
                );
            }
            log.debug("Tool response: {}", response.content());
            return ModelOptionsUtils.toJsonString(response.content());
        } catch (Exception e) {
            // Throw all exceptions as ToolExecutionException
            if (e instanceof ToolExecutionException) {
                throw e;
            }
            throw new ToolExecutionException(getToolDefinition(), e);
        }
    }
}
