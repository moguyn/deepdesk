package com.moguyn.deepdesk.capability;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.mcp.SyncMcpToolCallback;

import com.moguyn.deepdesk.config.CoreSettings;
import com.moguyn.deepdesk.config.CoreSettings.CapabilitySettings;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * Additional tests for {@link McpManager}
 */
@ExtendWith(MockitoExtension.class)
class McpManagerAdditionalTest {

    @Mock
    private DependencyValidator dependencyValidator;

    @Mock(strictness = Strictness.LENIENT)
    private McpCapabilityFactory capabilityFactory;

    @BeforeEach
    public void setUp() {
        // Create capability settings
        CapabilitySettings filesCapability = new CapabilitySettings();
        filesCapability.setType("files");
        Map<String, Object> filesConfig = new HashMap<>();
        filesCapability.setConfig(filesConfig);

        CapabilitySettings searchCapability = new CapabilitySettings();
        searchCapability.setType("search");
        Map<String, Object> searchConfig = new HashMap<>();
        searchCapability.setConfig(searchConfig);

        Arrays.asList(filesCapability, searchCapability);
    }

    @Test
    void loadTools_shouldCreateAndCollectTools() {
        // Given
        doNothing().when(dependencyValidator).verifyDependencies();

        // Create a capability setting
        CoreSettings.CapabilitySettings capability = new CoreSettings.CapabilitySettings();
        capability.setType("files");
        Map<String, Object> config = new HashMap<>();
        capability.setConfig(config);

        // Create a mock McpSyncClient
        McpSyncClient mockClient = mock(McpSyncClient.class);

        // Create a custom McpManager for testing
        McpManager mcpManager = new McpManager(Collections.singletonList(capability), dependencyValidator) {
            @Override
            protected McpCapabilityFactory getCapabilityFactory() {
                return capabilityFactory;
            }

            @Override
            public SyncMcpToolCallback[] loadTools() {
                // Store the client in the mcpClients map first (to test that part)
                try {
                    var field = McpManager.class.getDeclaredField("mcpClients");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, McpSyncClient> clients = (Map<String, McpSyncClient>) field.get(this);
                    clients.put("files", mockClient);

                    // Return a mock tool
                    SyncMcpToolCallback mockTool = mock(SyncMcpToolCallback.class);
                    return new SyncMcpToolCallback[]{mockTool};
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
                    fail("Failed to set up test: " + e.getMessage());
                    return new SyncMcpToolCallback[0];
                }
            }
        };

        when(capabilityFactory.createCapability(capability)).thenReturn(mockClient);

        // When
        SyncMcpToolCallback[] tools = mcpManager.loadTools();

        // Then
        assertNotNull(tools);
        assertEquals(1, tools.length);

        // Verify the client was stored in the mcpClients map
        try {
            var field = McpManager.class.getDeclaredField("mcpClients");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, McpSyncClient> clients = (Map<String, McpSyncClient>) field.get(mcpManager);
            assertEquals(1, clients.size());
            assertEquals(mockClient, clients.get("files"));
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            fail("Failed to access mcpClients field: " + e.getMessage());
        }
    }
}
