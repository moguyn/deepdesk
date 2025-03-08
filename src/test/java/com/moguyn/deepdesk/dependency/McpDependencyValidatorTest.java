package com.moguyn.deepdesk.dependency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests for {@link McpDependencyValidator}
 */
class McpDependencyValidatorTest {

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    void verifyDependencies_shouldNotThrowException_whenNpxIsAvailable() {
        // Given
        McpDependencyValidator validator = new McpDependencyValidator("echo");

        assertDoesNotThrow(() -> validator.verifyDependencies());
    }

    @Test
    void verifyDependencies_shouldThrowException_whenCommandIsNotAvailable() {
        // This test verifies that if verifyNpxAvailability throws an exception,
        // it is properly propagated through verifyDependencies

        // Create a subclass that throws an exception
        McpDependencyValidator validator = new McpDependencyValidator("not-existing-command");

        // When & Then
        var e = assertThrows(IllegalStateException.class, () -> validator.verifyDependencies());
        assertEquals("not-existing-command command is not available. Please install it to use this feature.", e.getMessage());
    }
}
