package com.onelubo.strongnostr.service;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory challenge storage (use Redis in production)
 */
@Profile("!redis")
@Component
public class ChallengeStoreInMemory implements ChallengeStore {

    private final Map<String, StoredChallenge> challenges = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void storeChallenge(NostrAuthChallenge nostrAuthChallenge) {
        long expiresAt = nostrAuthChallenge.getTimestamp() + CHALLENGE_VALIDITY_SECONDS * 1000L;
        StoredChallenge stored = new StoredChallenge(nostrAuthChallenge.getChallenge(), expiresAt,false);
        challenges.put(nostrAuthChallenge.getId(), stored);
    }

    @Override
    public Optional<StoredChallenge> getChallenge(String challengeId) {
        return Optional.ofNullable(challenges.get(challengeId));
    }

    @Override
    public boolean markChallengeAsUsed(String challengeId) {
        StoredChallenge challenge = challenges.get(challengeId);
        if (challenge != null && !challenge.used() && challenge.expiresAt() > System.currentTimeMillis()) {
            challenges.put(challengeId, challenge.markUsed());
            return true;
        }
        return false;
    }

    @Override
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    @PreDestroy
    @Override
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
