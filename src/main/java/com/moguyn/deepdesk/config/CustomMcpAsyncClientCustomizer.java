package com.moguyn.deepdesk.config;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema.Root;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomMcpAsyncClientCustomizer implements McpAsyncClientCustomizer {

    private final List<Root> roots;

    public CustomMcpAsyncClientCustomizer(String... roots) {
        this.roots = Arrays.stream(roots)
                .map(this::resolve)
                .filter(this::valid)
                .collect(Collectors.toList());
    }

    private boolean valid(Root root) {
        return root.name() != null && !root.name().isEmpty() && root.uri() != null && !root.uri().isEmpty();
    }

    private Root resolve(String path) {
        try {
            var resolvedPath = Paths.get(path).toRealPath().toString();
            return new Root("file://" + resolvedPath, path);
        } catch (IOException e) {
            log.error("Failed to resolve path: {}", path, e);
            return new Root("", "");
        }
    }

    @Override
    public void customize(String serverConfiurationName, McpClient.AsyncSpec spec) {
        spec.roots(roots);
    }
}
