package com.onelubo.strongnostr.dto.nostr;

public record NostrLoginRequest(
        String challengeId,
        String npub,
        String signature,
        String challenge
) {
}
