package com.onelubo.strongnostr.service.nostr;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;
import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest;
import com.onelubo.strongnostr.dto.nostr.NostrAuthResult;
import com.onelubo.strongnostr.model.user.User;
import com.onelubo.strongnostr.nostr.NostrEvent;
import com.onelubo.strongnostr.nostr.NostrEventVerifier;
import com.onelubo.strongnostr.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class NostrAuthenticationService {

    private final NostrUserService nostrUserService;
    private final NostrEventVerifier nostrEventVerifier;
    private final JwtTokenProvider jwtTokenProvider;

    private static final long CHALLENGE_VALIDITY_SECONDS = 300;
    private static final String CHALLENGE_PREFIX = "Strong Nostr authentication challenge: ";


    public NostrAuthenticationService(NostrUserService nostrUserService, NostrEventVerifier nostrEventVerifier,
                                      JwtTokenProvider jwtTokenProvider) {
        this.nostrUserService = nostrUserService;
        this.nostrEventVerifier = nostrEventVerifier;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public NostrAuthChallenge generateAuthChallenge() {
        String id = UUID.randomUUID().toString();
        String challenge = CHALLENGE_PREFIX + id;
        long timestamp = Instant.now().getEpochSecond();
        return new NostrAuthChallenge(id, challenge, timestamp);
    }

    public NostrAuthResult authenticateWithNostrEvent(NostrAuthRequest nostrAuthRequest) {
        try {
            NostrEvent event = nostrAuthRequest.getNostrEvent();

            if (!isValidNostrAuthEvent(event)) {
                return NostrAuthResult.failure("Invalid nostr event structure");
            }

            if (!isValidChallenge(event.getContent(), event.getCreatedAt())) {
                return NostrAuthResult.failure("Invalid or expired challenge");
            }

            if (!nostrEventVerifier.verifyEventSignature(event)) {
                return NostrAuthResult.failure("Invalid event signature");
            }

            String nostrPubKey = event.getnPub();
            User user = nostrUserService.getOrCreateUser(nostrPubKey, nostrAuthRequest.getUserProfile());

            user.markAsVerified();
            nostrUserService.saveUser(user);

            String accessToken = jwtTokenProvider.createAccessToken(user.getnPub());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getnPub());

            return NostrAuthResult.success(user, accessToken, refreshToken);
        } catch (Exception e) {
            return NostrAuthResult.failure(e.getMessage());
        }
    }

    private boolean isValidChallenge(String challenge, long timestamp) {
        long currentTime = Instant.now().getEpochSecond();

        if (Math.abs(currentTime - timestamp) > CHALLENGE_VALIDITY_SECONDS) {
            return false;
        }

        return challenge != null && !challenge.isEmpty() &&
               challenge.startsWith(CHALLENGE_PREFIX) &&
                challenge.length() > CHALLENGE_PREFIX.length() + 30; // 30 is the length of a typical UUID
    }

    private boolean isValidNostrAuthEvent(NostrEvent event) {
        return event != null &&
                event.getKind() == 22242 && // NIP-46 authentication event kind
                event.getnPub() != null && !event.getnPub().trim().isEmpty() &&
                event.getContent() != null && !event.getContent().trim().isEmpty() &&
                event.getSignature() != null && !event.getSignature().trim().isEmpty() &&
                event.getCreatedAt() > 0;
    }
}
