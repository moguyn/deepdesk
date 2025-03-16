package com.moguyn.deepdesk.advisor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

class StaticToolCallbackProviderTest {

    @Test
    void testConstructorWithVarargs() {
        // Arrange
        FunctionCallback callback1 = mock(FunctionCallback.class);
        FunctionCallback callback2 = mock(FunctionCallback.class);

        // Act
        ToolCallbackProvider provider = new StaticToolCallbackProvider(callback1, callback2);

        // Assert
        assertNotNull(provider);
        assertEquals(2, provider.getToolCallbacks().length);
    }

    @Test
    void testConstructorWithList() {
        // Arrange
        FunctionCallback callback1 = mock(FunctionCallback.class);
        FunctionCallback callback2 = mock(FunctionCallback.class);
        List<FunctionCallback> callbacks = Arrays.asList(callback1, callback2);

        // Act
        ToolCallbackProvider provider = new StaticToolCallbackProvider(callbacks);

        // Assert
        assertNotNull(provider);
        assertEquals(2, provider.getToolCallbacks().length);
    }

    @Test
    void testConstructorWithEmptyVarargs() {
        // Act
        ToolCallbackProvider provider = new StaticToolCallbackProvider();

        // Assert
        assertNotNull(provider);
        assertEquals(0, provider.getToolCallbacks().length);
    }

    @Test
    void testConstructorWithEmptyList() {
        // Arrange
        List<FunctionCallback> callbacks = List.of();

        // Act
        ToolCallbackProvider provider = new StaticToolCallbackProvider(callbacks);

        // Assert
        assertNotNull(provider);
        assertEquals(0, provider.getToolCallbacks().length);
    }

    @Test
    void testConstructorWithNullList() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new StaticToolCallbackProvider((List<FunctionCallback>) null));
    }

    @Test
    void testConstructorWithNullVarargs() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new StaticToolCallbackProvider((FunctionCallback[]) null));
    }
}
