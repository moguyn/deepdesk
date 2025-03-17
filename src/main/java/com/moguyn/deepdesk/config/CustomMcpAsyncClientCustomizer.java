package com.moguyn.deepdesk.config;

import java.io.File;
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
        if (root.name() == null || root.name().isEmpty() || root.uri() == null || root.uri().isEmpty()) {
            return false;
        }

        // Check if the path exists and is a directory
        String path = root.name();
        File file = new File(path);
        boolean isValid = file.exists() && file.isDirectory();

        if (!isValid) {
            log.warn("Path {} does not exist or is not a directory. Skipping.", path);
        }

        return isValid;
    }

    private Root resolve(String path) {
        if (path == null || path.trim().isEmpty()) {
            log.warn("Empty path provided. Skipping.");
            return new Root("", "");
        }

        try {
            var resolvedPath = Paths.get(path).toAbsolutePath().toString();
            return new Root("file://" + resolvedPath, path);
        } catch (Exception e) {
            log.warn("Invalid path: {}. Error: {}", path, e.getMessage());
            return new Root("", "");
        }
    }

    @Override
    public void customize(String serverConfiurationName, McpClient.AsyncSpec spec) {
        spec.roots(roots);
    }
}
