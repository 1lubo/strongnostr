package com.onelubo.strongnostr.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Objects;

public class NostrAuthenticationToken extends AbstractAuthenticationToken {

    private final String npub;  // The principal (user identifier)
    private NostrAuthenticationDetails details;

    /**
     * Constructor for authenticated token
     */
    public NostrAuthenticationToken(String npub, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.npub = npub;
        super.setAuthenticated(true);
    }

    /**
     * Constructor for unauthenticated token
     */
    public NostrAuthenticationToken(String npub) {
        super(null);
        this.npub = npub;
        super.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials needed after authentication
    }

    @Override
    public Object getPrincipal() {
        return npub; // The npub is our principal
    }

    @Override
    public String getName() {
        return npub; // Return npub as the name
    }

    public String getNpub() {
        return npub;
    }

    @Override
    public void setDetails(Object details) {
        if (details instanceof NostrAuthenticationDetails) {
            this.details = (NostrAuthenticationDetails) details;
        }
        super.setDetails(details);
    }

    public NostrAuthenticationDetails getNostrDetails() {
        return details;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        NostrAuthenticationToken that = (NostrAuthenticationToken) obj;
        return Objects.equals(npub, that.npub);
    }

    @Override
    public int hashCode() {
        return Objects.hash(npub);
    }

    @Override
    public String toString() {
        return "NostrAuthenticationToken{npub='" + (npub != null ? npub.substring(0, 8) + "..." : "null") + "'}";
    }
}
