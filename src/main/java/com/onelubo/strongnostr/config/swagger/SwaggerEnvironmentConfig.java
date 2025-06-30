package com.onelubo.strongnostr.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SwaggerEnvironmentConfig {

    /**
     *  Development profile configuration for Swagger - includes all endpoints
     */

    @Configuration
    @Profile("dev")
    static class DevelopmentSwaggerConfig {

        @Bean
        public GroupedOpenApi developmentApi() {
            return GroupedOpenApi.builder()
                                 .group("all")
                                 .displayName("All Endpoints (Development)")
                                 .pathsToMatch("/api/**")
                                 .packagesToScan("com.onelubo.strongnostr.rest")
                                 .build();
        }

        @Bean
        public GroupedOpenApi internalApi() {
            return GroupedOpenApi.builder()
                                 .group("internal")
                                 .displayName("Internal/Admin APIs")
                                 .pathsToMatch("/actuator/**", "/admin/**")
                                 .build();
        }
    }

    /**
     * Production profile configuration - limited documentation for security
     */
    @Configuration
    @Profile("prod")
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
    static class ProductionSwaggerConfig {

        @Bean
        public GroupedOpenApi productionPublicApi() {
            return GroupedOpenApi.builder()
                                 .group("public-only")
                                 .displayName("Public API")
                                 .pathsToMatch("/api/v1/nostr/auth/**", "/api/health")
                                 .build();
        }
    }

    /**
     * Testing profile configuration - includes test-specific endpoints
     */
    @Configuration
    @Profile("test")
    static class TestingSwaggerConfig {

        @Bean
        public GroupedOpenApi testingApi() {
            return GroupedOpenApi.builder()
                                 .group("testing")
                                 .displayName("Testing Endpoints")
                                 .pathsToMatch("/api/**", "/test/**")
                                 .build();
        }
    }
}
