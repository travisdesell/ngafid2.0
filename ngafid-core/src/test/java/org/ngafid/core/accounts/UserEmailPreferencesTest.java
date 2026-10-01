package org.ngafid.core.accounts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for UserEmailPreferences class.
 * Tests email preference creation, retrieval, and edge cases.
 */
public class UserEmailPreferencesTest {

    /**
     * Verifies the constructor stores a populated per-type opt-in map and exposes it unchanged via
     * {@code getEmailTypesUser}.
     */
    @Test
    @DisplayName("Should create UserEmailPreferences with valid data")
    public void testConstructorWithValidData() {
        int userId = 1;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("upload_process_start", true);
        emailTypesUser.put("import_processed_receipt", false);
        emailTypesUser.put("airsync_update_report", true);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        assertNotNull(preferences);
        assertEquals(emailTypesUser, preferences.getEmailTypesUser());
    }

    /**
     * Verifies an empty opt-in map is accepted and returned as an empty map.
     */
    @Test
    @DisplayName("Should create UserEmailPreferences with empty email types")
    public void testConstructorWithEmptyEmailTypes() {
        int userId = 2;
        HashMap<String, Boolean> emptyEmailTypes = new HashMap<>();

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emptyEmailTypes);

        assertNotNull(preferences);
        assertEquals(emptyEmailTypes, preferences.getEmailTypesUser());
        assertTrue(preferences.getEmailTypesUser().isEmpty());
    }

    /**
     * Verifies construction with a null opt-in map succeeds (no exception) and the null map is returned as-is.
     */
    @Test
    @DisplayName("Should create UserEmailPreferences with null email types")
    public void testConstructorWithNullEmailTypes() {
        int userId = 3;
        HashMap<String, Boolean> nullEmailTypes = null;

        // This should not throw an exception during construction
        UserEmailPreferences preferences = new UserEmailPreferences(userId, nullEmailTypes);

        assertNotNull(preferences);
        // The getEmailTypesUser() should return the null HashMap
        assertNull(preferences.getEmailTypesUser());
    }

    /**
     * Verifies {@code getEmailTypesUser} returns the same map instance that was supplied (no defensive copy).
     */
    @Test
    @DisplayName("Should get email types user")
    public void testGetEmailTypesUser() {
        int userId = 4;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("test_email_type", true);
        emailTypesUser.put("another_email_type", false);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        HashMap<String, Boolean> result = preferences.getEmailTypesUser();
        assertEquals(emailTypesUser, result);
        assertSame(emailTypesUser, result); // Should return the same reference
    }

    /**
     * Verifies {@code getPreference} returns the stored flag (true or false) for email types present in the map.
     */
    @Test
    @DisplayName("Should get preference for existing email type")
    public void testGetPreferenceForExistingEmailType() {
        int userId = 5;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("upload_process_start", true);
        emailTypesUser.put("import_processed_receipt", false);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        // Use existing EmailType enum values
        assertTrue(preferences.getPreference(EmailType.UPLOAD_PROCESS_START));
        assertFalse(preferences.getPreference(EmailType.IMPORT_PROCESSED_RECEIPT));
    }

    /**
     * Verifies {@code getPreference} defaults to false for an email type that is not present in the map.
     */
    @Test
    @DisplayName("Should get default preference for non-existing email type")
    public void testGetPreferenceForNonExistingEmailType() {
        int userId = 6;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("existing_type", true);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        // Use an existing EmailType that's not in our HashMap
        // Should return false (default value) for non-existing type
        assertFalse(preferences.getPreference(EmailType.AIRSYNC_UPDATE_REPORT));
    }

    /**
     * Verifies {@code getPreference} throws {@link NullPointerException} when the backing opt-in map is null.
     */
    @Test
    @DisplayName("Should throw NullPointerException when email types is null")
    public void testGetPreferenceWithNullEmailTypes() {
        int userId = 7;
        UserEmailPreferences preferences = new UserEmailPreferences(userId, null);

        // Should throw NullPointerException when emailTypesUser is null
        assertThrows(NullPointerException.class, () -> {
            preferences.getPreference(EmailType.UPLOAD_PROCESS_START);
        });
    }

    /**
     * Verifies {@code getPreference} returns false for any type when the opt-in map is empty.
     */
    @Test
    @DisplayName("Should get default preference when email types is empty")
    public void testGetPreferenceWithEmptyEmailTypes() {
        int userId = 8;
        HashMap<String, Boolean> emptyEmailTypes = new HashMap<>();
        UserEmailPreferences preferences = new UserEmailPreferences(userId, emptyEmailTypes);

        // Should return false (default value) for empty HashMap
        assertFalse(preferences.getPreference(EmailType.UPLOAD_PROCESS_START));
    }

    /**
     * Verifies several email types (including those absent from the map) resolve to their correct stored flag or the
     * false default.
     */
    @Test
    @DisplayName("Should handle multiple email types correctly")
    public void testMultipleEmailTypes() {
        int userId = 9;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("upload_process_start", true);
        emailTypesUser.put("import_processed_receipt", false);
        emailTypesUser.put("airsync_update_report", true);
        emailTypesUser.put("ADMIN_shutdown_notification", false);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        assertTrue(preferences.getPreference(EmailType.UPLOAD_PROCESS_START));
        assertFalse(preferences.getPreference(EmailType.IMPORT_PROCESSED_RECEIPT));
        assertTrue(preferences.getPreference(EmailType.AIRSYNC_UPDATE_REPORT));
        assertFalse(preferences.getPreference(EmailType.ADMIN_SHUTDOWN_NOTIFICATION));
        // Test a type not in our HashMap
        assertFalse(preferences.getPreference(EmailType.ADMIN_EXCEPTION_NOTIFICATION));
    }

    /**
     * Verifies {@code getPreference} throws {@link NullPointerException} when passed a null email type.
     */
    @Test
    @DisplayName("Should handle edge case with null email type")
    public void testGetPreferenceWithNullEmailType() {
        int userId = 10;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("upload_process_start", true);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        // Test with null EmailType
        assertThrows(NullPointerException.class, () -> {
            preferences.getPreference(null);
        });
    }

    /**
     * Verifies preferences resolve correctly when the map contains the full set of standard and admin email types.
     */
    @Test
    @DisplayName("Should handle large number of email types")
    public void testLargeNumberOfEmailTypes() {
        int userId = 12;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();

        // Add all available email types
        emailTypesUser.put("upload_process_start", true);
        emailTypesUser.put("import_processed_receipt", false);
        emailTypesUser.put("airsync_update_report", true);
        emailTypesUser.put("ADMIN_shutdown_notification", false);
        emailTypesUser.put("ADMIN_exception_notification", true);
        emailTypesUser.put("ADMIN_airsync_daemon_crash", false);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        // Test all of them
        assertTrue(preferences.getPreference(EmailType.UPLOAD_PROCESS_START));
        assertFalse(preferences.getPreference(EmailType.IMPORT_PROCESSED_RECEIPT));
        assertTrue(preferences.getPreference(EmailType.AIRSYNC_UPDATE_REPORT));
        assertFalse(preferences.getPreference(EmailType.ADMIN_SHUTDOWN_NOTIFICATION));
        assertTrue(preferences.getPreference(EmailType.ADMIN_EXCEPTION_NOTIFICATION));
        assertFalse(preferences.getPreference(EmailType.AIRSYNC_DAEMON_CRASH));
    }

    /**
     * Verifies the opt-in map is held by reference (not copied): mutating the original map after construction is
     * reflected by {@code getEmailTypesUser}.
     */
    @Test
    @DisplayName("Should maintain immutability of email types")
    public void testEmailTypesImmutability() {
        int userId = 13;
        HashMap<String, Boolean> originalEmailTypes = new HashMap<>();
        originalEmailTypes.put("original_type", true);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, originalEmailTypes);

        // Get the email types and try to modify them
        HashMap<String, Boolean> retrievedTypes = preferences.getEmailTypesUser();

        // The retrieved HashMap should be the same reference (not a copy)
        assertSame(originalEmailTypes, retrievedTypes);

        // Modifying the original should affect the retrieved one
        originalEmailTypes.put("new_type", false);
        assertEquals(originalEmailTypes, preferences.getEmailTypesUser());
    }

    /**
     * Verifies admin-prefixed email types resolve to their stored flags like any other type.
     */
    @Test
    @DisplayName("Should handle admin email types correctly")
    public void testAdminEmailTypes() {
        int userId = 14;
        HashMap<String, Boolean> emailTypesUser = new HashMap<>();
        emailTypesUser.put("ADMIN_shutdown_notification", true);
        emailTypesUser.put("ADMIN_exception_notification", false);
        emailTypesUser.put("ADMIN_airsync_daemon_crash", true);

        UserEmailPreferences preferences = new UserEmailPreferences(userId, emailTypesUser);

        assertTrue(preferences.getPreference(EmailType.ADMIN_SHUTDOWN_NOTIFICATION));
        assertFalse(preferences.getPreference(EmailType.ADMIN_EXCEPTION_NOTIFICATION));
        assertTrue(preferences.getPreference(EmailType.AIRSYNC_DAEMON_CRASH));
    }
}
