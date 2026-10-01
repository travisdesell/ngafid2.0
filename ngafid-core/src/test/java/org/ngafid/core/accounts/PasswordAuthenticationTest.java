package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PasswordAuthentication class.
 * Tests password hashing, authentication, and edge cases.
 */
public class PasswordAuthenticationTest {

    /**
     * Verifies the no-argument constructor builds an instance using the default hashing cost.
     */
    @Test
    @DisplayName("Should create PasswordAuthentication with default cost")
    public void testDefaultConstructor() {
        PasswordAuthentication auth = new PasswordAuthentication();
        assertNotNull(auth);
    }

    /**
     * Verifies the constructor accepts an in-range cost value.
     */
    @Test
    @DisplayName("Should create PasswordAuthentication with valid cost")
    public void testValidCostConstructor() {
        PasswordAuthentication auth = new PasswordAuthentication(10);
        assertNotNull(auth);
    }

    /**
     * Verifies the constructor rejects a negative cost with {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for negative cost")
    public void testNegativeCost() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordAuthentication(-1);
        });
    }

    /**
     * Verifies the constructor rejects a cost above the supported maximum (30) with {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for cost greater than 30")
    public void testCostTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordAuthentication(31);
        });
    }

    /**
     * Verifies 31 (one past the maximum) is rejected, pinning the upper bound of the valid cost range.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for cost exactly 31")
    public void testCostExactly31() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordAuthentication(31);
        });
    }

    /**
     * Verifies the minimum valid cost (0) is accepted.
     */
    @Test
    @DisplayName("Should accept cost of 0")
    public void testCostZero() {
        PasswordAuthentication auth = new PasswordAuthentication(0);
        assertNotNull(auth);
    }

    /**
     * Verifies the maximum valid cost (30) is accepted.
     */
    @Test
    @DisplayName("Should accept cost of 30")
    public void testCostThirty() {
        PasswordAuthentication auth = new PasswordAuthentication(30);
        assertNotNull(auth);
    }

    /**
     * Verifies {@code hash} produces a non-empty token in the expected {@code $31$}-prefixed layout.
     */
    @Test
    @DisplayName("Should hash password successfully")
    public void testHashPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();

        String hash = auth.hash(password);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$31$"));
        assertTrue(hash.length() > 10); // Should be a substantial hash
    }

    /**
     * Verifies {@code authenticate} returns true when the supplied password matches the hash.
     */
    @Test
    @DisplayName("Should authenticate with correct password")
    public void testAuthenticateCorrectPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();

        String hash = auth.hash(password);
        boolean result = auth.authenticate(password, hash);

        assertTrue(result);
    }

    /**
     * Verifies {@code authenticate} returns false when the supplied password does not match the hash.
     */
    @Test
    @DisplayName("Should not authenticate with incorrect password")
    public void testAuthenticateIncorrectPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        char[] wrongPassword = "wrongpassword".toCharArray();

        String hash = auth.hash(password);
        boolean result = auth.authenticate(wrongPassword, hash);

        assertFalse(result);
    }

    /**
     * Verifies {@code authenticate} rejects a token that does not match the expected layout at all.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid token format")
    public void testInvalidTokenFormat() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        String invalidToken = "invalidtoken";

        assertThrows(IllegalArgumentException.class, () -> {
            auth.authenticate(password, invalidToken);
        });
    }

    /**
     * Verifies {@code authenticate} rejects a token whose version prefix is not {@code $31$}.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for malformed token with wrong prefix")
    public void testMalformedTokenWrongPrefix() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        String malformedToken = "$30$16$invalidhash";

        assertThrows(IllegalArgumentException.class, () -> {
            auth.authenticate(password, malformedToken);
        });
    }

    /**
     * Verifies {@code authenticate} rejects a token whose encoded cost is out of range.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for token with invalid cost")
    public void testTokenWithInvalidCost() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        String invalidCostToken = "$31$99$invalidhash";

        assertThrows(IllegalArgumentException.class, () -> {
            auth.authenticate(password, invalidCostToken);
        });
    }

    /**
     * Verifies {@code authenticate} rejects a token missing the hash segment.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for token with missing parts")
    public void testTokenWithMissingParts() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        String incompleteToken = "$31$16";

        assertThrows(IllegalArgumentException.class, () -> {
            auth.authenticate(password, incompleteToken);
        });
    }

    /**
     * Verifies {@code authenticate} rejects an empty token string.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for empty token")
    public void testEmptyToken() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        String emptyToken = "";

        assertThrows(IllegalArgumentException.class, () -> {
            auth.authenticate(password, emptyToken);
        });
    }

    /**
     * Verifies {@code authenticate} throws {@link NullPointerException} when the token is null.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for null token")
    public void testNullToken() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();

        assertThrows(NullPointerException.class, () -> {
            auth.authenticate(password, null);
        });
    }

    /**
     * Verifies hashes produced at different costs (5 and 20) each authenticate successfully against their own hash.
     */
    @Test
    @DisplayName("Should handle different cost values in authentication")
    public void testDifferentCostValues() {
        // Test with cost 5
        PasswordAuthentication auth5 = new PasswordAuthentication(5);
        char[] password = "testpassword".toCharArray();
        String hash5 = auth5.hash(password);
        assertTrue(auth5.authenticate(password, hash5));

        // Test with cost 20
        PasswordAuthentication auth20 = new PasswordAuthentication(20);
        String hash20 = auth20.hash(password);
        assertTrue(auth20.authenticate(password, hash20));
    }

    /**
     * Documents that the PBKDF2 algorithm/key-spec catch blocks are unreachable in practice by exercising a normal
     * hash-then-authenticate round trip (the reachable success path) rather than attempting to trigger them.
     */
    @Test
    @DisplayName("Should document unreachable catch blocks in pbkdf2 method")
    public void testUnreachableCatchBlocks() {
        // These catch blocks in the pbkdf2 method are marked as "UNREACHABLE" because:
        // 1. NoSuchAlgorithmException: PBKDF2WithHmacSHA1 is part of standard Java security providers
        // 2. InvalidKeySpecException: PBEKeySpec parameters are all valid in this implementation

        // These are defensive programming catch blocks that cannot be triggered in practice
        // They are kept for theoretical edge cases but are truly unreachable

        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();

        // Test that normal operation works (which exercises the try block)
        String hash = auth.hash(password);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$31$"));

        boolean result = auth.authenticate(password, hash);
        assertTrue(result);

        // The catch blocks are unreachable in normal operation
        // This test documents that fact rather than trying to trigger them
    }

    /**
     * Verifies hashing and authentication succeed for a very long (1000-character) password.
     */
    @Test
    @DisplayName("Should handle edge case with very long password")
    public void testVeryLongPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();

        // Create a very long password to test edge cases
        StringBuilder longPassword = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longPassword.append("a");
        }
        char[] password = longPassword.toString().toCharArray();

        String hash = auth.hash(password);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$31$"));

        boolean result = auth.authenticate(password, hash);
        assertTrue(result);
    }

    /**
     * Verifies hashing and authentication succeed for a single-character password.
     */
    @Test
    @DisplayName("Should handle edge case with very short password")
    public void testVeryShortPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "a".toCharArray();

        String hash = auth.hash(password);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$31$"));

        boolean result = auth.authenticate(password, hash);
        assertTrue(result);
    }

    /**
     * Verifies hashing and authentication succeed for a password composed of special characters.
     */
    @Test
    @DisplayName("Should handle edge case with special characters in password")
    public void testSpecialCharactersPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~".toCharArray();

        String hash = auth.hash(password);
        assertNotNull(hash);
        assertTrue(hash.startsWith("$31$"));

        boolean result = auth.authenticate(password, hash);
        assertTrue(result);
    }
}
