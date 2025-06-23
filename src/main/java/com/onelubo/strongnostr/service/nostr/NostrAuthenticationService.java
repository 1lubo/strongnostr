package com.onelubo.strongnostr.service.nostr;

import com.onelubo.strongnostr.dto.NostrAuthChallenge;
import com.onelubo.strongnostr.dto.NostrAuthRequest;
import com.onelubo.strongnostr.dto.NostrAuthResult;
import com.onelubo.strongnostr.dto.NostrUserProfile;
import com.onelubo.strongnostr.model.user.User;
import com.onelubo.strongnostr.nostr.NostrEvent;
import com.onelubo.strongnostr.nostr.NostrEventVerifier;
import com.onelubo.strongnostr.nostr.NostrKeyManager;
import com.onelubo.strongnostr.repository.UserRepository;
import com.onelubo.strongnostr.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class NostrAuthenticationService {
    private final UserRepository userRepository;
    private final NostrEventVerifier nostrEventVerifier;
    private final NostrKeyManager nostrKeyManager;
    private final JwtTokenProvider jwtTokenProvider;

    private static final long CHALLENGE_VALIDITY_SECONDS = 300;
    private static final String CHALLENGE_PREFIX = "Strong Nostr authentication challenge: ";
    public static final String BASE_USER_NAME = "nostr_user_";

    public NostrAuthenticationService(UserRepository userRepository, NostrEventVerifier nostrEventVerifier, NostrKeyManager nostrKeyManager, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.nostrEventVerifier = nostrEventVerifier;
        this.nostrKeyManager = nostrKeyManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public NostrAuthChallenge generateAuthChallenge() {
        String challenge = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        return new NostrAuthChallenge(challenge, timestamp);
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

            String nostrPubKey = event.getPubkey();
            User user = findOrCreateUser(nostrPubKey, nostrAuthRequest.getUserProfile());

            updateUserFromProfile(user, nostrAuthRequest.getUserProfile());
            user.markAsVerified();
            userRepository.save(user);

            String jwtToken = jwtTokenProvider.generateToken(user.getId());

            return NostrAuthResult.success(user, jwtToken);
        } catch (Exception e) {
            return NostrAuthResult.failure(e.getMessage());
        }
    }

    private void updateUserFromProfile(User user, NostrUserProfile profileData) {
        if (profileData == null) return;

        NostrUserProfile userNostrProfile = user.getNostrProfile();
        if (userNostrProfile == null) {
            userNostrProfile = new NostrUserProfile();
            user.setNostrProfile(userNostrProfile);
        }

        if (profileData.getName() != null) {
            userNostrProfile.setName(profileData.getName());
        }

        if (profileData.getAbout() != null) {
            userNostrProfile.setAbout(profileData.getAbout());
        }

        if (profileData.getAvatarUrl() != null) {
            userNostrProfile.setAvatarUrl(profileData.getAvatarUrl());
        }

        if (profileData.getNip05() != null) {
            userNostrProfile.setNip05(profileData.getNip05());
        }

        if (profileData.getLud16() != null) {
            userNostrProfile.setLud16(profileData.getLud16());
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
                event.getPubkey() != null && !event.getPubkey().trim().isEmpty() &&
                event.getContent() != null && !event.getContent().trim().isEmpty() &&
                event.getSignature() != null && !event.getSignature().trim().isEmpty() &&
                event.getCreatedAt() > 0;
    }

    private User findOrCreateUser(String pubkey, NostrUserProfile nostrUserProfile) {
        Optional<User> existingUser = userRepository.findByNostrPubKey(pubkey);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        String hexKey = nostrKeyManager.npubToHex(pubkey);
        String userName = generateUniqueUsername(nostrUserProfile);

        User newUser = new User(userName, pubkey, hexKey);
        newUser.markAsVerified();

        if (nostrUserProfile != null) {
            updateUserFromProfile(newUser, nostrUserProfile);
        }

        return  userRepository.save(newUser);
    }

    private String generateUniqueUsername(NostrUserProfile nostrUserProfile) {
        String baseName;

        if (nostrUserProfile != null && nostrUserProfile.getName() != null &&
                !nostrUserProfile.getName().trim().isEmpty()) {
            baseName = generateCleanUserName(nostrUserProfile.getName());
        } else {
            baseName = BASE_USER_NAME;
        }

        String userName = baseName;
        int counter = 1;

        while (userRepository.existsByUsername(userName)) {
            userName = baseName + counter++;
        }

        return  userName;
    }

    private String generateCleanUserName(String userName) {
        return userName.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("[_{2,}]", "_")
                .replaceAll("^_|_$", "")
                .substring(0, Math.min(userName.length(), 32));
    }
}
