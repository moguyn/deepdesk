package com.moguyn.deepdesk.mcp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of DependencyValidator that verifies system dependencies
 * required for MCP functionality.
 */
@Slf4j
public class McpDependencyValidator implements DependencyValidator {

    private final List<String> softwares;

    public McpDependencyValidator(String... softwares) {
        this.softwares = Arrays.asList(softwares);
    }

    @Override
    public void verifyDependencies() {
        softwares.forEach(this::verifySoftwareAvailability);
    }

    /**
     * Verifies that npx command is available in the system path.
     *
     * @throws IllegalStateException if npx is not available
     */
    private void verifySoftwareAvailability(String software) {
        try {
            Process process = new ProcessBuilder("which", software)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        software + " command is not available. Please install it to use this feature.");
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to verify npx availability: " + e.getMessage(), e);
        }
    }
}
