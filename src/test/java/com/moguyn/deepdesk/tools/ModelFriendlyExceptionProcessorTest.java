package com.moguyn.deepdesk.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

class ModelFriendlyExceptionProcessorTest {

    private ModelFriendlyExceptionProcessor processor;
    private ToolExecutionException mockException;
    private ToolDefinition mockToolDefinition;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        processor = new ModelFriendlyExceptionProcessor();
        mockToolDefinition = ToolDefinition.builder()
                .name("testTool")
                .description("Test tool for unit tests")
                .inputSchema("{\"type\":\"object\",\"properties\":{\"test\":{\"type\":\"string\"}}}")
                .build();

        Exception mockCause = new RuntimeException("Test cause message");
        mockException = new ToolExecutionException(mockToolDefinition, mockCause);
    }

    @Test
    void process_shouldReturnJsonStringWithErrorDetails() {
        // When
        String result = processor.process(mockException);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("\"status\":\"error\""));
        assertTrue(result.contains("\"errorType\":\"ToolExecutionException\""));
        assertTrue(result.contains("\"errorMessage\":"));
        assertTrue(result.contains("\"cause\":\"Test cause message\""));
        assertTrue(result.contains("\"toolDefinition\":"));
    }

    @Test
    void process_shouldIncludeToolDefinitionInformation() {
        // When
        String result = processor.process(mockException);

        // Then
        assertTrue(result.contains("\"testTool\""));
        assertTrue(result.contains("\"Test tool for unit tests\""));
    }

    @Test
    void process_shouldHandleNullCause() {
        // Given
        RuntimeException causeWithNullMessage = new RuntimeException() {
            @Override
            public String getMessage() {
                return null;
            }
        };
        ToolExecutionException exceptionWithNullCause = new ToolExecutionException(mockToolDefinition, causeWithNullMessage);

        // When
        String result = processor.process(exceptionWithNullCause);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("\"status\":\"error\""));
        assertTrue(result.contains("\"errorType\":\"ToolExecutionException\""));
    }
}
