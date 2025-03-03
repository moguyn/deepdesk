package com.moguyn.deepdesk.capability;

import com.moguyn.deepdesk.config.CoreSettings;

import io.modelcontextprotocol.client.McpSyncClient;

public interface CapabililtyFactory {

    public McpSyncClient createCapability(CoreSettings.CapabilitySettings capabilitySettings);
}
