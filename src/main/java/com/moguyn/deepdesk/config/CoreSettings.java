package com.moguyn.deepdesk.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for core settings in application.yaml
 */
@ConfigurationProperties(prefix = "core")
public class CoreSettings {

    @NestedConfigurationProperty
    private List<Capability> capabilities;

    @NestedConfigurationProperty
    private String protocol;
    
    @NestedConfigurationProperty
    private IO io;

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
    
    public IO getIo() {
        return io;
    }

    public void setIo(IO io) {
        this.io = io;
    }

    /**
     * Enum representing the type of capability
     */
    public enum CapabilityType {
        FILES
    }

    /**
     * Represents a capability configuration
     */
    public static class Capability {
        private CapabilityType name;
        private List<String> paths;

        public CapabilityType getName() {
            return name;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setName(CapabilityType name) {
            this.name = name;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }
    
    /**
     * Represents IO settings configuration
     */
    public static class IO {
        private String type;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
} 