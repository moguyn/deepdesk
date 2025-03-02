package com.moguyn.deepdesk.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for core settings in application.yaml
 */
@Configuration
@ConfigurationProperties(prefix = "core")
public class CoreSettings {

    @NestedConfigurationProperty
    private List<Capability> capabilities;

    @NestedConfigurationProperty
    private String protocol;

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Represents a capability configuration
     */
    public static class Capability {
        private String name;
        private List<String> paths;

        public String getName() {
            return name;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }

    }

} 