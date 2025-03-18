package com.moguyn.deepdesk.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.mockConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moguyn.deepdesk.dependency.SoftwareDependencyValidator;

/**
 * Tests for the dependency validation in ApplicationConfig.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationConfigDependencyTest {

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Test
    void testDependencyValidation() {
        // Mock the constructor and instance of SoftwareDependencyValidator
        try (var mocked = mockConstruction(SoftwareDependencyValidator.class)) {
            // Call the method under test
            assertDoesNotThrow(() -> applicationConfig.dependencyValidation());

            // Verify that one validator was constructed
            assert mocked.constructed().size() == 1;
        }
    }
}
