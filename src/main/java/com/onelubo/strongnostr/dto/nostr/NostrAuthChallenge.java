package com.onelubo.strongnostr.dto.nostr;

import java.time.Instant;

public class NostrAuthChallenge {
    private final String id;
    private final String challenge;
    private final long timestamp;
    private final String message;

    public NostrAuthChallenge(String id, String challenge, long timestamp) {
        this.id = id;
        this.challenge = challenge;
        this.timestamp = timestamp;
        this.message = String.format("Strong Nostr authentication challenge: '%s' at '%s'", challenge, Instant.ofEpochMilli(timestamp).toString());
    }

    public  String getId() { return  this.id; }

    public String getChallenge() {
        return challenge;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
