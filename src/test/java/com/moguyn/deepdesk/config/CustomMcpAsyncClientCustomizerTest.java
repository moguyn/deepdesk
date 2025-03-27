package com.moguyn.deepdesk.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.Root;

/**
 * Tests for the CustomMcpAsyncClientCustomizer.
 */
class CustomMcpAsyncClientCustomizerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFilterInvalidRoots() {
        // Arrange
        String invalidPath = "non-existent-path";
        String emptyRoot = "";

        // Act
        CustomMcpSyncClientCustomizer customizer = new CustomMcpSyncClientCustomizer(invalidPath, emptyRoot);

        // Assert
        McpClient.SyncSpec spec = mock(McpClient.SyncSpec.class);
        customizer.customize("test", spec);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Root>> rootsCaptor = ArgumentCaptor.forClass(List.class);
        verify(spec).roots(rootsCaptor.capture());

        List<Root> capturedRoots = rootsCaptor.getValue();
        assertTrue(capturedRoots.isEmpty(), "All invalid roots should be filtered out");
    }

    @Test
    void shouldResolveValidRoots() throws IOException {
        // Arrange
        Path testDirPath = tempDir.resolve("test-dir");
        Files.createDirectories(testDirPath);
        String validPath = testDirPath.toFile().getAbsolutePath();

        // Act
        CustomMcpSyncClientCustomizer customizer = new CustomMcpSyncClientCustomizer(validPath);

        // Assert
        McpClient.SyncSpec spec = mock(McpClient.SyncSpec.class);
        customizer.customize("test", spec);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Root>> rootsCaptor = ArgumentCaptor.forClass(List.class);
        verify(spec).roots(rootsCaptor.capture());

        List<Root> capturedRoots = rootsCaptor.getValue();
        assertEquals(1, capturedRoots.size(), "Should have one valid root");
        Root root = capturedRoots.get(0);
        // The resolved path might have /private prefix on macOS
        assertTrue(root.uri().startsWith("file://"), "URI should be prefixed with file://");
        assertTrue(root.uri().endsWith(validPath) || root.uri().endsWith("/private" + validPath),
                "URI should include the path (with or without /private prefix)");
        assertEquals(validPath, root.name(), "Name should be the original path");
    }

    @Test
    void shouldHandleMultipleValidRoots() throws IOException {
        // Arrange
        Path testDir1Path = tempDir.resolve("test-dir1");
        Path testDir2Path = tempDir.resolve("test-dir2");
        Files.createDirectories(testDir1Path);
        Files.createDirectories(testDir2Path);
        String validPath1 = testDir1Path.toFile().getAbsolutePath();
        String validPath2 = testDir2Path.toFile().getAbsolutePath();

        // Act
        CustomMcpSyncClientCustomizer customizer = new CustomMcpSyncClientCustomizer(validPath1, validPath2);

        // Assert
        McpClient.SyncSpec spec = mock(McpClient.SyncSpec.class);
        customizer.customize("test", spec);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Root>> rootsCaptor = ArgumentCaptor.forClass(List.class);
        verify(spec).roots(rootsCaptor.capture());

        List<Root> capturedRoots = rootsCaptor.getValue();
        assertEquals(2, capturedRoots.size(), "Should have two valid roots");

        // Check that both paths are included, accounting for macOS /private prefix
        boolean foundPath1 = false;
        boolean foundPath2 = false;

        for (Root root : capturedRoots) {
            if (root.uri().endsWith(validPath1) || root.uri().endsWith("/private" + validPath1)) {
                foundPath1 = true;
            }
            if (root.uri().endsWith(validPath2) || root.uri().endsWith("/private" + validPath2)) {
                foundPath2 = true;
            }
        }

        assertTrue(foundPath1 && foundPath2, "Both paths should be in the roots list");
    }

    @Test
    void shouldHandleMixOfValidAndInvalidRoots() throws IOException {
        // Arrange
        Path testDirPath = tempDir.resolve("test-dir");
        Files.createDirectories(testDirPath);
        String validPath = testDirPath.toFile().getAbsolutePath();
        String invalidPath = "non-existent-path";

        // Act
        CustomMcpSyncClientCustomizer customizer = new CustomMcpSyncClientCustomizer(validPath, invalidPath);

        // Assert
        McpClient.SyncSpec spec = mock(McpClient.SyncSpec.class);
        customizer.customize("test", spec);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Root>> rootsCaptor = ArgumentCaptor.forClass(List.class);
        verify(spec).roots(rootsCaptor.capture());

        List<Root> capturedRoots = rootsCaptor.getValue();
        assertEquals(1, capturedRoots.size(), "Should have only one valid root");

        Root root = capturedRoots.get(0);
        assertTrue(root.uri().endsWith(validPath) || root.uri().endsWith("/private" + validPath),
                "URI should include the path (with or without /private prefix)");
    }
}
