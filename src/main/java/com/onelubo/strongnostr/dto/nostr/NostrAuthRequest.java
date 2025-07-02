package com.onelubo.strongnostr.dto.nostr;

import com.onelubo.strongnostr.nostr.NostrEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NostrAuthRequest {

    @NotNull(message = "Nostr event must not be null")
    @Valid
    private NostrEvent nostrEvent;

    @NotNull(message = "Challenge id must not be null")
    @NotBlank (message = "Challenge id must not be blank")
    private String challengeId;

    private NostrUserProfile userProfile;

    public NostrAuthRequest() {
    }

    public NostrAuthRequest(NostrEvent nostrEvent) {
        this.nostrEvent = nostrEvent;
    }

    public NostrEvent getNostrEvent() {
        return nostrEvent;
    }

    public NostrUserProfile getUserProfile() {
        return userProfile;
    }

    public String getChallengeId() {
        return challengeId;
    }

    @Override
    public String toString() {
        return "NostrAuthRequest{" +
                "nostrEvent=" + nostrEvent +
                ", challengeId='" + challengeId + '\'' +
                ", userProfile=" + userProfile +
                '}';
    }
}
