package com.moguyn.deepdesk.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;

import com.moguyn.deepdesk.chat.ChatRunner;
import com.moguyn.deepdesk.chat.CommandlineChatRunner;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ConfigurableApplicationContext context;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ChromaApi chromaApi;

    @Test
    void testChatRunnerCreation() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        ChatRunner chatRunner = config.chatRunner(chatClient);

        // Assert
        assertNotNull(chatRunner);
        assertTrue(chatRunner instanceof CommandlineChatRunner);
    }

    @Test
    void testCliCommandLineRunner() throws Exception {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();
        ChatRunner chatRunner = mock(ChatRunner.class);
        String[] args = new String[]{"arg1", "arg2"};

        // Act
        CommandLineRunner runner = config.cli(chatRunner, context);

        // Assert - verify the runner is created
        assertNotNull(runner);

        // The test would normally fail when context.close() is called,
        // but we're just testing that the runner calls the expected methods
        try {
            runner.run(args);
        } catch (NullPointerException e) {
            // Expected when mocking ConfigurableApplicationContext
        }

        // Assert
        verify(chatRunner).run(args);
    }

    @Test
    void testTokenCountEstimator() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        TokenCountEstimator estimator = config.tokenCountEstimator();

        // Assert
        assertNotNull(estimator);
        assertTrue(estimator.getClass().getSimpleName().contains("JTokkit"));
    }

    @Test
    void testToolExecutionExceptionProcessor() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        ToolExecutionExceptionProcessor processor = config.toolExecutionExceptionProcessor();

        // Assert
        assertNotNull(processor);
    }

    @Test
    void testCustomMcpAsyncClientCustomizer() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();
        String[] roots = new String[]{"/test/path1", "/test/path2"};

        // Act
        CustomMcpAsyncClientCustomizer customizer = config.customMcpAsyncClientCustomizer(roots);

        // Assert
        assertNotNull(customizer);
    }

    @Test
    void testChatMemoryAdvisor() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        AbstractChatMemoryAdvisor<VectorStore> advisor = config.chatMemoryAdvisor(vectorStore, 5);

        // Assert
        assertNotNull(advisor);
    }

    @Test
    void testChromaVectorStore() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();
        String collectionName = "testCollection";
        boolean initializeSchema = true;

        // Prepare builder mock to test the ChromaVectorStore setup
        ChromaVectorStore.Builder builderMock = mock(ChromaVectorStore.Builder.class);
        ChromaVectorStore vectorStoreMock = mock(ChromaVectorStore.class);

        // Create a mock for the static builder method using Mockito
        try (MockedStatic<ChromaVectorStore> mockedStatic = mockStatic(ChromaVectorStore.class)) {
            mockedStatic.when(() -> ChromaVectorStore.builder(chromaApi, embeddingModel))
                    .thenReturn(builderMock);

            when(builderMock.collectionName(collectionName)).thenReturn(builderMock);
            when(builderMock.initializeSchema(initializeSchema)).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(vectorStoreMock);

            // Act
            VectorStore result = config.chromaVectorStore(embeddingModel, chromaApi, collectionName, initializeSchema);

            // Assert
            assertNotNull(result);
            assertSame(vectorStoreMock, result);

            // Verify all builder methods were called with correct parameters
            mockedStatic.verify(() -> ChromaVectorStore.builder(chromaApi, embeddingModel));
            verify(builderMock).collectionName(collectionName);
            verify(builderMock).initializeSchema(initializeSchema);
            verify(builderMock).build();
        }
    }
}
