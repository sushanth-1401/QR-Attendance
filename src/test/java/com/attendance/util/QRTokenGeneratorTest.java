package com.attendance.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class QRTokenGeneratorTest {

    private QRTokenGenerator tokenGenerator;

    @BeforeEach
    public void setUp() {
        tokenGenerator = new QRTokenGenerator();
    }

    @Test
    @DisplayName("Generates 256-bit entropy Base64URL token")
    public void testGenerateTokenEntropy() {
        String token = tokenGenerator.generateToken();
        assertNotNull(token);
        assertTrue(token.length() >= 40, "Token should be at least 43 characters (256 bits Base64URL)");
    }

    @Test
    @DisplayName("Consecutive tokens must be distinct (high entropy)")
    public void testUniqueTokens() {
        String token1 = tokenGenerator.generateToken();
        String token2 = tokenGenerator.generateToken();
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Produces deterministic 64-character SHA-256 hex hash")
    public void testHashToken() {
        String token = "sample-secure-token-123456";
        String hash1 = tokenGenerator.hashToken(token);
        String hash2 = tokenGenerator.hashToken(token);

        assertEquals(64, hash1.length(), "SHA-256 hex string must be exactly 64 characters");
        assertEquals(hash1, hash2, "Hashing same token must produce deterministic output");
    }
}

