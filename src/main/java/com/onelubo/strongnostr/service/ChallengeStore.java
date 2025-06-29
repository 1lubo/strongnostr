package com.onelubo.strongnostr.service;

import java.util.Optional;

public interface ChallengeStore {
    void storeChallenge(String challengeId, String challenge, long expiresAt);
    Optional<StoredChallenge> getChallenge(String challengeId);
    boolean markChallengeAsUsed(String challengeId);
    default void cleanupExpired() {}
    default void shutdown() {}

    public record StoredChallenge(
            String challenge,
            long expiresAt,
            boolean used
    ) {
        public StoredChallenge markUsed() {
            return new StoredChallenge(challenge, expiresAt, true);
        }
    }
}
