package com.moguyn.deepdesk.tools;

import java.util.Map;

import org.apache.groovy.util.Maps;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.lang.NonNull;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelFriendlyExceptionProcessor implements ToolExecutionExceptionProcessor {

    @NonNull
    @Override
    public String process(@NonNull ToolExecutionException exception) {
        Map<String, Object> errorResponse = Maps.of(
                "status", "error",
                "errorType", exception.getClass().getSimpleName(),
                "errorMessage", exception.getMessage(),
                "cause", exception.getCause().getMessage(),
                "toolDefinition", exception.getToolDefinition()
        );
        log.debug("Tool execution error: {}", errorResponse);

        // Convert to JSON string for the model
        return ModelOptionsUtils.toJsonString(errorResponse);
    }
}
