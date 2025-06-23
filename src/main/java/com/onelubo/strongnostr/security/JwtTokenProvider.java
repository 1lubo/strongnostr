package com.onelubo.strongnostr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationInMillis;
    public static final String ISSUER = "StrongNostr";

    public JwtTokenProvider(
            @Value("${jwt.secret}") SecretKey secretKey,
            @Value("${jwt.expiration}") long jwtExpirationInMillis) {

        this.secretKey = Keys.hmacShaKeyFor(secretKey.getEncoded());
        this.jwtExpirationInMillis = jwtExpirationInMillis;
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(ISSUER)
                .signWith(secretKey)
                .compact();
    }

    public String generateToken(String userId, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMillis);

        Map<String, Object> claims = new HashMap<>(additionalClaims);
        claims.put("userId", userId);
        claims.put("tokenType", "access");

        return Jwts.builder()
                .claims(additionalClaims)
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(ISSUER)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMillis * 2); // Refresh token typically has a longer expiration

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(ISSUER)
                .signWith(secretKey)
                .compact();
    }

    public String generateNostrToken(String userId, String nostrPubKey) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("nostrPubKey", nostrPubKey);
        claims.put("tokenType", "nostr");

        return generateToken(userId, claims);
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return claims.get("userId", String.class);
        }
        throw new IllegalArgumentException("Invalid token");
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return claims.getExpiration();
        }
        throw new IllegalArgumentException("Invalid token");
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (IllegalArgumentException e) {
            return true; // If we can't parse the token, consider it expired
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // If we can't parse the token, it's invalid
        }
    }

    public boolean validateToken(String token, String userId) {
        try {
            String tokenUserId = getUserIdFromToken(token);
            return validateToken(token) && tokenUserId.equals(userId) && !isTokenExpired(token);
        } catch (IllegalArgumentException e) {
            return false; // If we can't parse the token, it's invalid
        }
    }

    public boolean isRefreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);
        }
        return false;
    }

    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken) || !isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String userId = getUserIdFromToken(refreshToken);
        return generateToken(userId);
    }

    public String getNostrPubKeyFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return claims.get("nostrPubKey", String.class);
        }
        throw new IllegalArgumentException("Invalid token");
    }

    public long getTokenValiditySeconds(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Date expiration = claims.getExpiration();
            long currentTime = System.currentTimeMillis();
            return (expiration.getTime() - currentTime) / 1000; // Convert milliseconds to seconds
        }
        throw new IllegalArgumentException("Invalid token");
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();

    }
}
