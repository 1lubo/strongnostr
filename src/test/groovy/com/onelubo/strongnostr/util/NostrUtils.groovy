package com.onelubo.strongnostr.util

import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrEventVerifier

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

class NostrUtils {
    public static final String VALID_SIGNATURE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    public static final String VALID_NPUB = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    public static final String INVALID_SIGNATURE = "invalidsignature1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
    public static final int VALID_EVENT_KIND = 22242; // Assuming this is the kind for authentication events


    static NostrEvent createValidAuthEvent(String npub, int kind) {
        NostrEvent event = new NostrEvent()
        event.setPubkey(npub)
        event.setCreatedAt(Instant.now().getEpochSecond())
        event.setKind(kind)
        event.setContent("Strong Nostr authentication challenge: " + UUID.randomUUID().toString() + " at " + Instant.now().getEpochSecond())
        event.setSignature(VALID_SIGNATURE)
        event.setTags(List.of(["tag1", "tag2"], ["tag3", "tag4"]))
        def serialized = NostrEventVerifier.serializedEventForId(event)
        def digest = MessageDigest.getInstance("SHA-256")
        def hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8))
        event.setId(HexFormat.of().formatHex(hash))
        return event
    }
}
