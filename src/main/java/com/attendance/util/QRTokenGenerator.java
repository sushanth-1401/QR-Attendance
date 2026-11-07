package com.attendance.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * Component for generating and hashing QR attendance tokens.
 */
@Component
public class QRTokenGenerator {

    private static final int TOKEN_BITS = 256;
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically secure 256-bit random token,
     * returned as a Base64URL-encoded string (no padding).
     *
     * @return Base64URL-encoded random token
     */
    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BITS / 8];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Computes the SHA-256 hash of the given token string and returns it as a hex string.
     *
     * @param token the token to hash
     * @return hex-encoded SHA-256 hash of the token
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }
}
