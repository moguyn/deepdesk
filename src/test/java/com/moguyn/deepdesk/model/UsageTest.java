package com.moguyn.deepdesk.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsageTest {

    @Test
    void testUsageCreation() {
        // Arrange
        Integer inputTokens = 10;
        Integer outputTokens = 20;

        // Act
        Usage usage = new Usage(inputTokens, outputTokens);

        // Assert
        assertEquals(inputTokens, usage.inputTokens());
        assertEquals(outputTokens, usage.outputTokens());
    }

    @Test
    void testUsageEquality() {
        // Arrange
        Usage usage1 = new Usage(10, 20);
        Usage usage2 = new Usage(10, 20);
        Usage usage3 = new Usage(10, 30);

        // Assert
        assertEquals(usage1, usage2);
        assertNotEquals(usage1, usage3);
    }
}