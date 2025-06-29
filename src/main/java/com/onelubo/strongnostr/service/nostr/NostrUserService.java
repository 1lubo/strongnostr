package com.onelubo.strongnostr.service.nostr;

import com.onelubo.strongnostr.dto.nostr.NostrUserProfile;
import com.onelubo.strongnostr.model.user.User;
import com.onelubo.strongnostr.nostr.NostrKeyManager;
import com.onelubo.strongnostr.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class NostrUserService {

    private static final String BASE_USER_NAME = "nostr_user_";

    private final UserRepository userRepository;
    private final NostrKeyManager nostrKeyManager;

    public NostrUserService(UserRepository userRepository, NostrKeyManager nostrKeyManager) {
        this.userRepository = userRepository;
        this.nostrKeyManager = nostrKeyManager;
    }

    public User getOrCreateUser(String npub, NostrUserProfile userProfile) {

        return userRepository.findByNpub(npub)
                             .map(user -> {
                                 updateUserFromProfile(user, userProfile);
                                 return user;
                             })
                             .orElseGet(() -> createUser(npub, userProfile));
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    private User createUser(String npub, NostrUserProfile userProfile) {
        String hexKey = nostrKeyManager.npubToHex(npub);
        String userName = generateUniqueUsername(userProfile);

        User newUser = new User(userName, npub, hexKey);
        newUser.markAsVerified();

        if (userProfile != null) {
            updateUserFromProfile(newUser, userProfile);
        }

        return  userRepository.save(newUser);
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
