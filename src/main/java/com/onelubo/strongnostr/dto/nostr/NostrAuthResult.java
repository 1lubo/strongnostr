package com.onelubo.strongnostr.dto.nostr;

import com.onelubo.strongnostr.model.user.User;

public record NostrAuthResult(
        boolean success,
        String message,
        User user,
        String accessToken,
        String refreshToken
)
{
    public static NostrAuthResult success(User user, String accessToken, String refreshToken) {
        return new NostrAuthResult(true, "Authentication successful", user, accessToken, refreshToken);
    }

    public static NostrAuthResult failure(String message) {
        return new NostrAuthResult(false, message, null, null, null);
    }
}
