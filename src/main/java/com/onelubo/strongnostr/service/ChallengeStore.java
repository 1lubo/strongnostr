package com.onelubo.strongnostr.service;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;

import java.util.Optional;

public interface ChallengeStore {
    long CHALLENGE_VALIDITY_SECONDS = 300;

    void storeChallenge(NostrAuthChallenge nostrAuthChallenge);
    Optional<StoredChallenge> getChallenge(String challengeId);
    boolean markChallengeAsUsed(String challengeId);
    default void cleanupExpired() {}
    default void shutdown() {}

    record StoredChallenge(
            String challenge,
            long expiresAt,
            boolean used
    ) {
        public StoredChallenge markUsed() {
            return new StoredChallenge(challenge, expiresAt, true);
        }
    }
}
