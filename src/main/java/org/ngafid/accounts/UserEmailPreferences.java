package org.ngafid.accounts;

import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class UserEmailPreferences {

    private int userId;
    private HashMap<String, Boolean> emailTypesUser;
    private String[] emailTypesKeys;

    private static HashMap<Integer, User> users = new HashMap<>();
    private static HashMap<String, Integer> userIDs = new HashMap<>();
    private static HashMap<String, HashMap<String, Boolean>> emailTypesUsers = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(UserEmailPreferences.class.getName());


    public UserEmailPreferences(int userId, HashMap<String, Boolean> emailTypesUser) {
        this.userId = userId;
        this.emailTypesUser = emailTypesUser;
        
        String[] keysRecent = EmailType.getEmailTypeKeysRecent(true);
        /*
        LOG.info("[EX] Email Type Keys: ");
        for (String key : keysRecent) {
            LOG.info("- Key "+key);
        }
        */

        this.emailTypesKeys = keysRecent;
    }

    public static void addUser(Connection connection, User user) throws SQLException {

        int userID = user.getId();
        String userEmail = user.getEmail();

        //user email --> userId
        userIDs.put(userEmail, userID);

        //userId --> User
        users.put(userID, user);

        emailTypesUsers.put( user.getEmail(), user.getUserEmailPreferences(connection).getEmailTypesUser() );

    }

    public static User getUser(int userId) {
        return users.get(userId);
    }

    public static int getUserIDFromEmail(String userEmail) {
        return userIDs.get(userEmail);
    }

    public HashMap<String, Boolean> getEmailTypesUser() {
        return emailTypesUser;
    }

    public static boolean getEmailTypeUserState(String email, EmailType emailType) {

        //Email does not exist in the map, default to true
        if (!emailTypesUsers.containsKey(email)) {
            LOG.info("User's email does not exist in the map, defaulting to true");
            return true;
        }

        HashMap<String, Boolean> emailTypesTarget = emailTypesUsers.get(email);

        String emailTypeValue = emailType.getType();

        //Email type does not exist in the map, default to true
        if (!emailTypesTarget.containsKey(emailTypeValue)) {
            LOG.info("Email Type does not exist in the map, defaulting to true");
            return true;
        }

        return emailTypesTarget.get(emailTypeValue);

    }

}
