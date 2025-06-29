package com.onelubo.strongnostr.dto.nostr;

public record NostrChallengeResponse(
        String challengeId,
        String challenge,
        long expiresAt,
        String instructions
) {}
