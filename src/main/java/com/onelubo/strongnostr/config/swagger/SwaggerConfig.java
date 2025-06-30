package com.onelubo.strongnostr.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("server.port")
    private String serverPort;

    @Value("application.version")
    String appVersion;

    @Bean
    public OpenAPI strongNostrOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server().url("https://api.strongnostr.com")
                                .description("Production Server")
                                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                                    .addSecuritySchemes("bearerAuth", createBearerAuthScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Strong Nostr API")
                .version(appVersion)
                .description("""
                             # Strong Nostr - Fitness Tracking with Nostr Authentication
                             
                             A decentralized fitness tracking application that uses Nostr protocol for authentication and user identity.
                             
                             ## Authentication
                             
                             This API uses Nostr-based authentication. To use protected endpoints:
                             
                             1. **Get Challenge**: Call `POST /api/v1/nostr/auth/challenge` to get an authentication challenge
                             2. **Sign Challenge**: Sign the challenge with your Nostr private key using BIP340 Schnorr signatures
                             3. **Login**: Submit the signed challenge via `POST /api/v1/nostr/auth/login` to get a JWT token
                             4. **Use Token**: Include the JWT token in the `Authorization: Bearer <token>` header for protected endpoints
                             
                             ## Features
                             
                             - **Nostr Authentication**: Decentralized identity using Nostr keys
                             - **Workout Tracking**: Create and manage workout sessions
                             - **Exercise Database**: Comprehensive exercise library with custom exercises
                             - **Progress Analytics**: Track your fitness progress over time
                             - **User Profiles**: Manage your fitness profile and preferences
                             """)
                .contact(new Contact()
                                 .name("Strong Nostr Team")
                                 .email("support@strongnostr.com")
                                 .url("https://strongnostr.com"))
                .license(new License()
                                 .name("MIT License")
                                 .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme createBearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtained from Nostr authentication. Format: `Bearer <your-jwt-token>`");
    }
}
