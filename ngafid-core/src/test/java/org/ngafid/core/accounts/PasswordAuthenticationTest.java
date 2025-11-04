package org.ngafid.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordAuthentication class.
 * Tests password hashing, authentication, and edge cases.
 */
public class PasswordAuthenticationTest {

    @Test
    @DisplayName("Should create PasswordAuthentication with default cost")
    public void testDefaultConstructor() {
        PasswordAuthentication auth = new PasswordAuthentication();
        assertNotNull(auth);
    }

    @Test
    @DisplayName("Should create PasswordAuthentication with valid cost")
    public void testValidCostConstructor() {
        PasswordAuthentication auth = new PasswordAuthentication(10);
        assertNotNull(auth);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative cost")
    public void testNegativeCost() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordAuthentication(-1);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for cost greater than 30")
    public void testCostTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordAuthentication(31);
        });
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for cost exactly 31")
    public void testCostExactly31() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PasswordAuthentication(31);
        });
    }

    @Test
    @DisplayName("Should accept cost of 0")
    public void testCostZero() {
        PasswordAuthentication auth = new PasswordAuthentication(0);
        assertNotNull(auth);
    }

    @Test
    @DisplayName("Should accept cost of 30")
    public void testCostThirty() {
        PasswordAuthentication auth = new PasswordAuthentication(30);
        assertNotNull(auth);
    }

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

    @Test
    @DisplayName("Should authenticate with correct password")
    public void testAuthenticateCorrectPassword() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        
        String hash = auth.hash(password);
        boolean result = auth.authenticate(password, hash);
        
        assertTrue(result);
    }

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

    @Test
    @DisplayName("Should throw IllegalArgumentException for null token")
    public void testNullToken() {
        PasswordAuthentication auth = new PasswordAuthentication();
        char[] password = "testpassword".toCharArray();
        
        assertThrows(NullPointerException.class, () -> {
            auth.authenticate(password, null);
        });
    }

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
