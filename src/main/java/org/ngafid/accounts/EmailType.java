package org.ngafid.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

import java.util.*;
import java.util.logging.Logger;

import org.ngafid.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public enum EmailType {

    //---------------------------------------------------------------------------------------------------------

    /*

        USAGE:

        1. Ensure that you have generated the 'email_preferences' table with 'php db/create_tables.php'
        2. Add email types here!    (Try to keep the constants and string values the same)
        3. Then generate your new types (or remove old ones, see below) with 'sh run/generate_email_types.sh'

    */
    UPLOAD_PROCESS_START("upload_process_start"),
    IMPORT_PROCESSED_RECEIPT("import_processed_receipt"),
    AIRSYNC_UPDATE_REPORT("airsync_update_report"),

    /*
        Admin email types, only visible to admins.
        Types containing the string 'ADMIN' (case-sensitive) will not be displayed to non-admin users.

        Because the email types will default to true anyways, never try to use the values of these
        to detect whether or not a user is an admin.
    */
    ADMIN_SHUTDOWN_NOTIFICATION("ADMIN_shutdown_notification"),
    ADMIN_EXCEPTION_NOTIFICATION("ADMIN_exception_notification"),
    AIRSYNC_DAEMON_CRASH("ADMIN_airsync_daemon_crash"),

    /*
        [NOT IMPLEMENTED, NOT SURE IF THIS WOULD BE USEFUL AT ALL]
        Managed email types, only configurable by the user's fleet manager.
    */
        //...

    /*
        Hidden email types (not visible to the user), used for testing or other purposes.
        Types containing the string 'HIDDEN' (case-sensitive) will not be displayed in the user interface.
          [This might be redundant outside testing with the forced email types, but I'm keeping it for now.]
    */
    //  TEST_EMAIL_TYPE("HIDDEN_test_email_type"),

    /*
        Forced email types, which cannot be changed inside the user interface.
          [For now I'm assuming these should all also be hidden, don't see a point in making them visible.
          These should also still get sent even if the database somehow stores a forced email type as false.]
    */
    ACCOUNT_CREATION_INVITE("FORCED_account_creation_invite"),
    PASSWORD_RESET("FORCED_password_reset"),
    ;


    //NOTE: To remove old email types from the database which aren't listed here anymore, set this flag to true
    private static boolean removeOldEmailTypes = false;

    //---------------------------------------------------------------------------------------------------------


    private final String type;

    //Track the most recent/up-to-date keys for the email types fetched from the database
    private final static HashSet<String> emailTypeKeysRecent = new HashSet<>();

    private static Logger LOG = Logger.getLogger(EmailType.class.getName());
    static {
        LOG.info("EmailType class loaded...");
    }


    //Constructor
    EmailType(String type) {
        this.type = type;
    }


    //Getters
    public String getType() {
        return type;
    }

    public static EmailType[] getAllTypes() {
        return values();
    }

    public static String[] getEmailTypeKeysRecent(boolean doRefresh) {

        //Force a refresh of the keys
        if (doRefresh)
            refreshEmailTypeKeysRecent();

        String[] keysOut = new String[emailTypeKeysRecent.size()];
        return emailTypeKeysRecent.toArray(keysOut);
    }

    public static boolean isForced(EmailType emailType) {
        return emailType.getType().contains("FORCED");
    }


    //PHP Execution
    public static void insertEmailTypesIntoDatabase() {

        /*
            Generate email types for all users in the database...
        */

        //Clear the recent email type keys
        emailTypeKeysRecent.clear();

        //Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {
            currentEmailTypes.add(emailType.getType());
        }

        //Remove old email types from the database
        if (removeOldEmailTypes) {
            removeOldEmailTypesFromDatabase(currentEmailTypes);
        }

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO email_preferences (user_id, email_type) ");

        //Generate "individual" email type queries
        for(EmailType emailType : EmailType.values()) {
            String emailTypeKey = emailType.getType();
            emailTypeKeysRecent.add(emailTypeKey);
            query.append("SELECT id, '").append(emailTypeKey).append("' FROM user UNION ALL ");
        }

        //Remove the last "UNION ALL" from the query
        int lastIndex = query.lastIndexOf("UNION ALL");
        query.delete(lastIndex, lastIndex + "UNION ALL".length());
        query.append(" ON DUPLICATE KEY UPDATE email_type = VALUES(email_type)");

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(query.toString())
            ) {

            statement.executeUpdate();
            System.out.println("Email Type generation query executed successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing Email Type generation query: " + e.getMessage());
        }

    }

    public static void insertEmailTypesIntoDatabase(int userIDTarget) {

        /*
            Generate email types for a specific user ID...
            (for new user registration)
        */

        //Clear the recent email type keys
        emailTypeKeysRecent.clear();

        //Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {
            currentEmailTypes.add(emailType.getType());
        }

        //Remove old email types from the database
        if (removeOldEmailTypes) {
            removeOldEmailTypesFromDatabase(currentEmailTypes);
        }

        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO email_preferences (user_id, email_type) ");

        //Generate "individual" email type queries
        for (EmailType emailType : EmailType.values()) {
            String emailTypeKey = emailType.getType();
            emailTypeKeysRecent.add(emailTypeKey);
            query.append("SELECT ").append(userIDTarget).append(", '").append(emailTypeKey).append("' UNION ALL ");
        }

        //Remove the last "UNION ALL" from the query
        int lastIndex = query.lastIndexOf("UNION ALL");

        query.delete(lastIndex, lastIndex + "UNION ALL".length());
        query.append(" ON DUPLICATE KEY UPDATE email_type = VALUES(email_type)");

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(query.toString())
            ) {

            statement.executeUpdate();
            System.out.println("Email Type generation query executed successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing Email Type generation query: " + e.getMessage());
        }

    }

    private static void removeOldEmailTypesFromDatabase(Set<String> currentEmailTypes) {

        String selectQuery = "SELECT DISTINCT email_type FROM email_preferences";
        String deleteQuery = "DELETE FROM email_preferences WHERE email_type = ?";

        try (
            Connection connection = Database.getConnection();
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)
            ) {

            ResultSet queryResult = selectStatement.executeQuery();
            while (queryResult.next()) {

                String emailType = queryResult.getString("email_type");
                if (!currentEmailTypes.contains(emailType)) {

                    deleteStatement.setString(1, emailType);
                    deleteStatement.executeUpdate();
                    System.out.println("Removed old Email Type: " + emailType);
            
                }
        
            }

            connection.close();

        } catch (SQLException e) {

            e.printStackTrace();
            System.out.println("Error removing old Email Types: " + e.getMessage());
        
        }

    }

    private static void refreshEmailTypeKeysRecent() {

        //Clear the recent email type keys
        emailTypeKeysRecent.clear();

        //Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {
            currentEmailTypes.add(emailType.getType());
        }

        //Remove old email types from the database
        if (removeOldEmailTypes) {
            removeOldEmailTypesFromDatabase(currentEmailTypes);
        }

        //Record all email types currently in the database
        for (EmailType emailType : EmailType.values()) {
            emailTypeKeysRecent.add(emailType.getType());
        }

    }


    //Main
    public static void main(String[] args) {
        insertEmailTypesIntoDatabase();
        System.exit(0);
    }

}
