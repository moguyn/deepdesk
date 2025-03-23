package com.moguyn.deepdesk.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Tests for the SyncMcpToolAdapter class.
 *
 * Note: Due to the complexity of mocking Protocol Buffer objects, these tests
 * focus primarily on the adapter's initialization and exception handling.
 */
class SyncMcpToolAdapterTest {

    private SyncMcpToolAdapter adapter;
    private McpSyncClient mockClient;
    private Tool mockTool;

    @BeforeEach
    void setUp() {
        mockClient = mock(McpSyncClient.class);
        mockTool = mock(Tool.class);

        // Configure the mock Tool with basic properties
        when(mockTool.name()).thenReturn("testTool");
        when(mockTool.description()).thenReturn("Test tool for unit tests");

        adapter = new SyncMcpToolAdapter(mockClient, mockTool);
    }

    @Test
    void constructor_shouldInitializeWithProvidedValues() {
        // Then - verify the adapter was created (implicit test of constructor)
        assertNotNull(adapter);
    }

    @Test
    void getToolDefinition_shouldReturnDefinitionWithCorrectName() {
        // When
        ToolDefinition definition = adapter.getToolDefinition();

        // Then - verify only the parts we can reliably test
        assertNotNull(definition);
        assertEquals("testTool", definition.name());
        assertEquals("Test tool for unit tests", definition.description());
    }

    @Test
    void call_shouldWrapClientExceptionInToolExecutionException() {
        // Given - a client that throws an exception
        RuntimeException clientException = new RuntimeException("Client error");
        when(mockClient.callTool(any(CallToolRequest.class))).thenThrow(clientException);

        // When/Then - verify exception handling
        ToolExecutionException exception = assertThrows(ToolExecutionException.class,
                () -> adapter.call("{}"));

        // Verify the original exception is wrapped
        assertEquals(clientException, exception.getCause());
    }
}
