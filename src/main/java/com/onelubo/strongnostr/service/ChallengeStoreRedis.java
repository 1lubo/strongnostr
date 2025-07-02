package com.onelubo.strongnostr.service;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Profile("redis")
@Component
public class ChallengeStoreRedis implements ChallengeStore {

    private final RedisTemplate<String, StoredChallenge> redisTemplate;
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public ChallengeStoreRedis(RedisTemplate<String, StoredChallenge> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.MINUTES);
    }
    @Override
    public void storeChallenge(NostrAuthChallenge nostrAuthChallenge) {
        long expiresAt = (nostrAuthChallenge.getTimestamp()  + CHALLENGE_VALIDITY_SECONDS) * 1000L;
        StoredChallenge stored = new StoredChallenge(nostrAuthChallenge.getChallenge(), expiresAt,false);
        long ttl = expiresAt - System.currentTimeMillis();
        redisTemplate.opsForValue().set(nostrAuthChallenge.getId(), stored, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<StoredChallenge> getChallenge(String challengeId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(challengeId));
    }

    @Override
    public boolean markChallengeAsUsed(String challengeId) {
        StoredChallenge challenge = redisTemplate.opsForValue().get(challengeId);
        if (challenge != null && !challenge.used() && challenge.expiresAt() > System.currentTimeMillis()) {
            redisTemplate.opsForValue().set(challengeId, challenge.markUsed());
            return  true;
        }
        return false;
    }

    @Override
    public void cleanupExpired() {
        // Redis does not require manual cleanup of expired keys, as it handles this automatically.
    }

    @Override
    public void shutdown() {
        // No-op for Redis, as it handles cleanup automatically
    }
}
