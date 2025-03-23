package com.moguyn.deepdesk.tools;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link FilepathTools} class.
 */
class FilepathToolsTest {

    private FilepathTools filepathTools;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        filepathTools = new FilepathTools();
    }

    @Test
    void getAbsolutePath_shouldReturnAbsolutePathForRelativePath() {
        // Given
        String relativePath = "src/test";

        // When
        String result = filepathTools.getAbsolutePath(relativePath);

        // Then
        assertNotNull(result);
        assertTrue(result.endsWith(relativePath.replace("/", File.separator)));
        assertTrue(Paths.get(result).isAbsolute());
    }

    @Test
    void getAbsolutePath_shouldReturnSamePathForAbsolutePath() {
        // Given
        String absolutePath = Paths.get("src/test").toAbsolutePath().toString();

        // When
        String result = filepathTools.getAbsolutePath(absolutePath);

        // Then
        assertEquals(absolutePath, result);
        assertTrue(Paths.get(result).isAbsolute());
    }

    @Test
    void getAbsolutePath_shouldHandleEmptyString() {
        // Given
        String emptyPath = "";

        // When
        String result = filepathTools.getAbsolutePath(emptyPath);

        // Then
        assertNotNull(result);
        assertTrue(Paths.get(result).isAbsolute());
        // Should resolve to current working directory
        assertEquals(Paths.get("").toAbsolutePath().toString(), result);
    }
}
