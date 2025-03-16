package com.moguyn.deepdesk.dependency;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests for {@link SoftwareDependencyValidator}
 */
class McpDependencyValidatorTest {

    @Test
    @EnabledOnOs({OS.MAC, OS.LINUX})
    void verifyDependencies_shouldNotThrowException_whenNpxIsAvailable() {
        // Given
        SoftwareDependencyValidator validator = new SoftwareDependencyValidator("echo");

        assertDoesNotThrow(() -> validator.verifyDependencies());
    }

    @Test
    void verifyDependencies_shouldThrowException_whenCommandIsNotAvailable() {
        // This test verifies that if verifyNpxAvailability throws an exception,
        // it is properly propagated through verifyDependencies

        // Create a subclass that throws an exception
        SoftwareDependencyValidator validator = new SoftwareDependencyValidator("not-existing-command");

        // When & Then
        var e = assertThrows(IllegalStateException.class, () -> validator.verifyDependencies());
        assertEquals("not-existing-command command is not available. Please install it to use this feature.", e.getMessage());
    }
}
