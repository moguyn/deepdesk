package com.moguyn.deepdesk.capability;

import org.springframework.ai.mcp.SyncMcpToolCallback;

public interface ToolManager extends AutoCloseable {

    SyncMcpToolCallback[] loadTools();

}
