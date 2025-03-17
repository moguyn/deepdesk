package com.moguyn.deepdesk.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

    @Test
    void testCorsConfiguration() {
        // Arrange
        WebConfig webConfig = new WebConfig();
        CorsRegistry registry = Mockito.mock(CorsRegistry.class);
        CorsRegistration corsRegistration = Mockito.mock(CorsRegistration.class);

        // Mock registry.addMapping to return corsRegistration
        Mockito.when(registry.addMapping("/**")).thenReturn(corsRegistration);

        // Mock chained method calls on corsRegistration
        Mockito.when(corsRegistration.allowedOriginPatterns("*")).thenReturn(corsRegistration);
        Mockito.when(corsRegistration.allowedMethods(Mockito.any(String[].class))).thenReturn(corsRegistration);
        Mockito.when(corsRegistration.allowedHeaders(Mockito.any(String[].class))).thenReturn(corsRegistration);
        Mockito.when(corsRegistration.allowCredentials(true)).thenReturn(corsRegistration);
        Mockito.when(corsRegistration.maxAge(3600)).thenReturn(corsRegistration);

        // Act
        webConfig.addCorsMappings(registry);

        // Assert
        Mockito.verify(registry).addMapping("/**");
        Mockito.verify(corsRegistration).allowedOriginPatterns("*");
        Mockito.verify(corsRegistration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        Mockito.verify(corsRegistration).allowedHeaders("Content-Type", "Authorization", "X-Requested-With");
        Mockito.verify(corsRegistration).allowCredentials(true);
        Mockito.verify(corsRegistration).maxAge(3600);
    }
}
