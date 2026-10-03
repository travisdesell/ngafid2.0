package org.ngafid.core.accounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.ngafid.core.Database;

/**
 * Enumerates the categories of notification email the system can send, each with a stable string key.
 *
 * <p>The keys back the {@code email_preferences} table and drive per-user opt-in/opt-out handling; admin-only types are
 * marked by an {@code ADMIN} substring in their key and are hidden from non-admin users. New types are added here and
 * then synchronized to the database via the generation scripts.
 */
public enum EmailType {

    // -------------------------------------------------------------------------------------------------------------

    /*
     *
     * USAGE:
     *
     * 1. Ensure that you have generated the 'email_preferences' table with 'php
     * db/create_tables.php'
     * 2. Add email types here! (Try to keep the constants and string values the
     * same)
     * 3. Then generate your new types with 'sh run/generate_email_types.sh'
     * 4. [Optional] If you have outdated types to remove, instead use 'sh
     * run/generate_email_types_and_remove.sh'
     *
     */
    UPLOAD_PROCESS_START("upload_process_start"),
    IMPORT_PROCESSED_RECEIPT("import_processed_receipt"),
    AIRSYNC_UPDATE_REPORT("airsync_update_report"),

    /*
     * Admin email types, only visible to admins.
     * Types containing the string 'ADMIN' (case-sensitive) will not be displayed to
     * non-admin users.
     *
     * Because the email types will default to true anyways, never try to use the
     * values of these
     * to detect whether or not a user is an admin.
     */
    ADMIN_SHUTDOWN_NOTIFICATION("ADMIN_shutdown_notification"),
    ADMIN_EXCEPTION_NOTIFICATION("ADMIN_exception_notification"),
    AIRSYNC_DAEMON_CRASH("ADMIN_airsync_daemon_crash"),

    /*
     * Forced email types, which cannot be changed inside the user interface.
     * These are not actually stored in the database, and exist for organizational
     * purposes.
     */
    ACCOUNT_CREATION_INVITE("FORCED_account_creation_invite"),
    PASSWORD_RESET("FORCED_password_reset"),
    BUG_REPORT("FORCED_bug_report");

    // -------------------------------------------------------------------------------------------------------------

    /*
     * Default value of the removal flag for old email types.
     *
     * Use 'sh run/generate_email_types_and_remove.sh' to temporarily override this
     * value
     * rather than changing it here.
     */
    private static boolean removeOldEmailTypes = false;

    // -------------------------------------------------------------------------------------------------------------

    private final String type;

    // Track the most recent/up-to-date keys for the email types fetched from the
    // database
    private static final HashSet<String> EMAIL_TYPE_KEYS_RECENT = new HashSet<>();

    // Store number of email types
    private static final int EMAIL_TYPE_COUNT = values().length;
    private static final int EMAIL_TYPE_NON_FORCED_COUNT;

    private static final Logger LOG = Logger.getLogger(EmailType.class.getName());

    static {
        int emailTypeNonForcedCounter = 0;
        for (EmailType emailType : EmailType.values()) {
            if (!isForced(emailType)) {
                emailTypeNonForcedCounter++;
            }
        }
        EMAIL_TYPE_NON_FORCED_COUNT = emailTypeNonForcedCounter;

        LOG.info("EmailType class loaded...");
        LOG.info("Detected " + EMAIL_TYPE_COUNT + " email types");
    }

    EmailType(String type) {
        this.type = type;
    }

    // Getters
    public String getType() {
        return type;
    }

    public static int getEmailTypeCount() {
        return EMAIL_TYPE_COUNT;
    }

    public static int getEmailTypeCountNonForced() {
        return EMAIL_TYPE_NON_FORCED_COUNT;
    }

    public static EmailType[] getAllTypes() {
        return values();
    }

    /**
     * Returns the set of email-type keys most recently synced with the database. When {@code doRefresh} is true, the
     * cache is first rebuilt from the current enum values (and old types pruned from the database if removal is
     * enabled) before being returned.
     *
     * @param doRefresh whether to rebuild the cached key set before returning it
     * @return the recent email-type keys, in no particular order
     */
    public static String[] getEmailTypeKeysRecent(boolean doRefresh) {

        // Force a refresh of the keys
        if (doRefresh) refreshEmailTypeKeysRecent();

        String[] keysOut = new String[EMAIL_TYPE_KEYS_RECENT.size()];
        return EMAIL_TYPE_KEYS_RECENT.toArray(keysOut);
    }

    /**
     * Reports whether an email type is "forced": one that cannot be toggled by the user and is not stored in the
     * database. Forced types are identified by the substring {@code "FORCED"} in their key.
     *
     * @param emailType the email type to test
     * @return true if the type is forced
     */
    public static boolean isForced(EmailType emailType) {
        return emailType.getType().contains("FORCED");
    }

    /**
     * Reports whether an email-type key names a "forced" type (see {@link #isForced(EmailType)}), identified by the
     * substring {@code "FORCED"} in the key.
     *
     * @param emailTypeName the email-type key to test
     * @return true if the key names a forced type
     */
    public static boolean isForced(String emailTypeName) {
        return emailTypeName.contains("FORCED");
    }

    /**
     * Ensures an {@code email_preferences} row exists for every (user, non-forced email type) pair across all users.
     * Forced types are skipped (they are not persisted); if old-type removal is enabled, email types no longer defined
     * in this enum are first deleted from the table. Builds a single UNION-ALL insert over the {@code user} table with
     * {@code ON DUPLICATE KEY UPDATE} so existing rows are left intact, and refreshes the recent-keys cache as a side
     * effect. Errors are logged rather than thrown.
     */
    public static void insertEmailTypesIntoDatabase() {

        /*
         * Generate email types for all users in the database...
         */

        // Clear the recent email type keys
        EMAIL_TYPE_KEYS_RECENT.clear();

        // Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {

            // Skip FORCED email types
            if (isForced(emailType)) {
                LOG.info("Skipping FORCED email type: " + emailType.getType());
                continue;
            }

            currentEmailTypes.add(emailType.getType());
        }

        // Remove old email types from the database
        if (removeOldEmailTypes) {
            removeOldEmailTypesFromDatabase(currentEmailTypes);
        }

        // Generate "individual" email type queries
        List<String> emailTypeQueries = new ArrayList<>();
        for (EmailType emailType : EmailType.values()) {

            // Skip FORCED email types
            if (isForced(emailType)) {
                continue;
            }

            String emailTypeKey = emailType.getType();
            EMAIL_TYPE_KEYS_RECENT.add(emailTypeKey);
            emailTypeQueries.add("SELECT id, '" + emailTypeKey + "' FROM user");

            LOG.info("Email Type: " + emailTypeKey + " marked for database insertion...");
        }

        // Merge individual email type queries
        String query = "INSERT INTO email_preferences (user_id, email_type) "
                + String.join(" UNION ALL ", emailTypeQueries)
                + " ON DUPLICATE KEY UPDATE email_type = VALUES(email_type)";

        try (Connection connection = Database.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)) {

            statement.executeUpdate();
            LOG.info("Email Type generation query executed successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            LOG.severe("Error executing Email Type generation query: " + e.getMessage());
        }
    }

    /**
     * Ensures an {@code email_preferences} row exists for every non-forced email type for a single user, used when a
     * new account is registered. Behaves like {@link #insertEmailTypesIntoDatabase()} but inserts rows only for the
     * given user id and runs on the caller's connection (so it can participate in the registration transaction).
     *
     * @param connection the database connection to run the insert on
     * @param userIDTarget the id of the user to generate email-preference rows for
     * @throws SQLException if the insert fails
     */
    public static void insertEmailTypesIntoDatabase(Connection connection, int userIDTarget) throws SQLException {

        /*
         * Generate email types for a specific user ID...
         * (for new user registration)
         */

        // Clear the recent email type keys
        EMAIL_TYPE_KEYS_RECENT.clear();

        // Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {

            // Skip FORCED email types
            if (isForced(emailType)) {
                continue;
            }

            currentEmailTypes.add(emailType.getType());
        }

        // Remove old email types from the database
        if (removeOldEmailTypes) {
            removeOldEmailTypesFromDatabase(currentEmailTypes);
        }

        // Generate "individual" email type queries
        List<String> emailTypeQueries = new ArrayList<>();
        for (EmailType emailType : EmailType.values()) {

            // Skip FORCED email types
            if (isForced(emailType)) {
                continue;
            }

            String emailTypeKey = emailType.getType();
            EMAIL_TYPE_KEYS_RECENT.add(emailTypeKey);
            emailTypeQueries.add("SELECT '" + userIDTarget + "', '" + emailTypeKey + "' FROM user");

            LOG.info("Email Type: " + emailTypeKey + " marked for database insertion...");
        }

        // Merge individual email type queries
        String query = "INSERT INTO email_preferences (user_id, email_type) "
                + String.join(" UNION ALL ", emailTypeQueries)
                + " ON DUPLICATE KEY UPDATE email_type = VALUES(email_type)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {

            statement.executeUpdate();
            LOG.info("Email Type generation query executed successfully");
        }
    }

    private static void removeOldEmailTypesFromDatabase(Set<String> currentEmailTypes) {

        String selectQuery = "SELECT DISTINCT email_type FROM email_preferences";
        String deleteQuery = "DELETE FROM email_preferences WHERE email_type = ?";

        List<String> emailTypesForDeletion = new ArrayList<>();

        try (Connection connection = Database.getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                ResultSet queryResult = selectStatement.executeQuery()) {

            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {

                // Mark email types for deletion
                while (queryResult.next()) {
                    String emailType = queryResult.getString("email_type");
                    if (!currentEmailTypes.contains(emailType)) {
                        emailTypesForDeletion.add(emailType);
                    }
                }

                // Perform deletion query on marked emails
                for (String emailType : emailTypesForDeletion) {
                    deleteStatement.setString(1, emailType);
                    deleteStatement.executeUpdate();
                    LOG.info("Removed old Email Type: " + emailType);
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
            LOG.severe("Error removing old Email Types: " + e.getMessage());
        }
    }

    private static void refreshEmailTypeKeysRecent() {

        // Clear the recent email type keys
        EMAIL_TYPE_KEYS_RECENT.clear();

        // Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {
            currentEmailTypes.add(emailType.getType());
        }

        // Remove old email types from the database
        if (removeOldEmailTypes) {
            removeOldEmailTypesFromDatabase(currentEmailTypes);
        }

        // Record all email types currently in the database
        for (EmailType emailType : EmailType.values()) {
            EMAIL_TYPE_KEYS_RECENT.add(emailType.getType());
        }
    }

    /**
     * Command-line entry point that regenerates the email-preference rows for all users. An optional first argument, if
     * parseable as a boolean, sets whether email types no longer defined in this enum are removed from the database.
     * Exits the JVM on completion unless running under a test framework.
     *
     * @param args optional single argument: "true" to also remove obsolete email types
     */
    public static void main(String[] args) {

        if (args.length > 0) {
            removeOldEmailTypes = Boolean.parseBoolean(args[0]);
            LOG.info("Removing old Email Types!!");
        }

        insertEmailTypesIntoDatabase();

        // Only exit if not running in a test environment
        if (!isRunningInTestEnvironment()) {
            System.exit(0);
        }
    }

    /**
     * Detects if the code is running in a test environment
     * @return true if running in tests, false otherwise
     */
    private static boolean isRunningInTestEnvironment() {
        // Check for common test frameworks
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("junit")
                    || className.contains("test")
                    || className.contains("Test")
                    || className.contains("surefire")
                    || className.contains("maven")) {
                return true;
            }
        }
        return false;
    }
}
