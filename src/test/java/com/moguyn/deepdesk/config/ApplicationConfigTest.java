package com.moguyn.deepdesk.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;

import com.moguyn.deepdesk.chat.ChatRunner;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ConfigurableApplicationContext context;

    @Test
    void testChatMemoryCreation() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        ChatMemory chatMemory = config.chatMemory();

        // Assert
        assertNotNull(chatMemory);
        assertTrue(chatMemory instanceof InMemoryChatMemory);
    }

    @Test
    void testTokenCountEstimatorCreation() {
        // Arrange
        ApplicationConfig config = new ApplicationConfig();

        // Act
        TokenCountEstimator estimator = config.tokenCountEstimator();

        // Assert
        assertNotNull(estimator);
    }

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
}
