package com.onelubo.strongnostr.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerCustomization {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                             .group("public")
                             .displayName("Public API")
                             .pathsToMatch("/api/v1/nostr/auth/**")
                             .build();
    }

    @Bean
    public GroupedOpenApi protectedApi() {
        return GroupedOpenApi.builder()
                             .group("protected")
                             .displayName("Protected API")
                             .pathsToMatch("/api/v1/nostr/workout/**", "/api/v1/nostr/exercise/**")
                             .build();
    }
}
