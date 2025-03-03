package com.moguyn.deepdesk.capability;

import org.springframework.ai.mcp.SyncMcpToolCallback;

public interface ToolManager {

    SyncMcpToolCallback[] loadTools();

    void shutdown();
}
