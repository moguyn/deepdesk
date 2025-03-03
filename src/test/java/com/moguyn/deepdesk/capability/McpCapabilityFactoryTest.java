package com.moguyn.deepdesk.capability;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.moguyn.deepdesk.config.CoreSettings;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * Tests for {@link McpCapabilityFactory}
 *
 * Note: This test only focuses on the public interface behavior and does not
 * test the private implementation details.
 */
class McpCapabilityFactoryTest {

    /**
     * Test that an unknown capability type throws an IllegalArgumentException
     */
    @Test
    void createCapability_shouldThrowException_whenTypeIsUnknown() {
        // Given
        McpCapabilityFactory factory = new McpCapabilityFactory();

        CoreSettings.CapabilitySettings settings = new CoreSettings.CapabilitySettings();
        settings.setType("unknown");
        settings.setConfig(new HashMap<>());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.createCapability(settings)
        );

        assertEquals("Unknown capability type: unknown", exception.getMessage());
    }

    /**
     * Test that empty paths for filesystem capability throws an
     * IllegalArgumentException
     */
    @Test
    void createCapability_shouldThrowException_whenFilesystemPathsAreEmpty() {
        // Given
        McpCapabilityFactory factory = new McpCapabilityFactory();

        CoreSettings.CapabilitySettings settings = new CoreSettings.CapabilitySettings();
        settings.setType("files");

        LinkedHashMap<String, String> paths = new LinkedHashMap<>();
        Map<String, Object> config = new HashMap<>();
        config.put("paths", paths);
        settings.setConfig(config);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.createCapability(settings)
        );

        assertEquals("Paths cannot be null or empty", exception.getMessage());
    }

    @Test
    void createCapability_shouldCreateFilesystemCapability() {
        // Given
        McpCapabilityFactory factory = new McpCapabilityFactory();

        CoreSettings.CapabilitySettings settings = new CoreSettings.CapabilitySettings();
        settings.setType("files");

        LinkedHashMap<String, String> paths = new LinkedHashMap<>();
        paths.put("path1", ".");

        Map<String, Object> config = new HashMap<>();
        config.put("paths", paths);
        settings.setConfig(config);

        // When
        try (McpSyncClient capability = factory.createCapability(settings)) {
            // Then
            assertNotNull(capability);
        }
    }
}
