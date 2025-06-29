package com.onelubo.strongnostr.nostr;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@Component
public class NostrEventVerifier {

    Logger logger = LoggerFactory.getLogger(NostrEventVerifier.class);

    private final NostrKeyManager nostrKeyManager;
    private final ECDomainParameters domainParameters;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public NostrEventVerifier(NostrKeyManager nostrKeyManager) {
        this.nostrKeyManager = nostrKeyManager;

        // Initialize BouncyCastle secp256k1 parameters
        ECParameterSpec secp256k1Spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        this.domainParameters = new ECDomainParameters(
                secp256k1Spec.getCurve(),
                secp256k1Spec.getG(),
                secp256k1Spec.getN(),
                secp256k1Spec.getH()
        );
    }

    /**
     * Verify a complete Nostr event signature
     */
    public boolean verifyEventSignature(NostrEvent nostrEvent) {
        try {
            if (!isValidEventStructure(nostrEvent)) {
                return false;
            }

            String computedId = computeEventId(nostrEvent);

            logger.debug("=== EVENT VERIFICATION DEBUG ===\nEvent ID (hex): {}\nEvent ID (bytes): {}\nSignature: {}\nPublic Key: {}", computedId, Arrays.toString(HexFormat.of().parseHex(computedId)), nostrEvent.getSignature(), nostrKeyManager.npubToHex(nostrEvent.getnPub()));

            if (!computedId.equals(nostrEvent.getId())) {
                logger.info("❌ Event ID mismatch!");
                return false;
            }

            String publicKeyHex = nostrKeyManager.npubToHex(nostrEvent.getnPub());

            return verifySignatureFromHash(computedId, nostrEvent.getSignature(), publicKeyHex);

        } catch (Exception e) {
            logger.error("Event verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compute Nostr event ID according to NIP-01
     */
    public String computeEventId(NostrEvent nostrEvent) {
        try {
            String serialized = serializeEventForId(nostrEvent);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute event ID", e);
        }
    }

    /**
     * Serialize event for ID computation according to NIP-01
     */
    public static String serializeEventForId(NostrEvent nostrEvent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[0,\"");
        sb.append(nostrEvent.getnPub());
        sb.append("\",");
        sb.append(nostrEvent.getCreatedAt());
        sb.append(",");
        sb.append(nostrEvent.getKind());
        sb.append(",");
        sb.append(serializeTags(nostrEvent.getTags()));
        sb.append(",\"");
        sb.append(escapeString(nostrEvent.getContent()));
        sb.append("\"]");
        return sb.toString();
    }

    /**
     * Verify signature from a pre-computed hash (for Nostr event IDs)
     */
    public boolean verifySignatureFromHash(String messageHashHex, String signatureHex, String publicKeyHex) {
        try {
            if (messageHashHex == null || signatureHex == null || publicKeyHex == null) {
                return false;
            }

            if (!isValidHexString(messageHashHex) || messageHashHex.length() != 64) {
                return false;
            }

            if (!isValidHexString(signatureHex) || signatureHex.length() != 128) {
                return false;
            }

            if (!isValidHexString(publicKeyHex) || publicKeyHex.length() != 64) {
                return false;
            }

            byte[] messageHash = HexFormat.of().parseHex(messageHashHex);
            byte[] signatureBytes = HexFormat.of().parseHex(signatureHex);

            BigInteger r = new BigInteger(1, Arrays.copyOfRange(signatureBytes, 0, 32));
            BigInteger s = new BigInteger(1, Arrays.copyOfRange(signatureBytes, 32, 64));
            BigInteger pubX = new BigInteger(1, HexFormat.of().parseHex(publicKeyHex));

            if (!isValidSchnorrComponent(r) || !isValidSchnorrComponent(s)) {
                return false;
            }

            return verifySchnorrSignatureFromHash(messageHash, r, s, pubX);

        } catch (Exception e) {
            logger.error("Hash signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifySchnorrSignatureFromMessage(byte[] message, BigInteger r, BigInteger s, BigInteger pubX) {
        try {
            ECPoint publicKeyPoint = reconstructPublicKeyPoint(pubX);
            if (publicKeyPoint == null) {
                return false;
            }

            BigInteger e = calculateChallengeFromMessage(r, pubX, message);

            ECPoint sG = domainParameters.getG().multiply(s).normalize();

            for (byte yPrefix : new byte[]{0x02, 0x03}) {
                try {
                    byte[] compressedPoint = concatenateBytes(new byte[]{yPrefix}, bigIntegerToBytes32(r));
                    ECPoint rPoint = domainParameters.getCurve().decodePoint(compressedPoint).normalize();

                    ECPoint eP = publicKeyPoint.multiply(e).normalize();
                    ECPoint sum = rPoint.add(eP).normalize();

                    if (sG.getAffineXCoord().toBigInteger().equals(sum.getAffineXCoord().toBigInteger())) {
                        return true;
                    }
                } catch (Exception ex) {
                    logger.debug("  Failed with Y prefix 0x{}: {}", String.format("%02x", yPrefix), ex.getMessage());
                }
            }

            logger.debug("❌ BIP340 VERIFICATION FAILED\nNo R point Y coordinate produced matching X coordinates");
            return false;

        } catch (Exception e) {
            logger.error("Message verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Also update the hash verification method to use the same logic
     */
    private boolean verifySchnorrSignatureFromHash(byte[] messageHash, BigInteger r, BigInteger s, BigInteger pubX) {
        try {
            ECPoint publicKeyPoint = reconstructPublicKeyPoint(pubX);
            if (publicKeyPoint == null) {
                return false;
            }

            BigInteger e = calculateChallengeFromHash(r, pubX, messageHash);
            ECPoint sG = domainParameters.getG().multiply(s).normalize();

            for (byte yPrefix : new byte[]{0x02, 0x03}) {
                try {
                    byte[] compressedPoint = concatenateBytes(new byte[]{yPrefix}, bigIntegerToBytes32(r));
                    ECPoint rPoint = domainParameters.getCurve().decodePoint(compressedPoint).normalize();

                    ECPoint eP = publicKeyPoint.multiply(e).normalize();
                    ECPoint sum = rPoint.add(eP).normalize();

                    if (sG.getAffineXCoord().toBigInteger().equals(sum.getAffineXCoord().toBigInteger())) {
                        return true;
                    }
                } catch (Exception ex) {
                    // Continue to next Y coordinate
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("Hash verification error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reconstruct public key point from x-coordinate using BouncyCastle
     */
    private ECPoint reconstructPublicKeyPoint(BigInteger x) {
        try {
            BigInteger p = domainParameters.getCurve().getField().getCharacteristic();
            if (x.compareTo(p) >= 0) {
                return null;
            }

            byte[] compressedPoint = concatenateBytes(new byte[]{0x02}, bigIntegerToBytes32(x));
            return domainParameters.getCurve().decodePoint(compressedPoint).normalize();

        } catch (Exception e) {
            logger.error("Public key reconstruction error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate BIP340 challenge for pre-hashed messages: e = H(r || P.x || messageHash)
     */
    private BigInteger calculateChallengeFromHash(BigInteger r, BigInteger pubX, byte[] messageHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bigIntegerToBytes32(r));
            digest.update(bigIntegerToBytes32(pubX));
            digest.update(messageHash); // messageHash is already 32 bytes
            return new BigInteger(1, digest.digest());
        } catch (Exception e) {
            logger.error("Challenge calculation error: {}", e.getMessage());
            return BigInteger.ZERO;
        }
    }

    /**
     * Calculate BIP340 challenge for raw messages: e = H(r || P.x || message)
     */
    private BigInteger calculateChallengeFromMessage(BigInteger r, BigInteger pubX, byte[] message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] rBytes = bigIntegerToBytes32(r);
            byte[] pubXBytes = bigIntegerToBytes32(pubX);

            logger.debug("CHALLENGE CALCULATION DEBUG:\n  r: {}\n  pubX: {}\n  message: '{}'\n  message bytes: {}",
                    HexFormat.of().formatHex(rBytes),
                    HexFormat.of().formatHex(pubXBytes),
                    new String(message, StandardCharsets.UTF_8),
                    HexFormat.of().formatHex(message));


            digest.update(rBytes);
            digest.update(pubXBytes);
            digest.update(message);

            byte[] challengeBytes = digest.digest();
            BigInteger challenge = new BigInteger(1, challengeBytes);

            logger.debug("  calculated challenge: {}", challenge.toString(16));

            return challenge;
        } catch (Exception e) {
            logger.error("Challenge calculation error: {}", e.getMessage());
            return BigInteger.ZERO;
        }
    }

    /**
     * Validate Schnorr signature component (r or s)
     */
    private boolean isValidSchnorrComponent(BigInteger component) {
        BigInteger curveOrder = domainParameters.getN();
        return !component.equals(BigInteger.ZERO) && component.compareTo(curveOrder) < 0;
    }

    /**
     * Check if event structure is valid
     */
    private boolean isValidEventStructure(NostrEvent nostrEvent) {
        return nostrEvent != null &&
                nostrEvent.getId() != null && !nostrEvent.getId().trim().isEmpty() &&
                nostrEvent.getnPub() != null && !nostrEvent.getnPub().trim().isEmpty() &&
                nostrEvent.getSignature() != null && !nostrEvent.getSignature().trim().isEmpty() &&
                nostrEvent.getContent() != null &&
                nostrEvent.getCreatedAt() > 0;
    }

    /**
     * Serialize event tags according to NIP-01
     */
    private static String serializeTags(List<List<String>> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            List<String> tag = tags.get(i);
            sb.append("[");

            for (int j = 0; j < tag.size(); j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeString(tag.get(j))).append("\"");
            }
            sb.append("]");
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * Escape string for JSON serialization
     */
    private static String escapeString(String str) {
        if (str == null) {
            return "";
        }

        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Validate hex string format
     */
    private boolean isValidHexString(String hex) {
        return hex != null && hex.matches("^[0-9a-fA-F]+$") && hex.length() % 2 == 0;
    }

    /**
     * Convert BigInteger to exactly 32 bytes with proper padding/truncation
     */
    private byte[] bigIntegerToBytes32(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length == 32) {
            return bytes;
        } else if (bytes.length > 32) {
            return Arrays.copyOfRange(bytes, bytes.length - 32, bytes.length);
        } else {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 32 - bytes.length, bytes.length);
            return padded;
        }
    }

    /**
     * Concatenate two byte arrays
     */
    private byte[] concatenateBytes(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
