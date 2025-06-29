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

    @Override
    public String toString() {
        return "NostrAuthenticationDetails{" +
                "claims=" + jwtClaims.keySet() +
                ", authTime=" + authenticationTime +
                '}';
    }
}
