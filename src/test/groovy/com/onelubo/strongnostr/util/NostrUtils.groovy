package com.onelubo.strongnostr.util

import com.onelubo.strongnostr.dto.nostr.NostrAuthChallenge
import com.onelubo.strongnostr.dto.nostr.NostrUserProfile
import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrKeyManager
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec

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
    public static final String CHALLENGE_PREFIX = "Strong Nostr authentication challenge: ";

    private static final ECParameterSpec secp256k1Spec = ECNamedCurveTable.getParameterSpec("secp256k1")

    private static final SchnorrSigner schnorrSigner = new SchnorrSigner()


    static createNostrUserProfile(String nip05, String name, String picture) {
        new NostrUserProfile(
                name: name,
                avatarUrl: picture,
                nip05: nip05
        )
    }

    static createSignedNostrEvent(String npub, String nSecHex, String challenge) {
        // First create the event without signature to compute the ID
        def event = new NostrEvent(
                id: null,
                kind: 22242,
                npub: npub,
                content: challenge,
                tags: [],
                createdAt: System.currentTimeMillis() / 1000, // Convert to seconds
                signature: null
        )

        // Compute the event ID
        String eventId = computeEventId(event)

        // Sign the event ID (not the challenge content)
        String signature = schnorrSigner.signEventId(nSecHex, eventId)

        // Set both ID and signature
        event.setId(eventId)
        event.setSignature(signature)

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

    static NostrAuthChallenge generateAuthChallenge() {
        String id = UUID.randomUUID().toString();
        String challenge = CHALLENGE_PREFIX + id;
        long timestamp = Instant.now().getEpochSecond();
        return new NostrAuthChallenge(id, challenge, timestamp);
    }


    private static String computeEventId(NostrEvent nostrEvent) {
        try {
            String serialized = serializeEventForId(nostrEvent)
            MessageDigest digest = MessageDigest.getInstance("SHA-256")
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8))
            return HexFormat.of().formatHex(hash)
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute event ID", e)
        }
    }

    private static String serializeEventForId(NostrEvent nostrEvent) {
        StringBuilder sb = new StringBuilder()
        sb.append("[0,\"")

        String pubkey = nostrEvent.getPubkey()
        if (pubkey == null && nostrEvent.getNpub() != null) {
            pubkey = convertNpubToHex(nostrEvent.getNpub())
        }

        sb.append(pubkey)
        sb.append("\",")
        sb.append(nostrEvent.getCreatedAt())
        sb.append(",")
        sb.append(nostrEvent.getKind())
        sb.append(",")
        sb.append(serializeTags(nostrEvent.getTags()))
        sb.append(",\"")
        sb.append(escapeString(nostrEvent.getContent()))
        sb.append("\"]")
        return sb.toString()
    }

    private static String convertNpubToHex(String npub) {
        try{
            NostrKeyManager nostrKeyManager = new NostrKeyManager()
            return nostrKeyManager.npubToHex(npub)
        } catch(Exception e) {
            throw new RuntimeException("Failed to convert npub to hex: " + npub, e)
        }
    }

    private static String serializeTags(List<List<String>> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]"
        }

        StringBuilder sb = new StringBuilder("[")

        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",")
            }
            List<String> tag = tags.get(i)
            sb.append("[")

            for (int j = 0; j < tag.size(); j++) {
                if (j > 0) {
                    sb.append(",")
                }
                sb.append("\"").append(escapeString(tag.get(j))).append("\"")
            }
            sb.append("]")
        }
        sb.append("]")

        return sb.toString()
    }

    private static String escapeString(String str) {
        if (str == null) {
            return ""
        }

        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
    }
}
