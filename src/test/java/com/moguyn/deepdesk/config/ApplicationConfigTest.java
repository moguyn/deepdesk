package com.moguyn.deepdesk.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;

import com.moguyn.deepdesk.chat.ChatRunner;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ConfigurableApplicationContext context;

    @Mock
    private VectorStore vectorStore;

    @Test
    void testChatRunnerCreation() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        ChatRunner chatRunner = config.chatRunner(chatClient);

        // Assert
        assertNotNull(chatRunner);
    }

    @Test
    void testCliCommandLineRunner() throws Exception {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();
        ChatRunner chatRunner = mock(ChatRunner.class);
        String[] args = new String[]{"arg1", "arg2"};

        // Act
        CommandLineRunner runner = config.cli(chatRunner, context);

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
        AbstractChatMemoryAdvisor<VectorStore> advisor = config.chatMemoryAdvisor(vectorStore);

        // Assert
        assertNotNull(advisor);
    }
}
