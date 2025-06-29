package com.onelubo.strongnostr.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtRequestFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                processToken(request, token);
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage());
            request.setAttribute("jwt_error", e.getMessage());
            // Don't block the request - let it continue and fail at AuthenticationEntryPoint
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HEADER_NAME);

        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }

        return null;
    }

    /**
     * Process and validate JWT token
     */
    private void processToken(HttpServletRequest request, String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {

                String npub = jwtTokenProvider.getNpubFromToken(token);

                if (npub != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    NostrAuthenticationToken authentication = createAuthentication(token, npub);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.info("Successfully authenticated user with npub: {}", maskNpub(npub));
                }
            } else {
                logger.info("Invalid JWT token received");
                request.setAttribute("jwt_error", "Invalid or expired token");
            }

        } catch (JwtException e) {
            logger.debug("JWT validation failed: {}", e.getMessage());
            request.setAttribute("jwt_error", "Token validation failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing JWT: {}", e.getMessage());
            request.setAttribute("jwt_error", "Token processing error");
        }
    }

    /**
     * Create Spring Security Authentication object from JWT
     */
    private NostrAuthenticationToken createAuthentication(String token, String npub) {
        Map<String, Object> claims = jwtTokenProvider.getAllClaimsFromToken(token);

        Collection<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
                                                                );

        NostrAuthenticationToken authentication = new NostrAuthenticationToken(npub, authorities);
        authentication.setAuthenticated(true);
        authentication.setDetails(new NostrAuthenticationDetails(claims, token));

        return authentication;
    }

    /**
     * Mask npub for logging (security)
     */
    private String maskNpub(String npub) {
        if (npub == null || npub.length() < 10) {
            return "***";
        }
        return npub.substring(0, 8) + "..." + npub.substring(npub.length() - 4);
    }

    /**
     * Skip JWT processing for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/auth/") ||
                path.startsWith("/api/public/") ||
                path.startsWith("/api/health") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-") ||
                path.startsWith("/v3/api-docs");
    }
}
