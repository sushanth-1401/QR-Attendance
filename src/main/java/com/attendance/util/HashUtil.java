package com.attendance.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for cryptographic hashing and secure random generation.
 */
public final class HashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HashUtil() {
        // Utility class — prevent instantiation
    }

    /**
     * Computes the SHA-256 hash of the input string and returns it as a hex-encoded string.
     *
     * @param input the string to hash
     * @return hex-encoded SHA-256 hash
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a cryptographically secure random value of the specified bit length.
     *
     * @param bits the number of random bits to generate (must be a multiple of 8)
     * @return Base64URL-encoded random bytes (no padding)
     */
    public static String generateSecureRandom(int bits) {
        if (bits <= 0 || bits % 8 != 0) {
            throw new IllegalArgumentException("Bits must be a positive multiple of 8, got: " + bits);
        }
        byte[] bytes = new byte[bits / 8];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Converts a byte array to a lowercase hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}
