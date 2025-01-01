package org.ngafid.accounts;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

public class UserEmailPreferences {

    private static final Logger LOG = Logger.getLogger(UserEmailPreferences.class.getName());
    private static final HashMap<Integer, User> USERS = new HashMap<>();
    private static final HashMap<String, Integer> USER_IDS = new HashMap<>();
    private static final HashMap<String, HashMap<String, Boolean>> EMAIL_TYPES_USERS = new HashMap<>();
    private final int userId;
    private final HashMap<String, Boolean> emailTypesUser;
    private final String[] emailTypesKeys;


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
        USER_IDS.put(userEmail, userID);

        //userId --> User
        USERS.put(userID, user);

        EMAIL_TYPES_USERS.put(user.getEmail(), user.getUserEmailPreferences(connection).getEmailTypesUser());

    }

    public static User getUser(int userId) {
        return USERS.get(userId);
    }

    public static int getUserIDFromEmail(String userEmail) {
        return USER_IDS.get(userEmail);
    }

    public static boolean getEmailTypeUserState(String email, EmailType emailType) {

        //Email does not exist in the map, default to true
        if (!EMAIL_TYPES_USERS.containsKey(email)) {
            LOG.info("User's email does not exist in the map, defaulting to true");
            return true;
        }

        HashMap<String, Boolean> emailTypesTarget = EMAIL_TYPES_USERS.get(email);

        String emailTypeValue = emailType.getType();

        //Email type does not exist in the map, default to true
        if (!emailTypesTarget.containsKey(emailTypeValue)) {
            LOG.info("Email Type does not exist in the map, defaulting to true");
            return true;
        }

        return emailTypesTarget.get(emailTypeValue);

    }

    public HashMap<String, Boolean> getEmailTypesUser() {
        return emailTypesUser;
    }

}
