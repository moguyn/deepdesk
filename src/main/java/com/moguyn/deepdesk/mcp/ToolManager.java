package com.moguyn.deepdesk.mcp;

import org.springframework.ai.mcp.SyncMcpToolCallback;

public interface ToolManager {

    SyncMcpToolCallback[] loadTools();

    void shutdown();
}
