package com.moguyn.deepdesk.config;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Tests for the VectorStore configuration in ApplicationConfig. Note: This test
 * is a simple reflection-based check since we can't directly test the
 * ChromaVectorStore creation without the actual dependency.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationConfigVectorStoreTest {

    @Test
    void testChromaVectorStoreMethodExists() throws Exception {
        // Verify that the method exists in the configuration class
        Method method = ApplicationConfig.class.getMethod("chromaVectorStore",
                EmbeddingModel.class,
                Class.forName("org.springframework.ai.chroma.vectorstore.ChromaApi"),
                String.class,
                boolean.class);

        assertNotNull(method);
        assertTrue(VectorStore.class.isAssignableFrom(method.getReturnType()));
    }
}
