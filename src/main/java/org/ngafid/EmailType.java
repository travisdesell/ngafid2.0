package org.ngafid;

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

        1. Ensure that you have generated the 'user_preferences_emails' table with 'php db/create_tables.php'
        2. Add email types here!    (Try to keep the constants and string values the same)
        3. Then generate your new types (or remove old ones, see below) with 'sh run/generate_email_types.sh'

    */
    UPLOAD_PROCESS_START("upload_process_start"),
    IMPORT_PROCESSED_RECEIPT("import_processed_receipt"),
    AIRSYNC_UPDATE_REPORT("airsync_update_report"),

    /*
        Admin email types, only visible to admins.
        Types containing the string 'ADMIN' (case-sensitive) will not be displayed to non-admin users.
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
    TEST_EMAIL_TYPE("HIDDEN_test_email_type"),

    /*
        Forced email types, which cannot be changed inside the user interface.
          [For now I'm assuming these should all also be hidden, don't see a point in making them visible.
          These should also still get sent even if the database somehow stores a forced email type as false.]
    */
    ACCOUNT_CREATION_INVITE("FORCED_account_creation_invite"),
    PASSWORD_RESET("FORCED_password_reset"),
    ;


    //NOTE: To remove old email types from the database which aren't listed here anymore, set this flag to true
    private static boolean removeOldEmailTypes = true;

    //---------------------------------------------------------------------------------------------------------


    

    private final String type;


    private static Logger LOG = Logger.getLogger(EmailType.class.getName());
    static {
        LOG.info("EmailType class loaded...");
        
        //Send test email
        ArrayList<String> recipient = new ArrayList<>();
        ArrayList<String> bccRecipients = new ArrayList<>();
        recipient.add("aidanqsack@gmail.com");
        SendEmail.sendEmail(
            recipient,
            bccRecipients,
            "Test Email",
            "AUGHHHHHHHHH",
            EmailType.TEST_EMAIL_TYPE
            );
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

    public static boolean isForced(EmailType emailType) {
        return emailType.getType().contains("FORCED");
        }

    //PHP Execution
    public static void insertEmailTypesIntoDatabase() {

        //Record all email types currently in the database
        Set<String> currentEmailTypes = new HashSet<>();
        for (EmailType emailType : EmailType.values()) {
            currentEmailTypes.add(emailType.getType());
            }

        //Remove old email types from the database
        if (removeOldEmailTypes)
            removeOldEmailTypesFromDatabase(currentEmailTypes);



        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO user_preferences_emails (user_id, email_type) ");

        //Generate "individual" email type queries
        for(EmailType emailType : EmailType.values()) {
            query.append("SELECT id, '").append(emailType.getType()).append("' FROM user UNION ALL ");
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

            }
        catch (SQLException e) {

            e.printStackTrace();
            System.out.println("Error executing Email Type generation query: " + e.getMessage());

            }

        }

    private static void removeOldEmailTypesFromDatabase(Set<String> currentEmailTypes) {

        String selectQuery = "SELECT DISTINCT email_type FROM user_preferences_emails";
        String deleteQuery = "DELETE FROM user_preferences_emails WHERE email_type = ?";

        try (
            Connection connection = Database.getConnection();
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)
            ) {

            ResultSet queryResult = selectStatement.executeQuery();
            while (queryResult.next()) {

                String emailType = queryResult.getString("email_type");
                if (currentEmailTypes.contains(emailType) == false) {

                    deleteStatement.setString(1, emailType);
                    deleteStatement.executeUpdate();
                    System.out.println("Removed old Email Type: " + emailType);
                
                    }
            
                }

            connection.close();

            }

        catch (SQLException e) {

            e.printStackTrace();
            System.out.println("Error removing old Email Types: " + e.getMessage());
        
            }
        }

    //Main
    public static void main(String[] args) {
        insertEmailTypesIntoDatabase();
        System.exit(0);
        }

    }
