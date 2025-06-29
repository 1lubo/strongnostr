package com.onelubo.strongnostr.nostr;

/**
 * Bech32 encoding/decoding implementation for Nostr keys
 * Based on BIP 173 specification
 */
public class Bech32 {

    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final int[] GENERATOR = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};

    /**
     * Encode data as Bech32
     */
    public static String encode(String hrp, int[] data) {
        // Create checksum
        int[] checksum = createChecksum(hrp, data);

        // Build result string
        StringBuilder sb = new StringBuilder();
        sb.append(hrp);
        sb.append('1');

        // Add data
        for (int value : data) {
            sb.append(CHARSET.charAt(value));
        }

        // Add checksum
        for (int value : checksum) {
            sb.append(CHARSET.charAt(value));
        }

        return sb.toString();
    }

    /**
     * Decode Bech32 string
     */
    public static Bech32Data decode(String bech32) {
        if (bech32.length() < 8 || bech32.length() > 90) {
            throw new IllegalArgumentException("Invalid Bech32 length");
        }

        // Check case consistency
        if (!bech32.equals(bech32.toLowerCase()) && !bech32.equals(bech32.toUpperCase())) {
            throw new IllegalArgumentException("Mixed case Bech32 string");
        }

        bech32 = bech32.toLowerCase();

        int separatorPos = bech32.lastIndexOf('1');
        if (separatorPos < 1 || separatorPos + 7 > bech32.length()) {
            throw new IllegalArgumentException("Invalid separator position");
        }

        String hrp = bech32.substring(0, separatorPos);
        String data = bech32.substring(separatorPos + 1);

        // Decode data part
        int[] values = new int[data.length()];
        for (int i = 0; i < data.length(); i++) {
            int charIndex = CHARSET.indexOf(data.charAt(i));
            if (charIndex < 0) {
                throw new IllegalArgumentException("Invalid character in Bech32 string");
            }
            values[i] = charIndex;
        }

        // Verify checksum
        if (!verifyChecksum(hrp, values)) {
            throw new IllegalArgumentException("Invalid Bech32 checksum");
        }

        // Return data without checksum
        int[] dataValues = new int[values.length - 6];
        System.arraycopy(values, 0, dataValues, 0, dataValues.length);

        return new Bech32Data(hrp, dataValues);
    }

    /**
     * Create checksum for Bech32 encoding
     */
    private static int[] createChecksum(String hrp, int[] data) {
        // ✅ CORRECT: Need space for HRP + separator + data + 6 zeros
        int[] values = new int[hrp.length() * 2 + 1 + data.length + 6];

        int pos = 0;

        // HRP high bits
        for (int i = 0; i < hrp.length(); i++) {
            values[pos++] = hrp.charAt(i) >> 5;
        }

        // Separator
        values[pos++] = 0;

        // HRP low bits
        for (int i = 0; i < hrp.length(); i++) {
            values[pos++] = hrp.charAt(i) & 31;
        }

        // Data
        for (int datum : data) {
            values[pos++] = datum;
        }
        // Add 6 zeros for checksum
        for (int i = 0; i < 6; i++) {
            values[pos++] = 0;
        }

        // Now calculate polymod
        int polymod = polymod(values) ^ 1;

        // Extract checksum
        int[] checksum = new int[6];
        for (int i = 0; i < 6; i++) {
            checksum[i] = (polymod >> (5 * (5 - i))) & 31;
        }

        return checksum;
    }
    /**
     * Verify Bech32 checksum
     */
    private static boolean verifyChecksum(String hrp, int[] data) {
        // Create values array for polymod calculation
        int hrpLen = hrp.length();
        int[] values = new int[hrpLen * 2 + 1 + data.length];

        int pos = 0;

        // HRP high bits
        for (int i = 0; i < hrpLen; i++) {
            values[pos++] = hrp.charAt(i) >> 5;
        }

        // Separator
        values[pos++] = 0;

        // HRP low bits
        for (int i = 0; i < hrpLen; i++) {
            values[pos++] = hrp.charAt(i) & 31;
        }

        // Data (including checksum)
        for (int datum : data) {
            values[pos++] = datum;
        }

        return polymod(values) == 1;
    }

    /**
     * Polymod function for Bech32 checksum
     */
    private static int polymod(int[] values) {
        int chk = 1;
        for (int value : values) {
            int top = chk >> 25;
            chk = (chk & 0x1ffffff) << 5 ^ value;
            for (int i = 0; i < 5; i++) {
                chk ^= ((top >> i) & 1) != 0 ? GENERATOR[i] : 0;
            }
        }
        return chk;
    }

    public record Bech32Data(String hrp, int[] data) {
    }
}
