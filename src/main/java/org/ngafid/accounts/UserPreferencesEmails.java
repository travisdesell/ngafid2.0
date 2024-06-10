package org.ngafid.accounts;

import java.util.HashMap;
import java.util.Map;

import org.ngafid.Database;
import org.ngafid.EmailType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;

import java.util.logging.Logger;

import static org.ngafid.flights.calculations.Parameters.*;



public class UserPreferencesEmails {

    private int userId;
    private HashMap<String, Boolean> emailTypesUser;
    private String[] emailTypesKeys;

    private static HashMap<Integer, User> users = new HashMap<Integer, User>();
    private static HashMap<String, HashMap<String, Boolean>> emailTypesUsers = new HashMap<String, HashMap<String, Boolean>>();

    private static final Logger LOG = Logger.getLogger(UserPreferencesEmails.class.getName());

    //Store default email types in a HashMap
    private static HashMap<String, String> defaultEmailTypes = new HashMap<String, String>();
    private static String[] defaultEmailKeys;
    static {

        try {

            Connection connection = Database.getConnection();
            populateEmailTypes(connection);

            }
        catch (SQLException e) {

            System.err.println("Error initializing email types: " + e.getMessage());
            e.printStackTrace();

            }
    
        }

    //Constructor
    public UserPreferencesEmails(int userId, HashMap<String, Boolean> emailTypesUser) {
        this.userId = userId;
        this.emailTypesUser = emailTypesUser;
        this.emailTypesKeys = defaultEmailKeys;
        }


    public static UserPreferencesEmails defaultPreferences(int userId) {

        HashMap<String, Boolean> emailTypesUser = new HashMap<String, Boolean>();

        for (String emailType : UserPreferencesEmails.defaultEmailTypes.keySet()) {
            emailTypesUser.put(emailType, false);
            }

        return new UserPreferencesEmails(userId, emailTypesUser);

        }

    private static void populateEmailTypes(Connection connection) throws SQLException {

        String query = "SELECT email_type, enabled FROM email_preferences";
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            defaultEmailTypes.put(resultSet.getString(1), resultSet.getString(2));
            }
    
        defaultEmailKeys = defaultEmailTypes.keySet().toArray( new String[defaultEmailTypes.size() ]);

        }

    public static void addUser(User user) {

        users.put(user.getId(), user);

        emailTypesUsers.put(
            user.getEmail(),
            user.getUserPreferencesEmails().getEmailTypesUser()
            );

        }

    public static User getUser(int userId) {
        return users.get(userId);
        }

    public static HashMap<String, String> getDefaultEmailTypes() {
        return new HashMap<>(defaultEmailTypes);
        }

    public HashMap<String, Boolean> getEmailTypesUser() {
        return emailTypesUser;
        }

    public static boolean getEmailTypeUserState(String email, EmailType emailType) {

        //Email does not exist in the map, default to true
        if (emailTypesUsers.containsKey(email) == false) {
            LOG.info("User's email does not exist in the map, defaulting to true");
            return true;
            }

        HashMap<String, Boolean> emailTypesTarget = emailTypesUsers.get(email);

        String emailTypeValue = emailType.getType();

        //Email type does not exist in the map, default to true
        if (emailTypesTarget.containsKey(emailTypeValue) == false) {
            LOG.info("Email Type does not exist in the map, defaulting to true");
            return true;
            }

        return emailTypesTarget.get(emailTypeValue);

        }

    }