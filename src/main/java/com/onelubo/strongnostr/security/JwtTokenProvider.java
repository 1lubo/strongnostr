package com.onelubo.strongnostr.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationInMillis;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationInMillis;

    @Value("${jwt.issuer}")
    public String issuer;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create access token for Nostr user
     */
    public String createAccessToken(String npub) {
        return createToken(npub, accessTokenExpirationInMillis, "access");
    }

    /**
     * Create refresh token for Nostr user
     */
    public String createRefreshToken(String npub) {
        return createToken(npub, refreshTokenExpirationInMillis, "refresh");
    }

    /**
     * Create JWT token with specified expiry
     */
    private String createToken(String npub, long expiryMilliseconds, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMilliseconds);

        return Jwts.builder()
                   .subject(npub)
                   .issuer(issuer)
                   .issuedAt(now)
                   .expiration(expiry)               // Expiry time
                   .claim("type", tokenType)            // Token type
                   .claim("npub", npub)                 // Explicit npub claim
                   .signWith(secretKey)                 // Sign with secret
                   .compact();
    }

    /**
     * Extract npub from JWT token
     */
    public String getNpubFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getSubject(); // Subject is the npub
        } catch (Exception e) {
            logger.debug("Failed to extract npub from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get all claims from JWT token
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    /**
     * Get token type (access/refresh)
     */
    public String getTokenType(String token) {
        try {
            return (String) getAllClaimsFromToken(token).get("type");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return true;
        } catch (SecurityException e) {
            logger.debug("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.debug("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.debug("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.debug("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Refresh access token using refresh token
     */
    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new JwtException("Invalid refresh token");
        }

        if (!"refresh".equals(getTokenType(refreshToken))) {
            throw new JwtException("Token is not a refresh token");
        }

        String npub = getNpubFromToken(refreshToken);
        if (npub == null) {
            throw new JwtException("Cannot extract npub from refresh token");
        }

        return createAccessToken(npub);
    }
}
