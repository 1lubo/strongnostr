package com.onelubo.strongnostr.dto.nostr;

import com.onelubo.strongnostr.model.user.User;

public class NostrAuthResult {
    private boolean success;
    private String message;
    private User user;
    private String jwtToken;

    private NostrAuthResult(boolean success, String message, User user, String jwtToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.jwtToken = jwtToken;
    }

    public static NostrAuthResult success(User user, String jwtToken) {
        return new NostrAuthResult(true, "Authentication successful", user, jwtToken);
    }

    public static NostrAuthResult failure(String message) {
        return new NostrAuthResult(false, message, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public String getJwtToken() {
        return jwtToken;
    }
}
