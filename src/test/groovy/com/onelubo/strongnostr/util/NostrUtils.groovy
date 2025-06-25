package com.onelubo.strongnostr.util

import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrEventVerifier

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

class NostrUtils {
    public static final String VALID_SIGNATURE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    public static final String VALID_NSEC = "nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5"
    public static final String VALID_NPUB = "npub1rtlqca8r6auyaw5n5h3l5422dm4sry5dzfee4696fqe8s6qgudks7djtfs"
    public static final String VALID_PRIVATE_KEY_HEX = "67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa"
    public static final String PUBLIC_FROM_PRIVATE_KEY_HEX = "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e"
    public static final String VALID_PUBLIC_KEY_HEX = "1afe0c74e3d7784eba93a5e3fa554a6eeb01928d12739ae8ba4832786808e36d"
    public static final String INVALID_SIGNATURE = "invalidsignature1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
    public static final int VALID_EVENT_KIND = 22242 // Assuming this is the kind for authentication events


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

    static byte[] hexStringToByteArray(String hex) {
        int len = hex.length()
        byte[] data = new byte[len / 2]
        for (int i = 0; i < len; i += 2) {
            data[i.intdiv(2)] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16))
        }
        return data
    }

    static boolean isValidHex(String hex) {
        return hex != null && hex.matches(/^[0-9a-fA-F]+$/) && hex.length() % 2 == 0
    }
}
