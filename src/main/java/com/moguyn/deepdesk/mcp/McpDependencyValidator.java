package com.moguyn.deepdesk.mcp;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of DependencyValidator that verifies system dependencies
 * required for MCP functionality.
 */
@Slf4j
public class McpDependencyValidator implements DependencyValidator {

    @Override
    public void verifyDependencies() {
        verifyNpxAvailability();
    }

    /**
     * Verifies that npx command is available in the system path.
     *
     * @throws IllegalStateException if npx is not available
     */
    private void verifyNpxAvailability() {
        try {
            Process process = new ProcessBuilder("which", "npx")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "npx command is not available. Please install Node.js and npm to use this feature.");
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to verify npx availability: " + e.getMessage(), e);
        }
    }
}
