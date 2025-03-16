package com.moguyn.deepdesk.advisor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.ai.model.Content;

/**
 * Test for the ContextLimiter interface
 */
class ContextLimiterTest {

    @Test
    void testEmptyListTruncation() {
        // Arrange
        ContextLimiter<TestContent> limiter = new TestContextLimiter();
        List<TestContent> emptyList = Collections.emptyList();

        // Act
        List<TestContent> result = limiter.truncate(emptyList);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testSingleItemTruncation() {
        // Arrange
        ContextLimiter<TestContent> limiter = new TestContextLimiter();
        TestContent content = mock(TestContent.class);
        List<TestContent> singleItemList = Collections.singletonList(content);

        // Act
        List<TestContent> result = limiter.truncate(singleItemList);

        // Assert
        assertEquals(1, result.size());
        assertEquals(content, result.get(0));
    }

    @Test
    void testMultipleItemsTruncation() {
        // Arrange
        ContextLimiter<TestContent> limiter = new TestContextLimiter();
        TestContent content1 = mock(TestContent.class);
        TestContent content2 = mock(TestContent.class);
        TestContent content3 = mock(TestContent.class);
        List<TestContent> multipleItemsList = Arrays.asList(content1, content2, content3);

        // Act
        List<TestContent> result = limiter.truncate(multipleItemsList);

        // Assert
        assertEquals(3, result.size());
        assertEquals(content1, result.get(0));
        assertEquals(content2, result.get(1));
        assertEquals(content3, result.get(2));
    }

    /**
     * Simple implementation of ContextLimiter for testing
     */
    private static class TestContextLimiter implements ContextLimiter<TestContent> {

        @Override
        public List<TestContent> truncate(List<TestContent> messages) {
            // Simple implementation that doesn't actually truncate
            return messages;
        }
    }

    /**
     * Simple implementation of Content interface for testing
     */
    private interface TestContent extends Content {
    }
}
