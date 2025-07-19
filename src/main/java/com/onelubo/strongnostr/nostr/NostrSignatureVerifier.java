package com.onelubo.strongnostr.nostr;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NostrSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(NostrSignatureVerifier.class.getName());

    // secp256k1 curve constants
    private static final BigInteger CURVE_ORDER = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    private static final BigInteger FIELD_SIZE = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    private static final BigInteger CURVE_B = BigInteger.valueOf(7); // y² = x³ + 7


    /**
     * Verify Schnorr signature according to BIP-340
     * @param pubkeyHex 32-byte public key (x-coordinate only)
     * @param messageHex Message that was signed (as hex)
     * @param signatureHex 64-byte signature (r || s)
     * @return true if signature is valid
     */
    public static boolean verifySchnorrSignature(String pubkeyHex, String messageHex, String signatureHex) {
        try {
            // Parse inputs
            byte[] pkBytes = hexToBytes(pubkeyHex);
            byte[] msgBytes = hexToBytes(messageHex);
            byte[] sigBytes = hexToBytes(signatureHex);

            // Validate input lengths
            if (pkBytes.length != 32) {
                logger.debug("Invalid public key length: {}", pkBytes.length);
                throw new IllegalArgumentException("Public key must be 32 bytes");
            }
            if (sigBytes.length != 64) {
                logger.debug("Invalid signature length: {}", sigBytes.length);
                throw new IllegalArgumentException("Signature must be 64 bytes");
            }

            // Extract r and s from signature
            byte[] rBytes = Arrays.copyOfRange(sigBytes, 0, 32);
            byte[] sBytes = Arrays.copyOfRange(sigBytes, 32, 64);

            BigInteger r = bytesToBigInteger(rBytes);
            BigInteger s = bytesToBigInteger(sBytes);

            // Validate r and s ranges
            if (r.compareTo(FIELD_SIZE) >= 0) {
                return false; // r >= p
            }
            if (s.compareTo(CURVE_ORDER) >= 0) {
                return false; // s >= n
            }

            // Lift public key x-coordinate to point P
            BigInteger pkX = bytesToBigInteger(pkBytes);
            Point P = liftX(pkX);
            if (P == null) {
                return false; // Invalid public key
            }

            // Calculate challenge: e = int(hash_BIP0340/challenge(bytes(r) || bytes(P) || m)) mod n
            byte[] challengeInput = new byte[32 + 32 + msgBytes.length];
            System.arraycopy(rBytes, 0, challengeInput, 0, 32);
            System.arraycopy(pkBytes, 0, challengeInput, 32, 32);
            System.arraycopy(msgBytes, 0, challengeInput, 64, msgBytes.length);

            byte[] challengeHash = taggedHash("BIP0340/challenge", challengeInput);
            BigInteger e = bytesToBigInteger(challengeHash).mod(CURVE_ORDER);

            // Calculate R = s⋅G - e⋅P
            Point sG = multiplyGenerator(s);
            Point eP = multiplyPoint(P, e);
            Point R = addPoints(sG, Point.of(eP.x, FIELD_SIZE.subtract(eP.y))); // subtract eP

            // Verification checks
            if (R.isInfinity) {
                return false;
            }

            if (!R.hasEvenY()) {
                return false;
            }

            return R.x.equals(r);

        } catch (Exception e) {
            // Any exception during verification means invalid signature
            return false;
        }
    }

    /**
     * Verify Schnorr signature with byte arrays
     * @param pubkeyBytes 32-byte public key
     * @param messageBytes Message that was signed
     * @param signatureBytes 64-byte signature
     * @return true if signature is valid
     */
    public static boolean verifySchnorrSignature(byte[] pubkeyBytes, byte[] messageBytes, byte[] signatureBytes) {
        return verifySchnorrSignature(
                bytesToHex(pubkeyBytes),
                bytesToHex(messageBytes),
                bytesToHex(signatureBytes)
                                     );
    }


    /**
     * Tagged hash function as specified in BIP-340
     * @param tag The tag name
     * @param data Data to hash
     * @return 32-byte hash
     */
    private static byte[] taggedHash(String tag, byte[] data) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] tagBytes = tag.getBytes(StandardCharsets.UTF_8);
            byte[] tagHash = sha256.digest(tagBytes);

            sha256.reset();
            sha256.update(tagHash);
            sha256.update(tagHash);
            sha256.update(data);

            return sha256.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Convert hex string to byte array
     * @param hex Hex string (with or without 0x prefix)
     * @return byte array
     */
    private static byte[] hexToBytes(String hex) {
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    /**
     * Convert byte array to hex string
     * @param bytes byte array
     * @return hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Convert 32-byte array to BigInteger (big-endian, unsigned)
     * @param bytes 32-byte array
     * @return BigInteger
     */
    private static BigInteger bytesToBigInteger(byte[] bytes) {
        if (bytes.length != 32) {
            throw new IllegalArgumentException("Expected 32 bytes, got " + bytes.length);
        }
        return new BigInteger(1, bytes); // 1 = positive sign
    }

    /**
     * Convert BigInteger to 32-byte array (big-endian)
     * @param num BigInteger
     * @return 32-byte array
     */
    private static byte[] bigIntegerToBytes(BigInteger num) {
        byte[] bytes = num.toByteArray();

        // Handle cases where toByteArray() returns more or fewer than 32 bytes
        if (bytes.length == 32) {
            return bytes;
        } else if (bytes.length == 33 && bytes[0] == 0) {
            // Remove extra sign byte
            return Arrays.copyOfRange(bytes, 1, 33);
        } else if (bytes.length < 32) {
            // Pad with leading zeros
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 32 - bytes.length, bytes.length);
            return padded;
        } else {
            throw new IllegalArgumentException("BigInteger too large for 32 bytes");
        }
    }

    /**
     * Lift x-coordinate to point with even y-coordinate
     * @param x x-coordinate
     * @return Point object or null if invalid
     */
    private static Point liftX(BigInteger x) {
        if (x.compareTo(FIELD_SIZE) >= 0) {
            return null;
        }

        // Calculate y² = x³ + 7 (secp256k1 curve equation)
        BigInteger c = x.modPow(BigInteger.valueOf(3), FIELD_SIZE)
                        .add(CURVE_B)
                        .mod(FIELD_SIZE);

        // Calculate y = c^((p+1)/4) mod p
        BigInteger y = c.modPow(FIELD_SIZE.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), FIELD_SIZE);

        // Verify that y² ≡ c (mod p)
        if (!y.modPow(BigInteger.valueOf(2), FIELD_SIZE).equals(c)) {
            return null;
        }

        // Return point with even y-coordinate
        BigInteger evenY = y.remainder(BigInteger.valueOf(2)).equals(BigInteger.ZERO)
                ? y
                : FIELD_SIZE.subtract(y);

        return Point.of(x, evenY);
    }

    /**
     * Point multiplication: k * G (where G is the generator point)
     * Using secp256k1 generator point
     */
    private static Point multiplyGenerator(BigInteger k) {
        // secp256k1 generator point coordinates
        BigInteger gx = new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
        BigInteger gy = new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);

        return multiplyPoint(Point.of(gx, gy), k);
    }

    /**
     * Point multiplication using double-and-add algorithm
     * @param point The point to multiply
     * @param scalar The scalar multiplier
     * @return k * point
     */
    private static Point multiplyPoint(Point point, BigInteger scalar) {
        if (scalar.equals(BigInteger.ZERO) || point.isInfinity) {
            return Point.infinity();
        }

        if (scalar.equals(BigInteger.ONE)) {
            return point;
        }

        Point result = Point.infinity();
        Point addend = point;

        while (scalar.compareTo(BigInteger.ZERO) > 0) {
            if (scalar.testBit(0)) { // if scalar is odd
                result = addPoints(result, addend);
            }
            addend = addPoints(addend, addend); // double the point
            scalar = scalar.shiftRight(1); // divide scalar by 2
        }

        return result;
    }

    /**
     * Point addition on secp256k1 curve
     * @param p1 First point
     * @param p2 Second point
     * @return p1 + p2
     */
    private static Point addPoints(Point p1, Point p2) {
        if (p1.isInfinity) return p2;
        if (p2.isInfinity) return p1;

        BigInteger x1 = p1.x, y1 = p1.y;
        BigInteger x2 = p2.x, y2 = p2.y;

        if (x1.equals(x2)) {
            if (y1.equals(y2)) {
                // Point doubling
                BigInteger s = x1.modPow(BigInteger.valueOf(2), FIELD_SIZE)
                                 .multiply(BigInteger.valueOf(3))
                                 .multiply(y1.multiply(BigInteger.valueOf(2)).modInverse(FIELD_SIZE))
                                 .mod(FIELD_SIZE);

                BigInteger x3 = s.modPow(BigInteger.valueOf(2), FIELD_SIZE)
                                 .subtract(x1.multiply(BigInteger.valueOf(2)))
                                 .mod(FIELD_SIZE);

                BigInteger y3 = s.multiply(x1.subtract(x3))
                                 .subtract(y1)
                                 .mod(FIELD_SIZE);

                return Point.of(x3, y3);
            } else {
                // Points are inverses
                return Point.infinity();
            }
        } else {
            // Point addition
            BigInteger s = y2.subtract(y1)
                             .multiply(x2.subtract(x1).modInverse(FIELD_SIZE))
                             .mod(FIELD_SIZE);

            BigInteger x3 = s.modPow(BigInteger.valueOf(2), FIELD_SIZE)
                             .subtract(x1)
                             .subtract(x2)
                             .mod(FIELD_SIZE);

            BigInteger y3 = s.multiply(x1.subtract(x3))
                             .subtract(y1)
                             .mod(FIELD_SIZE);

            return Point.of(x3, y3);
        }
    }

    /**
         * Point on secp256k1 curve
         */
        private record Point(BigInteger x, BigInteger y, boolean isInfinity) {

        static Point infinity() {
                return new Point(null, null, true);
            }

            static Point of(BigInteger x, BigInteger y) {
                return new Point(x, y, false);
            }

            boolean hasEvenY() {
                return !isInfinity && y.remainder(BigInteger.valueOf(2)).equals(BigInteger.ZERO);
            }
        }
}
