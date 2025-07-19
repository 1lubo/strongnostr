package com.onelubo.strongnostr.service.nostr;

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge;
import com.onelubo.strongnostr.dto.nostr.NostrAuthRequest;
import com.onelubo.strongnostr.dto.nostr.NostrAuthResult;
import com.onelubo.strongnostr.model.user.User;
import com.onelubo.strongnostr.nostr.NostrEvent;
import com.onelubo.strongnostr.nostr.NostrKeyManager;
import com.onelubo.strongnostr.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static com.onelubo.strongnostr.nostr.NostrSignatureVerifier.verifySchnorrSignature;

@Service
@Transactional
public class NostrAuthenticationService {

    private final Logger logger = LoggerFactory.getLogger(NostrAuthenticationService.class);

    private final NostrUserService nostrUserService;
    private final JwtTokenProvider jwtTokenProvider;

    private static final long CHALLENGE_VALIDITY_SECONDS = 300;
    private static final String CHALLENGE_PREFIX = "Strong Nostr authentication challenge: ";


    public NostrAuthenticationService(NostrUserService nostrUserService, JwtTokenProvider jwtTokenProvider) {
        this.nostrUserService = nostrUserService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public NostrAuthChallenge generateAuthChallenge() {
        String id = UUID.randomUUID().toString();
        String challenge = CHALLENGE_PREFIX + id;
        long timestamp = Instant.now().getEpochSecond();
        return new NostrAuthChallenge(id, challenge, timestamp);
    }

    public NostrAuthResult authenticateWithNostrEvent(NostrAuthRequest nostrAuthRequest, String expectedChallenge) {
        try {
            NostrEvent event = nostrAuthRequest.getNostrEvent();

            if (!isValidNostrAuthEvent(event)) {
                logger.debug("Invalid event received: {}", event);
                return NostrAuthResult.failure("Invalid nostr event structure");
            }

            if (!isValidChallenge(event.getContent(), event.getCreatedAt())) {
                logger.debug("Invalid or expired challenge: {}", event.getContent());
                return NostrAuthResult.failure("Invalid or expired challenge");
            }

            String computedEventId = computeEventId(event);

            if (!computedEventId.equals(event.getId())) {
                logger.debug("Event ID mismatch: computed={}, expected={}", computedEventId, event.getId());
                return NostrAuthResult.failure("Event ID mismatch");
            }

            if (!event.getContent().contains(expectedChallenge)) {
                logger.debug("Challenge mismatch: expected={}, received={}", expectedChallenge, event.getContent());
                return NostrAuthResult.failure("Challenge mismatch");
            }

            if (!verifySchnorrSignature(event.getPubkey(), computedEventId, event.getSignature())) {
                logger.debug("Invalid signature for event ID: {}", computedEventId);
                return NostrAuthResult.failure("Invalid signature");
            }

            String nostrPubKey = event.getNpub();
            User user = nostrUserService.getOrCreateUser(nostrPubKey, nostrAuthRequest.getUserProfile());

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
                event.getNpub() != null && !event.getNpub().trim().isEmpty() &&
                event.getContent() != null && !event.getContent().trim().isEmpty() &&
                event.getSignature() != null && !event.getSignature().trim().isEmpty() &&
                event.getCreatedAt() > 0;
    }

    private String computeEventId(NostrEvent nostrEvent) {
        try {
            String serialized = serializeEventForId(nostrEvent);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute event ID", e);
        }
    }

    private static String serializeEventForId(NostrEvent nostrEvent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[0,\"");

        String pubkey = nostrEvent.getPubkey();
        if (pubkey == null && nostrEvent.getNpub() != null) {
            pubkey = convertNpubToHex(nostrEvent.getNpub());
        }

        sb.append(pubkey);
        sb.append("\",");
        sb.append(nostrEvent.getCreatedAt());
        sb.append(",");
        sb.append(nostrEvent.getKind());
        sb.append(",");
        sb.append(serializeTags(nostrEvent.getTags()));
        sb.append(",\"");
        sb.append(escapeString(nostrEvent.getContent()));
        sb.append("\"]");
        return sb.toString();
    }

    private static String convertNpubToHex(String npub) {
        try{
            NostrKeyManager nostrKeyManager = new NostrKeyManager();
            return nostrKeyManager.npubToHex(npub);
        } catch(Exception e) {
            throw new RuntimeException("Failed to convert npub to hex: " + npub, e);
        }
    }

    private static String serializeTags(List<List<String>> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            List<String> tag = tags.get(i);
            sb.append("[");

            for (int j = 0; j < tag.size(); j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeString(tag.get(j))).append("\"");
            }
            sb.append("]");
        }
        sb.append("]");

        return sb.toString();
    }

    private static String escapeString(String str) {
        if (str == null) {
            return "";
        }

        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
