package org.ngafid.accounts;

import java.util.HashMap;
import java.util.Map;

import org.ngafid.Database;
import org.ngafid.accounts.EmailType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;

import java.util.logging.Logger;

import static org.ngafid.flights.calculations.Parameters.*;



public class UserEmailPreferences {

    private int userId;
    private HashMap<String, Boolean> emailTypesUser;
    private String[] emailTypesKeys;

    private static HashMap<Integer, User> users = new HashMap<Integer, User>();
    private static HashMap<String, HashMap<String, Boolean>> emailTypesUsers = new HashMap<String, HashMap<String, Boolean>>();

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

        users.put(user.getId(), user);

        emailTypesUsers.put( user.getEmail(), user.getUserEmailPreferences(connection).getEmailTypesUser() );

    }

    public static User getUser(int userId) {
        return users.get(userId);
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