package com.moguyn.deepdesk.chat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import com.moguyn.deepdesk.capability.ToolManager;

/**
 * Tests for {@link CommandlineChatRunner}
 */
@ExtendWith(MockitoExtension.class)
class CommandlineChatRunnerTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ToolManager toolManager;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private CommandlineChatRunner chatRunner;
    private final PrintStream standardOut = System.out;
    private final InputStream standardIn = System.in;
    private ByteArrayOutputStream outputStreamCaptor;

    @BeforeEach
    public void setUp() {
        // Set up output capturing
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        chatRunner = new CommandlineChatRunner(chatClient, toolManager);
    }

    @AfterEach
    public void tearDown() {
        // Reset System.out and System.in
        System.setOut(standardOut);
        System.setIn(standardIn);
    }

    @Test
    void run_shouldProcessUserInputAndReturnAiResponse() {
        // Given
        String userInput = "Hello AI\nexit\n";
        String expectedAiResponse = "Hello, I am an AI assistant.";

        // Mock System.in with our test input
        ByteArrayInputStream testIn = new ByteArrayInputStream(userInput.getBytes());
        System.setIn(testIn);

        // Mock ChatClient response chain
        when(chatClient.prompt("Hello AI")).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(expectedAiResponse);

        // When
        chatRunner.run();

        // Then
        verify(chatClient, times(1)).prompt("Hello AI");
        verify(toolManager, times(1)).shutdown();

        // Verify output contains expected strings
        String capturedOutput = outputStreamCaptor.toString();
        assert (capturedOutput.contains("我是您的AI助手"));
        assert (capturedOutput.contains("用户: "));
        assert (capturedOutput.contains("AI: " + expectedAiResponse));
    }

    @Test
    void run_shouldExitWhenUserEntersBye() {
        // Given
        String userInput = "bye\n";

        // Mock System.in with our test input
        ByteArrayInputStream testIn = new ByteArrayInputStream(userInput.getBytes());
        System.setIn(testIn);

        // When
        chatRunner.run();

        // Then
        verify(chatClient, never()).prompt(anyString());
        verify(toolManager, times(1)).shutdown();
    }

    @Test
    void run_shouldExitWhenUserEntersExit() {
        // Given
        String userInput = "exit\n";

        // Mock System.in with our test input
        ByteArrayInputStream testIn = new ByteArrayInputStream(userInput.getBytes());
        System.setIn(testIn);

        // When
        chatRunner.run();

        // Then
        verify(chatClient, never()).prompt(anyString());
        verify(toolManager, times(1)).shutdown();
    }

    @Test
    void run_shouldHandleMultipleConversationTurns() {
        // Given
        String userInput = "First message\nSecond message\nexit\n";

        // Mock System.in with our test input
        ByteArrayInputStream testIn = new ByteArrayInputStream(userInput.getBytes());
        System.setIn(testIn);

        // Mock ChatClient responses
        when(chatClient.prompt("First message")).thenReturn(requestSpec);
        when(chatClient.prompt("Second message")).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Response 1", "Response 2");

        // When
        chatRunner.run();

        // Then
        verify(chatClient, times(1)).prompt("First message");
        verify(chatClient, times(1)).prompt("Second message");
        verify(toolManager, times(1)).shutdown();

        // Verify output contains expected strings
        String capturedOutput = outputStreamCaptor.toString();
        assert (capturedOutput.contains("Response 1"));
        assert (capturedOutput.contains("Response 2"));
    }
}
