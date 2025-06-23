package com.onelubo.strongnostr.dto;

import com.onelubo.strongnostr.nostr.NostrEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class NostrAuthRequest {

    @NotNull(message = "Nostr event must not be null")
    @Valid
    private NostrEvent nostrEvent;

    private NostrUserProfile userProfile;

    public NostrAuthRequest() {
    }

    public NostrAuthRequest(NostrEvent nostrEvent) {
        this.nostrEvent = nostrEvent;
    }

    public NostrAuthRequest(NostrEvent nostrEvent, NostrUserProfile nostrProfile) {
        this.nostrEvent = nostrEvent;
        this.userProfile = nostrProfile;
    }

    public NostrEvent getNostrEvent() {
        return nostrEvent;
    }

    public NostrUserProfile getUserProfile() {
        return userProfile;
    }
}
