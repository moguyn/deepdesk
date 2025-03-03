package com.moguyn.deepdesk;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Tests for the {@link DeepDesk} main application class.
 */
class DeepDeskTest {

    @Test
    void main_shouldStartSpringApplication() {
        // Given
        String[] args = {"--test"};

        // When/Then
        try (MockedStatic<SpringApplication> springAppMock = Mockito.mockStatic(SpringApplication.class)) {
            DeepDesk.main(args);

            // Verify that SpringApplication.run was called with correct parameters
            springAppMock.verify(()
                    -> SpringApplication.run(eq(DeepDesk.class), eq(args)));
        }
    }
}
