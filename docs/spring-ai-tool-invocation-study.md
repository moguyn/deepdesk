# Spring AI Tool Invocation Study

## Executive Summary

This document summarizes findings from a detailed investigation into Spring AI's tool invocation mechanisms, with particular focus on the Model Context Protocol (MCP) integration. The study examines how Spring AI facilitates interactions between chat models and tools, especially concerning error handling and streaming capabilities.

## Core Concepts

### Tool Invocation Flow

Spring AI implements a consistent pattern for tool invocation across different model implementations:

1. **Request Handling**: Models receive user messages and identify when tool calls are necessary
2. **Tool Management**: The `ToolCallingManager` resolves and executes tool calls
3. **Response Aggregation**: Results from tool executions are incorporated into the model's response
4. **Security Boundary**: Models don't have direct access to APIs provided as tools

### Decision-Making Process

The tool invocation decision flow typically works as follows:

1. The model receives a prompt and determines if tool calls are needed
2. If tools are needed, the model outputs a structured tool call request
3. Spring AI intercepts this request and routes it to the appropriate tool
4. Once tool execution completes, results are returned to the model
5. The model incorporates the tool results into its response generation
6. This process can repeat if the model decides additional tool calls are necessary

### Limitations with Streaming

A key limitation identified is the handling of tool calls in streaming mode:

- Both `OllamaChatModel` and `OpenAiChatModel` execute tool calls synchronously
- The streaming flow is temporarily blocked during tool execution
- There is no current support for streaming intermediate tool results while the model continues processing

## MCP Server Integration

The Model Context Protocol (MCP) server provides additional capabilities:

### Key Components

- **Transport Types**: Supports STDIO and web-based transports (HTTP, WebFlux)
- **Execution Modes**: Offers both synchronous and asynchronous operation
- **Tool Registration**: Automatic conversion of Spring AI tool callbacks to MCP tool registrations

### Tool Callback Implementation

Two primary implementations handle tool execution:

1. **SyncMcpToolCallback**: Synchronous execution through the MCP client
   ```java
   // Key method showing synchronous execution
   @Override
   public String call(String functionInput) {
       Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
       CallToolResult response = this.mcpClient.callTool(new CallToolRequest(this.tool.name(), arguments));
       if (response.isError() != null && response.isError()) {
           throw new IllegalStateException("Error calling tool: " + response.content());
       }
       return ModelOptionsUtils.toJsonString(response.content());
   }
   ```

2. **AsyncMcpToolCallback**: Asynchronous execution that uses Project Reactor
   - Despite the asynchronous implementation, tool calls still block when integrated with the model

## Error Handling Strategies

### Current Behavior

By default, tool execution errors result in exceptions that interrupt the model's flow:

```java
if (response.isError() != null && response.isError()) {
    throw new IllegalStateException("Error calling tool: " + response.content());
}
```

### Built-in Exception Processing

Spring AI provides a built-in exception handling mechanism via the `ToolExecutionExceptionProcessor` interface:

```java
// DefaultToolExecutionExceptionProcessor (simplified view)
public class DefaultToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {
    private final boolean throwExceptions;

    public DefaultToolExecutionExceptionProcessor(boolean throwExceptions) {
        this.throwExceptions = throwExceptions;
    }

    @Override
    public String processException(ToolExecutionException toolExecutionException) {
        if (throwExceptions) {
            throw toolExecutionException;
        }
        return formatErrorMessage(toolExecutionException);
    }
    
    // Additional implementation details...
}
```

By configuring this processor to not throw exceptions, errors can be returned as structured messages to the model.

### Custom Error Handling Options

Several approaches are available to improve error handling:

1. **Using ToolExecutionExceptionProcessor**:
   - Spring AI provides a built-in mechanism through `ToolExecutionExceptionProcessor`
   - `DefaultToolExecutionExceptionProcessor` can be configured to either throw exceptions or return error messages

2. **Custom ToolCallbackProvider**:
   - Override the default provider by creating a custom implementation
   - Use the `@Primary` annotation to ensure your implementation is chosen:
   ```java
   @Bean
   @Primary
   public ToolCallbackProvider customMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
       return new CustomSyncMcpToolCallbackProvider(mcpClients);
   }
   ```

3. **Disabling Auto-Configuration**:
   - Exclude `McpClientAutoConfiguration` when complete customization is needed
   - Use properties to disable: `spring.ai.mcp.client.enabled=false`

### Example Custom Implementation

Here's an example of a custom tool callback that handles errors gracefully:

```java
public class ErrorHandlingMcpToolCallback implements ToolCallback {
    private final McpSyncClient mcpClient;
    private final Tool tool;

    public ErrorHandlingMcpToolCallback(McpSyncClient mcpClient, Tool tool) {
        this.mcpClient = mcpClient;
        this.tool = tool;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(McpToolUtils.prefixedToolName(this.mcpClient.getClientInfo().name(), this.tool.name()))
            .description(this.tool.description())
            .inputSchema(ModelOptionsUtils.toJsonString(this.tool.inputSchema()))
            .build();
    }

    @Override
    public String call(String functionInput) {
        try {
            Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
            CallToolResult response = this.mcpClient.callTool(
                new CallToolRequest(this.tool.name(), arguments));
            
            if (response.isError() != null && response.isError()) {
                // Return structured error response instead of throwing exception
                return ModelOptionsUtils.toJsonString(Map.of(
                    "error", true,
                    "message", response.content(),
                    "tool", this.tool.name()
                ));
            }
            return ModelOptionsUtils.toJsonString(response.content());
        } catch (Exception e) {
            // Catch any unexpected exceptions and return as structured data
            return ModelOptionsUtils.toJsonString(Map.of(
                "error", true,
                "message", e.getMessage(),
                "type", e.getClass().getSimpleName(),
                "tool", this.tool.name()
            ));
        }
    }
}
```

## Configuration Options

### MCP Client Configuration

The MCP client can be configured through properties:

```properties
# Enable/disable the MCP client
spring.ai.mcp.client.enabled=true

# Client type: SYNC or ASYNC
spring.ai.mcp.client.type=SYNC

# Client information
spring.ai.mcp.client.name=spring-ai-mcp-client
spring.ai.mcp.client.version=1.0.0

# Timeout duration (in seconds)
spring.ai.mcp.client.request-timeout=20
```

## Impact on User Experience and Model Learning

How error handling is implemented has significant implications:

1. **User Experience**:
   - With default exception throwing: Users see raw error messages or interrupted flows
   - With structured error handling: The conversation continues naturally as the model can explain issues
   
2. **Model Learning**:
   - When errors are returned to the model instead of throwing exceptions, the model can:
     - Learn from the error and adjust its approach
     - Try alternative parameters or tools
     - Provide helpful explanations to the user
   - This creates a more resilient and adaptive system overall

## Recommendations

1. **Error Handling**: Implement a custom `ToolCallbackProvider` with improved error handling to return structured error responses instead of throwing exceptions
2. **Flow Continuity**: Configure the `ToolExecutionExceptionProcessor` to return errors rather than throw exceptions
3. **Future Enhancements**: Consider implementing a reactive approach for true streaming of intermediate tool results
4. **Model-Aware Error Design**: Structure error responses in a way that helps models understand and adapt to the error

## Conclusion

Spring AI provides a robust framework for tool invocation with various models, though certain limitations exist in streaming scenarios. The MCP integration offers flexible deployment options but follows similar patterns regarding tool execution flow. Custom implementations can address specific requirements for error handling and tool execution behavior. 