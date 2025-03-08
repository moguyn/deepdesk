package com.moguyn.deepdesk.advisor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.model.Content;
import org.springframework.ai.tokenizer.TokenCountEstimator;

@ExtendWith(MockitoExtension.class)
class MaxTokenSizeContenTruncatorTest {

    @Mock
    private TokenCountEstimator tokenCountEstimator;

    private MaxTokenSizeContenTruncator<TestContent> truncator;

    private static final int MAX_TOKEN_SIZE = 100;

    @BeforeEach
    public void setUp() {
        truncator = new MaxTokenSizeContenTruncator<>(tokenCountEstimator, MAX_TOKEN_SIZE);
    }

    @Test
    void testTruncate_whenTotalSizeBelowMax_shouldReturnAllContent() {
        // Arrange
        TestContent content1 = new TestContent("Content 1");
        TestContent content2 = new TestContent("Content 2");
        TestContent content3 = new TestContent("Content 3");
        List<TestContent> contentList = Arrays.asList(content1, content2, content3);

        // Mock token count estimations - each content has 20 tokens, total 60 (below max)
        when(tokenCountEstimator.estimate("Content 1")).thenReturn(20);
        when(tokenCountEstimator.estimate("Content 2")).thenReturn(20);
        when(tokenCountEstimator.estimate("Content 3")).thenReturn(20);

        // Act
        List<TestContent> result = truncator.truncate(contentList);

        // Assert
        assertEquals(3, result.size());
        assertEquals(contentList, result);
        verify(tokenCountEstimator, times(3)).estimate(anyString());
    }

    @Test
    void testTruncate_whenTotalSizeExceedsMax_shouldTruncateOldestContent() {
        // Arrange
        TestContent content1 = new TestContent("Old content");
        TestContent content2 = new TestContent("Middle content");
        TestContent content3 = new TestContent("Recent content");
        List<TestContent> contentList = Arrays.asList(content1, content2, content3);

        // Mock token counts: content1 = 50, content2 = 30, content3 = 40, total = 120
        when(tokenCountEstimator.estimate("Old content")).thenReturn(50);
        when(tokenCountEstimator.estimate("Middle content")).thenReturn(30);
        when(tokenCountEstimator.estimate("Recent content")).thenReturn(40);

        // Act
        List<TestContent> result = truncator.truncate(contentList);

        // Assert
        assertEquals(2, result.size());
        assertEquals(content2, result.get(0));
        assertEquals(content3, result.get(1));
    }

    @Test
    void testTruncate_whenAllContentExceedsMax_shouldReturnEmptyList() {
        // Arrange
        TestContent content1 = new TestContent("Very large content 1");
        TestContent content2 = new TestContent("Very large content 2");
        List<TestContent> contentList = Arrays.asList(content1, content2);

        // Mock token counts: content1 = 120, content2 = 130, total = 250
        when(tokenCountEstimator.estimate("Very large content 1")).thenReturn(120);
        when(tokenCountEstimator.estimate("Very large content 2")).thenReturn(130);

        // Act
        List<TestContent> result = truncator.truncate(contentList);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testTruncate_withEmptyList_shouldReturnEmptyList() {
        // Arrange
        List<TestContent> emptyList = Collections.emptyList();

        // Act
        List<TestContent> result = truncator.truncate(emptyList);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testTruncate_whenSingleContentExceedsMax_shouldRemoveIt() {
        // Arrange
        TestContent largeContent = new TestContent("Large content");
        TestContent smallContent = new TestContent("Small content");
        List<TestContent> contentList = Arrays.asList(largeContent, smallContent);

        // Mock token counts: largeContent = 150, smallContent = 30, total = 180
        when(tokenCountEstimator.estimate("Large content")).thenReturn(150);
        when(tokenCountEstimator.estimate("Small content")).thenReturn(30);

        // Act
        List<TestContent> result = truncator.truncate(contentList);

        // Assert
        assertEquals(1, result.size());
        assertEquals(smallContent, result.get(0));
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
