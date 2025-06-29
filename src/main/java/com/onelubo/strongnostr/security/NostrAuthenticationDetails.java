package com.onelubo.strongnostr.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class NostrAuthenticationDetails {
    private final Map<String, Object> jwtClaims;
    private final String token;
    private final Instant authenticationTime;

    public NostrAuthenticationDetails(Map<String, Object> jwtClaims, String token) {
        this.jwtClaims = new HashMap<>(jwtClaims);
        this.token = token;
        this.authenticationTime = Instant.now();
    }

    public Map<String, Object> getJwtClaims() {
        return new HashMap<>(jwtClaims); // Return defensive copy
    }

    public String getToken() {
        return token;
    }

    public Instant getAuthenticationTime() {
        return authenticationTime;
    }

    public Object getClaim(String claimName) {
        return jwtClaims.get(claimName);
    }

    public boolean hasClaim(String claimName) {
        return jwtClaims.containsKey(claimName);
    }

    @Override
    public String toString() {
        return "NostrAuthenticationDetails{" +
                "claims=" + jwtClaims.keySet() +
                ", authTime=" + authenticationTime +
                '}';
    }
}
