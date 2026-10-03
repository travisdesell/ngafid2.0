package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link User}'s two-factor-authentication accessors: the enabled flag, the TOTP secret, the backup codes,
 * and the setup-complete flag, verifying each getter's default and that the corresponding setter round-trips.
 *
 * <p>Shared user/fleet fixtures and per-test seeding live in {@link UserTestBase}.
 */
public class UserTwoFactorTest extends UserTestBase {

    /**
     * Verifies {@code isTwoFactorEnabled} defaults to false for a freshly loaded user.
     */
    @Test
    @DisplayName("Should return false for two-factor authentication with default user")
    public void isTwoFactorEnabledWithDefaultUser() {
        User user = user1Fleet1;

        boolean isEnabled = user.isTwoFactorEnabled();

        assertFalse(isEnabled);
    }

    /**
     * Verifies {@code setTwoFactorEnabled(true)} is reflected by {@code isTwoFactorEnabled}.
     */
    @Test
    @DisplayName("Should set two-factor authentication enabled with true value")
    public void setTwoFactorEnabledWithTrueValue() {
        User user = user1Fleet1;

        user.setTwoFactorEnabled(true);

        assertTrue(user.isTwoFactorEnabled());
    }

    /**
     * Verifies {@code getTwoFactorSecret} defaults to null for a freshly loaded user.
     */
    @Test
    @DisplayName("Should return null for two-factor secret with default user")
    public void getTwoFactorSecretWithDefaultUser() {
        User user = user1Fleet1;

        String secret = user.getTwoFactorSecret();

        assertNull(secret);
    }

    /**
     * Verifies {@code setTwoFactorSecret} stores the secret so {@code getTwoFactorSecret} returns it.
     */
    @Test
    @DisplayName("Should set two-factor secret with valid secret")
    public void setTwoFactorSecretWithValidSecret() {
        User user = user1Fleet1;
        String secret = "testsecret123";

        user.setTwoFactorSecret(secret);

        assertEquals(secret, user.getTwoFactorSecret());
    }

    /**
     * Verifies {@code getBackupCodes} defaults to null for a freshly loaded user.
     */
    @Test
    @DisplayName("Should return null for backup codes with default user")
    public void getBackupCodesWithDefaultUser() {
        User user = user1Fleet1;

        String backupCodes = user.getBackupCodes();

        assertNull(backupCodes);
    }

    /**
     * Verifies {@code setBackupCodes} stores the codes so {@code getBackupCodes} returns them.
     */
    @Test
    @DisplayName("Should set backup codes with valid codes")
    public void setBackupCodesWithValidCodes() {
        User user = user1Fleet1;
        String backupCodes = "code1,code2,code3";

        user.setBackupCodes(backupCodes);

        assertEquals(backupCodes, user.getBackupCodes());
    }

    /**
     * Verifies {@code isTwoFactorSetupComplete} defaults to false for a freshly loaded user.
     */
    @Test
    @DisplayName("Should return false for two-factor setup completion with default user")
    public void isTwoFactorSetupCompleteWithDefaultUser() {
        User user = user1Fleet1;

        boolean isComplete = user.isTwoFactorSetupComplete();

        assertFalse(isComplete);
    }

    /**
     * Verifies {@code setTwoFactorSetupComplete(true)} is reflected by {@code isTwoFactorSetupComplete}.
     */
    @Test
    @DisplayName("Should set two-factor setup complete with true value")
    public void setTwoFactorSetupCompleteWithTrueValue() {
        User user = user1Fleet1;

        user.setTwoFactorSetupComplete(true);

        assertTrue(user.isTwoFactorSetupComplete());
    }
}
