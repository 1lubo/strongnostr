package com.onelubo.strongnostr.util

import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class SchnorrSigner {
    private final ECDomainParameters domainParameters
    private final SecureRandom secureRandom
    private ECParameterSpec secp256k1Spec

    SchnorrSigner() {
        this.secp256k1Spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        this.domainParameters = new ECDomainParameters(
                secp256k1Spec.getCurve(),
                secp256k1Spec.getG(),
                secp256k1Spec.getN(),
                secp256k1Spec.getH()
        )
        this.secureRandom = new SecureRandom()
    }

    /**
     * Sign a Nostr event ID (32-byte hash) using BIP340 Schnorr signatures
     * This is what Nostr actually does - sign the event ID, not the raw content
     */
    String signEventId(String privateKeyHex, String eventIdHex) {
        try {
            // Convert private key from hex
            def privateKey = new BigInteger(privateKeyHex, 16)

            // Convert event ID from hex to bytes (should be 32 bytes)
            def eventIdBytes = HexFormat.of().parseHex(eventIdHex)

            if (eventIdBytes.length != 32) {
                throw new IllegalArgumentException("Event ID must be exactly 32 bytes")
            }

            // Generate Schnorr signature over the event ID hash
            def signature = signMessageHash(eventIdBytes, privateKey)

            // Return as hex string (64 bytes = 128 hex chars)
            return HexFormat.of().formatHex(signature)

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign event ID", e)
        }
    }

    /**
     * Sign a challenge string using BIP340 Schnorr signatures
     * This method hashes the message during signing
     */
    String signChallenge(String privateKeyHex, String challenge) {
        try {
            // Convert private key from hex
            def privateKey = new BigInteger(privateKeyHex, 16)

            // Convert challenge to bytes
            def messageBytes = challenge.getBytes(StandardCharsets.UTF_8)

            // Generate Schnorr signature (this will hash the message)
            def signature = signMessage(messageBytes, privateKey)

            // Return as hex string (64 bytes = 128 hex chars)
            return HexFormat.of().formatHex(signature)

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign challenge", e)
        }
    }

    /**
     * Core BIP340 Schnorr signing for pre-hashed messages (like Nostr event IDs)
     */
    private byte[] signMessageHash(byte[] messageHash, BigInteger privateKey) {
        // Ensure we have the correct public key (with even Y coordinate per BIP340)
        def P = domainParameters.getG().multiply(privateKey).normalize()

        // Apply BIP340 key normalization - negate private key if public key Y is odd
        if (P.getAffineYCoord().toBigInteger().testBit(0)) {
            privateKey = domainParameters.getN().subtract(privateKey)
            P = P.negate().normalize()
        }

        def px = P.getAffineXCoord().toBigInteger()

        // Generate nonce
        def k = generateNonceForHash(messageHash, privateKey)

        // Calculate R = k * G
        def R = domainParameters.getG().multiply(k).normalize()
        def rx = R.getAffineXCoord().toBigInteger()

        // If R.y is odd, negate k (BIP340 requirement)
        if (R.getAffineYCoord().toBigInteger().testBit(0)) {
            k = domainParameters.getN().subtract(k)
        }

        // Calculate challenge e = H(rx || px || messageHash)
        def e = calculateChallengeFromHash(rx, px, messageHash)

        // Calculate s = (k + e * d) mod n
        def s = k.add(e.multiply(privateKey)).mod(domainParameters.getN())

        return combineSignature(rx, s)
    }

    /**
     * Core BIP340 Schnorr signing for raw messages (will be hashed during challenge calculation)
     */
    private byte[] signMessage(byte[] message, BigInteger privateKey) {
        // Ensure we have the correct public key (with even Y coordinate per BIP340)
        def P = domainParameters.getG().multiply(privateKey).normalize()

        // Apply BIP340 key normalization - negate private key if public key Y is odd
        if (P.getAffineYCoord().toBigInteger().testBit(0)) {
            privateKey = domainParameters.getN().subtract(privateKey)
            P = P.negate().normalize()
        }

        def px = P.getAffineXCoord().toBigInteger()

        // Generate nonce
        def k = generateNonce(message, privateKey)

        // Calculate R = k * G
        def R = domainParameters.getG().multiply(k).normalize()
        def rx = R.getAffineXCoord().toBigInteger()

        // If R.y is odd, negate k (BIP340 requirement)
        if (R.getAffineYCoord().toBigInteger().testBit(0)) {
            k = domainParameters.getN().subtract(k)
        }

        // Calculate challenge e = H(rx || px || message)
        def e = calculateChallenge(rx, px, message)

        // Calculate s = (k + e * d) mod n
        def s = k.add(e.multiply(privateKey)).mod(domainParameters.getN())

        return combineSignature(rx, s)
    }

    /**
     * Generate deterministic nonce for pre-hashed messages
     */
    private BigInteger generateNonceForHash(byte[] messageHash, BigInteger privateKey) {
        def digest = MessageDigest.getInstance("SHA-256")

        // Combine private key and message hash for deterministic nonce
        def privateKeyBytes = bigIntegerToBytes32(privateKey)
        digest.update(privateKeyBytes)
        digest.update(messageHash)

        def nonceBytes = digest.digest()
        def nonce = new BigInteger(1, nonceBytes)

        // Ensure nonce is in valid range [1, n-1]
        def n = domainParameters.getN()
        nonce = nonce.mod(n.subtract(BigInteger.ONE)).add(BigInteger.ONE)

        return nonce
    }

    /**
     * Generate deterministic nonce for raw messages
     */
    private BigInteger generateNonce(byte[] message, BigInteger privateKey) {
        def digest = MessageDigest.getInstance("SHA-256")

        // Combine private key and message for deterministic nonce
        def privateKeyBytes = bigIntegerToBytes32(privateKey)
        digest.update(privateKeyBytes)
        digest.update(message)

        def nonceBytes = digest.digest()
        def nonce = new BigInteger(1, nonceBytes)

        // Ensure nonce is in valid range [1, n-1]
        def n = domainParameters.getN()
        nonce = nonce.mod(n.subtract(BigInteger.ONE)).add(BigInteger.ONE)

        return nonce
    }

    /**
     * Calculate BIP340 challenge for pre-hashed messages: e = H(rx || px || messageHash)
     */
    private BigInteger calculateChallengeFromHash(BigInteger rx, BigInteger px, byte[] messageHash) {
        def digest = MessageDigest.getInstance("SHA-256")

        // Add rx (32 bytes)
        digest.update(bigIntegerToBytes32(rx))

        // Add px (32 bytes)
        digest.update(bigIntegerToBytes32(px))

        // Add message hash (already 32 bytes)
        digest.update(messageHash)

        def challengeBytes = digest.digest()
        return new BigInteger(1, challengeBytes)
    }

    /**
     * Calculate BIP340 challenge for raw messages: e = H(rx || px || message)
     */
    private BigInteger calculateChallenge(BigInteger rx, BigInteger px, byte[] message) {
        def digest = MessageDigest.getInstance("SHA-256")

        // Add rx (32 bytes)
        digest.update(bigIntegerToBytes32(rx))

        // Add px (32 bytes)
        digest.update(bigIntegerToBytes32(px))

        // Add message
        digest.update(message)

        def challengeBytes = digest.digest()
        return new BigInteger(1, challengeBytes)
    }

    /**
     * Convert BigInteger to exactly 32 bytes
     */
    private byte[] bigIntegerToBytes32(BigInteger value) {
        def bytes = value.toByteArray()

        if (bytes.length == 32) {
            return bytes
        } else if (bytes.length > 32) {
            // Remove leading sign byte
            return Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length)
        } else {
            // Pad with leading zeros
            def padded = new byte[32]
            System.arraycopy(bytes, 0, padded, 32 - bytes.length, bytes.length)
            return padded
        }
    }

    /**
     * Combine r and s into 64-byte signature
     */
    private byte[] combineSignature(BigInteger r, BigInteger s) {
        def rBytes = bigIntegerToBytes32(r)
        def sBytes = bigIntegerToBytes32(s)

        def signature = new byte[64]
        System.arraycopy(rBytes, 0, signature, 0, 32)
        System.arraycopy(sBytes, 0, signature, 32, 32)

        return signature
    }
}
