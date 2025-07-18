package com.onelubo.strongnostr.nostr;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import java.util.HexFormat;

@Component
public class NostrKeyManager {

    Logger logger = LoggerFactory.getLogger(NostrKeyManager.class);

    private final ECParameterSpec secp256k1Spec;
    private final ECDomainParameters domainParameters;
    private final SecureRandom secureRandom;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public NostrKeyManager() {
        this.secp256k1Spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        this.domainParameters = new ECDomainParameters(
                secp256k1Spec.getCurve(),
                secp256k1Spec.getG(),
                secp256k1Spec.getN(),
                secp256k1Spec.getH()
        );
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate new secp256k1 key pair for Nostr
     */
    public NostrKeyPair generateKeyPair() {
        try {
            ECKeyPairGenerator generator = new ECKeyPairGenerator();

            ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(domainParameters, secureRandom);

            generator.init(keyGenParams);
            AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();

            ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters) keyPair.getPrivate();
            BigInteger privateKeyInt = privateKeyParams.getD();
            String privateKeyHex = String.format("%064x", privateKeyInt);

            ECPublicKeyParameters publicKeyParams = (ECPublicKeyParameters) keyPair.getPublic();
            ECPoint publicKeyPoint = publicKeyParams.getQ();

            String publicKeyHex = extractXCoordinateHex(publicKeyPoint);

            String nostrPublicKey = hexToNpub(publicKeyHex);
            String nostrPrivateKey = hexToNsec(privateKeyHex);

            return new NostrKeyPair(nostrPublicKey, nostrPrivateKey, publicKeyHex, privateKeyHex);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Nostr key pair", e);
        }
    }

    /**
     * Derive public key from private key using secp256k1
     */
    public String derivePublicKeyFromPrivate(String privateKeyHex) {
        validateHexKey(privateKeyHex, 64, "Private key");

        try {
            BigInteger privateKeyInt = new BigInteger(privateKeyHex, 16);

            BigInteger n = domainParameters.getN();
            if (privateKeyInt.compareTo(BigInteger.ONE) < 0 || privateKeyInt.compareTo(n) >= 0) {
                throw new IllegalArgumentException("Private key out of valid range");
            }

            ECPoint publicKeyPoint = domainParameters.getG().multiply(privateKeyInt);

            logger.debug("DEBUG - Original public key point:\n  X: {}\n  Y: {}\n  Y is odd: {}", publicKeyPoint.normalize().getAffineXCoord().toBigInteger().toString(16), publicKeyPoint.normalize().getAffineYCoord().toBigInteger().toString(16), publicKeyPoint.normalize().getAffineYCoord().toBigInteger().testBit(0));

            String result = extractXCoordinateHex(publicKeyPoint);

            BigInteger resultX = new BigInteger(result, 16);
            byte[] evenPoint = new byte[33];
            evenPoint[0] = 0x02;
            System.arraycopy(resultX.toByteArray(), 0, evenPoint, 33 - resultX.toByteArray().length, resultX.toByteArray().length);
            ECPoint reconstructed = domainParameters.getCurve().decodePoint(evenPoint).normalize();

            logger.debug("DEBUG - Reconstructed public key point:\n  X: {}\n  Y: {}\n  Y is odd: {}", reconstructed.getAffineXCoord().toBigInteger().toString(16), reconstructed.getAffineYCoord().toBigInteger().toString(16), reconstructed.getAffineYCoord().toBigInteger().testBit(0));

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to derive public key", e);
        }
    }

    /**
     * Convert npub to hex format
     */
    public String npubToHex(String npub) {
        if (npub == null) {
            throw new IllegalArgumentException("npub cannot be null");
        }

        if (npub.startsWith("npub1")) {
            Bech32Result decoded = decodeBech32(npub);
            if (!"npub".equals(decoded.hrp())) {
                throw new IllegalArgumentException("Invalid npub format");
            }
            return HexFormat.of().formatHex(decoded.data());
        } else if (isValidHex(npub) && npub.length() == 64) {
            return npub.toLowerCase();
        } else {
            throw new IllegalArgumentException("Invalid public key format");
        }
    }

    /**
     * Convert hex to npub format
     */
    public String hexToNpub(String hex) {
        validateHexKey(hex, 64, "Public key");

        byte [] keyBytes = HexFormat.of().parseHex(hex);
        return encodeBech32("npub", keyBytes);
    }

    /**
     * Convert nsec to hex format
     */
    public String nsecToHex(String nsec) {
        if (nsec == null) {
            throw new IllegalArgumentException("nsec cannot be null");
        }

        if (nsec.startsWith("nsec1")) {
            Bech32Result decoded = decodeBech32(nsec);
            if (!"nsec".equals(decoded.hrp())) {
                throw new IllegalArgumentException("Invalid nsec format");
            }
            return HexFormat.of().formatHex(decoded.data);
        } else if (isValidHex(nsec) && nsec.length() == 64) {
            return nsec.toLowerCase();
        } else {
            throw new IllegalArgumentException("Invalid private key format");
        }
    }

    /**
     * Convert hex to nsec format
     */
    public String hexToNsec(String hex) {
        validateHexKey(hex, 64, "Private key");

        byte [] keyBytes = HexFormat.of().parseHex(hex);
        return encodeBech32("nsec", keyBytes);
    }

    /**
     * Validate Nostr public key format (npub or hex)
     */
    public boolean isValidNostrPublicKey(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            return false;
        }

        try {
            if (publicKey.startsWith("npub1")) {
                Bech32Result decoded = decodeBech32(publicKey);
                return "npub".equals(decoded.hrp()) && decoded.data().length == 32;
            } else {
                return isValidHex(publicKey) && publicKey.length() == 64;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate Nostr private key format (nsec or hex)
     */
    public boolean isValidNostrPrivateKey(String privateKey) {
        if (privateKey == null || privateKey.trim().isEmpty()) {
            return false;
        }

        try {
            if (privateKey.startsWith("nsec1")) {
                Bech32Result decoded = decodeBech32(privateKey);
                return "nsec".equals(decoded.hrp()) && decoded.data().length == 32;
            } else {
                return isValidHex(privateKey) && privateKey.length() == 64;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Encode data as Bech32 with given human-readable part
     */
    public String encodeBech32(String hrp, byte[] data) {
        if (hrp == null || data == null) {
            throw new IllegalArgumentException("HRP and data cannot be null");
        }

        try {
            int[] convertedData = convertBits(data, 8, 5, true);

            return Bech32.encode(hrp, convertedData);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to encode Bech32", e);
        }
    }

    /**
     * Decode Bech32 string
     */
    public Bech32Result decodeBech32(String bech32) {
        if (bech32 == null || bech32.trim().isEmpty()) {
            throw new IllegalArgumentException("Bech32 string cannot be null or empty");
        }

        try {
            Bech32.Bech32Data decoded = Bech32.decode(bech32);

            int[] convertedData = convertBits(decoded.data(), 5, 8, false);
            byte[] dataBytes = new byte[convertedData.length];
            for (int i = 0; i < convertedData.length; i++) {
                dataBytes[i] = (byte) convertedData[i];
            }

            return new Bech32Result(decoded.hrp(), dataBytes);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode Bech32: " + e.getMessage(), e);
        }
    }

    /**
     * Convert between different bit groupings for Bech32
     */
    private int[] convertBits(byte[] data, int fromBits, int toBits, boolean pad) {
        int acc = 0;
        int bits = 0;
        int maxv = (1 << toBits) - 1;
        java.util.List<Integer> result = new java.util.ArrayList<>();

        for (byte b : data) {
            int value = b & 0xFF; // Convert to unsigned int

            acc = (acc << fromBits) | value;
            bits += fromBits;

            while (bits >= toBits) {
                bits -= toBits;
                result.add((acc >> bits) & maxv);
            }
        }

        if (pad) {
            if (bits > 0) {
                result.add((acc << (toBits - bits)) & maxv);
            }
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new IllegalArgumentException("Invalid padding in bit conversion");
        }

        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] convertBits(int[] data, int fromBits, int toBits, boolean pad) {
        int acc = 0;
        int bits = 0;
        int maxv = (1 << toBits) - 1;
        int maxacc = (1 << (fromBits + toBits - 1)) - 1;

        java.util.List<Integer> result = new java.util.ArrayList<>();

        for (int value : data) {
            if (value < 0 || (value >> fromBits) != 0) {
                throw new IllegalArgumentException("Invalid data for base conversion");
            }
            acc = ((acc << fromBits) | value) & maxacc;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                result.add((acc >> bits) & maxv);
            }
        }

        if (pad) {
            if (bits > 0) {
                result.add((acc << (toBits - bits)) & maxv);
            }
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new IllegalArgumentException("Invalid padding in base conversion");
        }

        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private void validateHexKey(String hex, int expectedLength, String keyType) {
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException(keyType + " must not be null or empty");
        }
        if (hex.length() != expectedLength) {
            throw new IllegalArgumentException(keyType + " must be " + expectedLength + " characters long");
        }
        if (!isValidHex(hex)) {
            throw new IllegalArgumentException(keyType + " must be a valid hexadecimal string");
        }
    }

    private boolean isValidHex(String hex) {
        return hex != null && hex.matches("^[0-9a-fA-F]+$") && hex.length() % 2 == 0;
    }

    /**
     * Extract 32-byte X coordinate from EC point (consistent method)
     */
    private String extractXCoordinateHex(ECPoint point) {
        ECPoint normalizedPoint = point.normalize();

        BigInteger y = normalizedPoint.getAffineYCoord().toBigInteger();
        if (y.testBit(0)) {
            normalizedPoint = normalizedPoint.negate().normalize();
        }

        byte[] xCoordBytes = normalizedPoint.getAffineXCoord().toBigInteger().toByteArray();

        if (xCoordBytes.length == 33 && xCoordBytes[0] == 0) {
            byte[] temp = new byte[32];
            System.arraycopy(xCoordBytes, 1, temp, 0, 32);
            xCoordBytes = temp;
        } else if (xCoordBytes.length < 32) {
            byte[] temp = new byte[32];
            System.arraycopy(xCoordBytes, 0, temp, 32 - xCoordBytes.length, xCoordBytes.length);
            xCoordBytes = temp;
        }

        return HexFormat.of().formatHex(xCoordBytes);
    }

    public static class NostrKeyPair {
        private final String nsec;
        private final String npub;
        private final String nPubHex;
        private final String nSecHex;


        public NostrKeyPair(String npub, String nsec, String nPubHex, String nSecHex) {
            this.nsec = nsec;
            this.npub = npub;
            this.nPubHex = nPubHex;
            this.nSecHex = nSecHex;
        }

        public String getnPub() {
            return npub;
        }

        public String getnPubHex() {
            return nPubHex;
        }

        public String getnSecHex() {
            return nSecHex;
        }

        @Override
        public String toString() {
            return "NostrKeyPair{" +
                    "publicKey='" + npub.substring(0, 12) + "...'" +
                    ", publicKeyHex='" + nPubHex.substring(0, 8) + "...'" +
                    '}';
        }
    }

    public record Bech32Result(String hrp, byte[] data) {
    }
}