package com.moguyn.deepdesk.mcp;

/**
 * Interface for validating system dependencies required by the application.
 */
public interface DependencyValidator {

    /**
     * Verifies that all required system dependencies are available.
     *
     * @throws IllegalStateException if any required dependencies are missing
     */
    void verifyDependencies();
}
