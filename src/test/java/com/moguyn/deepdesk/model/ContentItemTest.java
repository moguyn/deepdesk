package com.moguyn.deepdesk.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentItemTest {

    @Test
    void testContentItemCreation() {
        // Arrange
        String text = "Test content";
        String type = "text";

        // Act
        ContentItem contentItem = new ContentItem(text, type);

        // Assert
        assertEquals(text, contentItem.text());
        assertEquals(type, contentItem.type());
    }

    @Test
    void testContentItemEquality() {
        // Arrange
        ContentItem item1 = new ContentItem("Test content", "text");
        ContentItem item2 = new ContentItem("Test content", "text");
        ContentItem item3 = new ContentItem("Different content", "text");

        // Assert
        assertEquals(item1, item2);
        assertNotEquals(item1, item3);
    }
}