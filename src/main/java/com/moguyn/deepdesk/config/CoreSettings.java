package com.moguyn.deepdesk.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for core settings in application.yaml
 */
@ConfigurationProperties(prefix = "core")
public class CoreSettings {

    @NestedConfigurationProperty
    private List<Capabilities> capabilities;

    @NestedConfigurationProperty
    private String protocol;
    
    @NestedConfigurationProperty
    private IO io;

    public List<Capabilities> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<Capabilities> capabilities) {
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

    public static class Capabilities {
        private String type;
        private Map<String, Object> config;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
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