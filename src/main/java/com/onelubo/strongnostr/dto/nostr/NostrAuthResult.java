package com.onelubo.strongnostr.dto.nostr;

import com.onelubo.strongnostr.model.user.User;

public class NostrAuthResult {
    private boolean success;
    private String message;
    private User user;
    private String accessToken;
    private String refreshToken;

    private NostrAuthResult(boolean success, String message, User user, String accessToken, String refreshToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static NostrAuthResult success(User user, String accessToken, String refreshToken) {
        return new NostrAuthResult(true, "Authentication successful", user, accessToken, refreshToken);
    }

    public static NostrAuthResult failure(String message) {
        return new NostrAuthResult(false, message, null, null, null);
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

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
