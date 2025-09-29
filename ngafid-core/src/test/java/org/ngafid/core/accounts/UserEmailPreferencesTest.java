package org.ngafid.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

/**
 * Unit tests for UserEmailPreferences class.
 * Tests email preference creation, retrieval, and edge cases.
 */
public class UserEmailPreferencesTest {

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

    @Test
    @DisplayName("Should get default preference when email types is empty")
    public void testGetPreferenceWithEmptyEmailTypes() {
        int userId = 8;
        HashMap<String, Boolean> emptyEmailTypes = new HashMap<>();
        UserEmailPreferences preferences = new UserEmailPreferences(userId, emptyEmailTypes);
        
        // Should return false (default value) for empty HashMap
        assertFalse(preferences.getPreference(EmailType.UPLOAD_PROCESS_START));
    }

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
