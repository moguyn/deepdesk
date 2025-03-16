package com.moguyn.deepdesk.advisor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.model.Content;
import org.springframework.ai.tokenizer.TokenCountEstimator;

@ExtendWith(MockitoExtension.class)
class MaxTokenSizeContentLimiterTest {

    @Mock
    private TokenCountEstimator tokenCountEstimator;

    private MaxTokenSizeContentLimiter<TestContent> limiter;

    private static final int MAX_TOKEN_SIZE = 100;

    @BeforeEach
    public void setUp() {
        limiter = new MaxTokenSizeContentLimiter<>(tokenCountEstimator, MAX_TOKEN_SIZE);
    }

    @Test
    void testDoEstimateTokenCountSingleItem() {
        // Arrange
        TestContent content = new TestContent("Test content");
        when(tokenCountEstimator.estimate("Test content")).thenReturn(10);

        // Act
        int result = limiter.doEstimateTokenCount(content);

        // Assert
        assertEquals(10, result);
    }

    @Test
    void testDoEstimateTokenCountMultipleItems() {
        // Arrange
        TestContent content1 = new TestContent("Content 1");
        TestContent content2 = new TestContent("Content 2");
        TestContent content3 = new TestContent("Content 3");
        List<TestContent> contentList = Arrays.asList(content1, content2, content3);

        when(tokenCountEstimator.estimate("Content 1")).thenReturn(10);
        when(tokenCountEstimator.estimate("Content 2")).thenReturn(20);
        when(tokenCountEstimator.estimate("Content 3")).thenReturn(30);

        // Act
        int result = limiter.doEstimateTokenCount(contentList);

        // Assert
        assertEquals(60, result);
    }

    @Test
    void testDoEstimateTokenCountEmptyList() {
        // Arrange
        List<TestContent> emptyList = Collections.emptyList();

        // Act
        int result = limiter.doEstimateTokenCount(emptyList);

        // Assert
        assertEquals(0, result);
    }

    /**
     * Simple implementation of Content interface for testing
     */
    private static class TestContent implements Content {

        private final String text;

        public TestContent(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Collections.emptyMap();
        }
    }
}
