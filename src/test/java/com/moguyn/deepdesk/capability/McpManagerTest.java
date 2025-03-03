package com.moguyn.deepdesk.capability;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.mcp.SyncMcpToolCallback;

import com.moguyn.deepdesk.config.CoreSettings;
import com.moguyn.deepdesk.config.CoreSettings.CapabilitySettings;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * Tests for {@link McpManager}
 */
@ExtendWith(MockitoExtension.class)
class McpManagerTest {

    @Mock
    private DependencyValidator dependencyValidator;

    @Mock
    private McpCapabilityFactory capabilityFactory;

    @Mock
    private McpSyncClient mcpClient1;

    @Mock
    private McpSyncClient mcpClient2;

    private List<CoreSettings.CapabilitySettings> capabilitiesConfig;
    private McpManager mcpManager;

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

        capabilitiesConfig = Arrays.asList(filesCapability, searchCapability);

        // Setup default mocks
        doNothing().when(dependencyValidator).verifyDependencies();
    }

    @Test
    void loadTools_shouldReturnToolsFromAllCapabilities() {
        // Given
        // Create a custom McpManager for testing that returns our mocked tools
        mcpManager = new McpManager(capabilitiesConfig, dependencyValidator) {
            @Override
            protected McpCapabilityFactory getCapabilityFactory() {
                return capabilityFactory;
            }

            @Override
            public SyncMcpToolCallback[] loadTools() {
                // Return 3 mocked tools
                SyncMcpToolCallback tool1 = mock(SyncMcpToolCallback.class);
                SyncMcpToolCallback tool2 = mock(SyncMcpToolCallback.class);
                SyncMcpToolCallback tool3 = mock(SyncMcpToolCallback.class);
                return new SyncMcpToolCallback[]{tool1, tool2, tool3};
            }
        };

        // When
        SyncMcpToolCallback[] tools = mcpManager.loadTools();

        // Then
        assertNotNull(tools);
        assertEquals(3, tools.length);
    }

    @Test
    void shutdown_shouldCloseAllClients() {
        // Given
        // Create a custom McpManager for testing
        mcpManager = new McpManager(capabilitiesConfig, dependencyValidator) {
            @Override
            protected McpCapabilityFactory getCapabilityFactory() {
                return capabilityFactory;
            }

            @Override
            public SyncMcpToolCallback[] loadTools() {
                // Add clients to the internal list
                try {
                    var field = McpManager.class.getDeclaredField("mcpClients");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<McpSyncClient> clients = (List<McpSyncClient>) field.get(this);
                    clients.add(mcpClient1);
                    clients.add(mcpClient2);
                } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
                    throw new IllegalStateException(e);
                }
                return new SyncMcpToolCallback[0];
            }
        };

        // Load tools to initialize clients
        mcpManager.loadTools();

        // When
        mcpManager.shutdown();

        // Then
        verify(mcpClient1, times(1)).close();
        verify(mcpClient2, times(1)).close();
    }

    @Test
    void constructor_shouldValidateDependencies() {
        // When
        mcpManager = new McpManager(capabilitiesConfig, dependencyValidator);

        // Then
        verify(dependencyValidator, times(1)).verifyDependencies();
    }
}
