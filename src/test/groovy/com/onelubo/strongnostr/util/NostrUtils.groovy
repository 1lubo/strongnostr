package com.onelubo.strongnostr.util

import com.onelubo.strongnostr.nostr.NostrEvent
import com.onelubo.strongnostr.nostr.NostrEventVerifier
import com.onelubo.strongnostr.nostr.NostrKeyManager
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECPoint

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
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

    private static final ECParameterSpec secp256k1Spec = ECNamedCurveTable.getParameterSpec("secp256k1")
    private static final ECDomainParameters domainParameters = new ECDomainParameters(
            secp256k1Spec.getCurve(),
            secp256k1Spec.getG(),
            secp256k1Spec.getN(),
            secp256k1Spec.getH()
    )

    private static final SchnorrSigner schnorrSigner = new SchnorrSigner()

    private static final NostrEventVerifier nostrEventVerifier = new NostrEventVerifier(new NostrKeyManager())

    static NostrEvent createValidAuthEvent(String npub, int kind) {
        NostrEvent event = new NostrEvent()
        event.setNpub(npub)
        event.setCreatedAt(Instant.now().toEpochMilli())
        event.setKind(kind)
        event.setContent("Strong Nostr authentication challenge: " + UUID.randomUUID().toString() + " at " + Instant.now().getEpochSecond())
        event.setSignature(VALID_SIGNATURE)
        event.setTags(List.of(["tag1", "tag2"], ["tag3", "tag4"]))
        def serialized = NostrEventVerifier.serializeEventForId(event)
        def digest = MessageDigest.getInstance("SHA-256")
        def hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8) as byte[])
        event.setId(HexFormat.of().formatHex(hash))
        return event
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
        String eventId = nostrEventVerifier.computeEventId(event)

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

    static String signChallenge(String challenge, String privateKeyHex) {
        try {
            // Convert hex private key to BigInteger
            BigInteger privateKey = new BigInteger(privateKeyHex, 16)

            // Hash the challenge message
            byte[] messageHash = hashMessage(challenge)

            // Generate Schnorr signature
            byte[] signature = generateSchnorrSignature(messageHash, privateKey)

            // Return as hex string
            return bytesToHex(signature)

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign challenge", e)
        }
    }

    private static byte[] hashMessage(String message) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(message.getBytes(StandardCharsets.UTF_8))
    }

    private static byte[] generateSchnorrSignature(byte[] messageHash, BigInteger privateKey) throws Exception {
        // Generate random nonce k
        BigInteger k = generateNonce(messageHash, privateKey)

        // Calculate R = k * G
        ECPoint R = domainParameters.getG().multiply(k).normalize()

        // Extract x-coordinate of R (r value)
        BigInteger r = R.getAffineXCoord().toBigInteger()

        // If R.y is odd, negate k (BIP340 requirement)
        if (R.getAffineYCoord().toBigInteger().testBit(0)) {
            k = domainParameters.getN().subtract(k)
        }

        // Calculate public key point P = d * G
        ECPoint P = domainParameters.getG().multiply(privateKey).normalize()
        BigInteger px = P.getAffineXCoord().toBigInteger()

        // Calculate challenge e = H(r || P || m)
        BigInteger e = calculateChallenge(r, px, messageHash)

        // Calculate s = (k + e * d) mod n
        BigInteger s = k.add(e.multiply(privateKey)).mod(domainParameters.getN())

        // Return signature as r || s (64 bytes total)
        return combineSignatureComponents(r, s)
    }

    private static BigInteger generateNonce(byte[] messageHash, BigInteger privateKey) throws Exception {
        // Simple deterministic nonce generation (in production, use full RFC 6979)
        MessageDigest digest = MessageDigest.getInstance("SHA-256")

        // Combine private key and message hash
        byte[] privateKeyBytes = privateKey.toByteArray()
        digest.update(privateKeyBytes)
        digest.update(messageHash)

        // Add some randomness to prevent nonce reuse
        SecureRandom random = new SecureRandom()
        byte[] randomBytes = new byte[32]
        random.nextBytes(randomBytes)
        digest.update(randomBytes)

        byte[] nonceBytes = digest.digest()
        BigInteger nonce = new BigInteger(1, nonceBytes)

        // Ensure nonce is within valid range [1, n-1]
        BigInteger n = domainParameters.getN()
        nonce = nonce.mod(n.subtract(BigInteger.ONE)).add(BigInteger.ONE)

        return nonce
    }

    private static BigInteger calculateChallenge(BigInteger r, BigInteger px, byte[] messageHash) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256")

        // Add r (32 bytes, big-endian)
        byte[] rBytes = bigIntegerToBytes32(r)
        digest.update(rBytes)

        // Add public key x-coordinate (32 bytes, big-endian)
        byte[] pxBytes = bigIntegerToBytes32(px)
        digest.update(pxBytes)

        // Add message hash
        digest.update(messageHash)

        // Return as BigInteger
        byte[] challengeBytes = digest.digest()
        return new BigInteger(1, challengeBytes)
    }

    private static byte[] bigIntegerToBytes32(BigInteger value) {
        byte[] bytes = value.toByteArray()

        if (bytes.length == 32) {
            return bytes
        } else if (bytes.length > 32) {
            // Remove leading zero byte if present
            return Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length)
        } else {
            // Pad with leading zeros
            byte[] padded = new byte[32]
            System.arraycopy(bytes, 0, padded, 32 - bytes.length, bytes.length)
            return padded
        }
    }

    private static byte[] combineSignatureComponents(BigInteger r, BigInteger s) {
        byte[] rBytes = bigIntegerToBytes32(r)
        byte[] sBytes = bigIntegerToBytes32(s)

        byte[] signature = new byte[64]
        System.arraycopy(rBytes, 0, signature, 0, 32)
        System.arraycopy(sBytes, 0, signature, 32, 32)

        return signature
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder()
        for (byte b : bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
