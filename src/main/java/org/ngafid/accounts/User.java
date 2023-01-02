package org.ngafid.accounts;


import java.sql.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.ngafid.filters.Filter;
import org.ngafid.flights.Tails;

import com.google.gson.Gson;

public class User {
    private static final Logger LOG = Logger.getLogger(User.class.getName());

    /**
     *  The following variables hold all the user informatino that is stored in the 'user'
     *  table in the mysql database.
     */
    private int id = -1;
    private String email;
    private String firstName;
    private String lastName;
    private String country;
    private String state;
    private String city;
    private String address;
    private String phoneNumber;
    private String zipCode;
    private boolean admin;
    private boolean aggregateView;

    /**
     * The following are references to the fleet the user has access to (if it has approved access
     * or management rights to a fleet), and the access level the user has on that fleet.
     */
    private Fleet fleet;
    private FleetAccess fleetAccess;

    /**
     * @return the user's id.
     */
    public int getId() {
        return id;
    }

    /**
     * @return the user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the full name (first + last) of the user.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }


    /**
     * @return true if the user has admin access
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @return true if the user has aggregate view access
     */
    public boolean hasAggregateView() {
        return aggregateView;
    }


    /**
     * @return the user's fleet id
     */
    public int getFleetId() {
        return fleet.getId();
    }

    /**
     * @return the user's fleet access
     */
    public String getFleetAccessType() {
        return fleetAccess.getAccessType();
    }

    /**
     * @return the number of users waiting for access to this user's fleet
     */
    public int getWaitingUserCount() {
        return fleet.getWaitingUserCount();
    }

    /**
     * @return the number of unconfirmed tails for this user's fleet
     */
    public int getUnconfirmedTailsCount(Connection connection) throws SQLException {
        return Tails.getUnconfirmedTailsCount(connection, fleet.getId());
    }

    /**
     * Checks to see if the user has access to a particular flight. To have access, the user must not be waiting on fleet access, and the user must have access to the fleet of the flight.
     * @param flightId the id of the flight.
     *
     * @return true if the user has access to the flight.
     */
    public boolean hasFlightAccess(Connection connection, int flightId) throws SQLException {
        if (fleetAccess.isWaiting() || fleetAccess.isDenied()) return false;

        PreparedStatement query = connection.prepareStatement("SELECT id FROM flights WHERE fleet_id = ? and id = ?");
        query.setInt(1, getFleetId());
        query.setInt(2, flightId);

        LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();

        //if there was a flight in the result set then the user had access (i.e., the flight was in the user's fleet).
        return resultSet.next();
    }

    /**
     * Checks to see if the user is a manager of a particular fleet. Typically used to allow access to modify that fleet.
     *
     * @param fleetId the fleet the access check is being made on
     * 
     * @return true if the user has access to that fleet.
     */
    public boolean managesFleet(int fleetId) {
        return fleet.getId() == fleetId && fleetAccess.isManager();
    }

    /**
     * Checks to see if the user can upload flights for a fleet.
     *
     * @param fleetId the fleet the access check is being made on
     * 
     * @return true if the user has access to that fleet.
     */
    public boolean hasUploadAccess(int fleetId) {
        return fleet.getId() == fleetId && (fleetAccess.isManager() || fleetAccess.isUpload());
    }

    /**
     * Checks to see if the user can view flights for a fleet.
     *
     * @param fleetId the fleet the access check is being made on
     * 
     * @return true if the user has access to that fleet.
     */
    public boolean hasViewAccess(int fleetId) {
        return fleet.getId() == fleetId && (fleetAccess.isManager() || fleetAccess.isUpload() || fleetAccess.isView());
    }

    /**
     * Return the number of users in the NGAFID
     *
     * @param connection A connection to the mysql database.
     *
     * @exception SQLException if there was a problem with the query or database.
     *
     * @return the number of users in the NGAFID
     */
    public static int getNumberUsers(Connection connection) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT count(id) FROM user");

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            int numberUsers = resultSet.getInt(1);
            return numberUsers;
        } else {
            return 0;
        }
    }


    /**
     * Get a user from the database based on the users id. 
     *
     * @param connection A connection to the mysql database.
     * @param userId The user's id.
     *
     * @exception SQLException if there was a problem with the SQL query.
     *
     * @return A user object for the user with that id, null if the user did not exist.
     */
    public static User get(Connection connection, int userId, int fleetId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id, email, first_name, last_name, country, state, city, address, phone_number, zip_code, admin, aggregate_view FROM user WHERE id = ?");
        query.setInt(1, userId);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) return null;

        User user = new User();
        user.id = resultSet.getInt(1);
        user.email = resultSet.getString(2);
        user.firstName = resultSet.getString(3);
        user.lastName = resultSet.getString(4);
        user.country = resultSet.getString(5);
        user.state = resultSet.getString(6);
        user.city = resultSet.getString(7);
        user.address = resultSet.getString(8);
        user.phoneNumber = resultSet.getString(9);
        user.zipCode = resultSet.getString(10);
        user.admin = resultSet.getBoolean(11);
        user.aggregateView = resultSet.getBoolean(12);

        //get the access level of the user for this fleet
        user.fleetAccess = FleetAccess.get(connection, user.id, fleetId);
        user.fleet = Fleet.get(connection, fleetId);

        //do not need to get the fleet as this is called from populateUsers

        return user;
    }

    /**
     * Queries the users preferences from the database
     *
     * @param connection A connection to the mysql database
     * @param userId the userId to query for
     * @param gson is a Gson object to convert to JSON
     *
     * @return an instane of {@link UserPreferences} with all the user's preferences and settings
     */
    public static UserPreferences getUserPreferences(Connection connection, int userId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT decimal_precision FROM user_preferences WHERE user_id = ?");
        query.setInt(1, userId);

        ResultSet resultSet = query.executeQuery();

        int decimalPrecision = 1;

        if (resultSet.next()) {
            decimalPrecision = resultSet.getInt(1);
        }

        boolean optOut = resultSet.getBoolean(2);
        boolean uploadProcess = resultSet.getBoolean(3);
        boolean uploadStatus = resultSet.getBoolean(4);
        boolean criticalEvents = resultSet.getBoolean(5);
        boolean emailError = resultSet.getBoolean(6);
        EmailFrequency frequency = EmailFrequency.valueOf(resultSet.getString(7));

        UserPreferences userPreferences = null;

        query = connection.prepareStatement("SELECT dsn.name FROM user_preferences_metrics AS upm INNER JOIN double_series_names AS dsn ON dsn.id = upm.metric_id WHERE upm.user_id = ? ORDER BY dsn.name");
        query.setInt(1, userId);

        resultSet = query.executeQuery();

        List<String> metricNames = new ArrayList<>();

        while (resultSet.next()) {
            metricNames.add(resultSet.getString(1));
        }

        if (metricNames.isEmpty()) {
            userPreferences = UserPreferences.defaultPreferences(userId);
            storeUserPreferences(connection, userId, userPreferences);
        } else {
            userPreferences = new UserPreferences(userId, decimalPrecision, metricNames, optOut, uploadProcess, uploadStatus, criticalEvents, emailError, frequency);
        }

        return userPreferences;
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection A connection to the mysql database
     * @param userId the userId to update for
     * @param userPreferences the {@link UserPreferences} instance to store
     */
    public static void storeUserPreferences(Connection connection, int userId, UserPreferences userPreferences) throws SQLException {
        String queryString = "INSERT INTO user_preferences (user_id, decimal_precision) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), decimal_precision = VALUES(decimal_precision)";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, userId);
        query.setInt(2, userPreferences.getDecimalPrecision());

        query.executeUpdate();

        for (String metric : userPreferences.getFlightMetrics()) {
            addUserPreferenceMetric(connection, userId, metric);
        }
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection A connection to the mysql database
     * @param userId the userId to update for
     * @param userPreferences the {@link UserPreferences} instance to store
     */
    public static void addUserPreferenceMetric(Connection connection, int userId, String metricName) throws SQLException {
        String queryString = "INSERT INTO user_preferences_metrics (user_id, metric_id) VALUES (?, (SELECT id FROM double_series_names WHERE name = ?))";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, userId);
        query.setString(2, metricName);

        query.executeUpdate();
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection A connection to the mysql database
     * @param userId the userId to update for
     * @param userPreferences the {@link UserPreferences} instance to store
     */
    public static void removeUserPreferenceMetric(Connection connection, int userId, String metricName) throws SQLException {
        String queryString = "DELETE FROM user_preferences_metrics WHERE user_id = ? AND metric_id = (SELECT id FROM double_series_names WHERE name = ?)";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, userId);
        query.setString(2, metricName);

        query.executeUpdate();
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection A connection to the mysql database
     * @param userId the userId to update for
     * @param precision the new decimal precision value to store
     */
    public static UserPreferences updateUserPreferencesPrecision(Connection connection, int userId, int decimalPrecision) throws SQLException {
        String queryString = "UPDATE user_preferences SET decimal_precision = ? WHERE user_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, decimalPrecision);
        query.setInt(2, userId);

        query.executeUpdate();

        return getUserPreferences(connection, userId);
    }

    /**
     * Checks to see if the passphrase provided matches the password reset passphrase for this user
     *
     * @param connection A connection to the mysql database.
     * @param the user's email address
     * @param a generated passphrase for the password reset
     *
     * @return true if the password hashes correctly to the user's password token.
     */
    public static boolean validatePassphrase(Connection connection, String emailAddress, String passphrase) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id FROM user WHERE email = ? AND reset_phrase = ?");
        query.setString(1, emailAddress);
        query.setString(2, passphrase);

        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) return false;
        else return true;
    }


    /**
     * Checks to see if this password validates against the user's password token
     *
     * @param connection A connection to the mysql database.
     * @param password the password to be tested
     *
     * @return true if the password hashes correctly to the user's password token.
     */
    public boolean validate(Connection connection, String password) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT password_token FROM user WHERE id = ?");
        query.setInt(1, id);

        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) return false;
        String passwordToken = resultSet.getString(1);

        return new PasswordAuthentication().authenticate(password.toCharArray(), passwordToken);
    }

    /**
     * Get a user from the database based on the user email and password. The password will be hashed to see
     * if it was correct.
     *
     * @param connection A connection to the mysql database.
     * @param email The user's email.
     * @param password The password entered by the user for login.
     *
     * @exception SQLException if there was a problem with the SQL query.
     * @exception AccountException if the password was incorrect.
     *
     * @return A user object if the password for that email address was correct. null if the user did not exist.
     */
    public static User get(Connection connection, String email, String password) throws SQLException, AccountException {
        PreparedStatement query = connection.prepareStatement("SELECT id, password_token, first_name, last_name, country, state, city, address, phone_number, zip_code, admin, aggregate_view FROM user WHERE email = ?");
        query.setString(1, email);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) return null;

        User user = new User();
        user.id = resultSet.getInt(1);
        user.email = email;
        String passwordToken = resultSet.getString(2);
        user.firstName = resultSet.getString(3);
        user.lastName = resultSet.getString(4);
        user.country = resultSet.getString(5);
        user.state = resultSet.getString(6);
        user.city = resultSet.getString(7);
        user.address = resultSet.getString(8);
        user.phoneNumber = resultSet.getString(9);
        user.zipCode = resultSet.getString(10);
        user.admin = resultSet.getBoolean(11);
        user.aggregateView = resultSet.getBoolean(12);

        if (!new PasswordAuthentication().authenticate(password.toCharArray(), passwordToken)) {
            LOG.info("User password was incorrect.");
            throw new AccountException("Login Error", "Incorrect password.");
        }

        //for now it should be just one user per fleet
        ArrayList<FleetAccess> allFleets = FleetAccess.get(connection, user.getId()); 

        if (allFleets.size() > 1) {
            LOG.severe("ERROR: user had access to multiple fleets (" + allFleets.size() + "), this should never happen (yet)!.");
            System.exit(1);
        } else if (allFleets.size() == 0) {
            LOG.severe("ERROR: user did not have access to ANY fleet. This should never happen.");
            return null;
        }

        user.fleetAccess = allFleets.get(0);

        user.fleet = Fleet.get(connection, user.fleetAccess.getFleetId());

        if (user.fleetAccess.isManager()) {
            user.fleet.populateUsers(connection, user.getId());
        }

        return user;
    }

    /**
     * This is called if the fleet info (like waiting users) is modified, and it will re-calculate
     * the fleet information for a user.
     *
     * @param connection is the connection to the database
     */
    public void updateFleet(Connection connection) throws SQLException {
        fleet = Fleet.get(connection, fleetAccess.getFleetId());
        if (fleetAccess.isManager()) {
            fleet.populateUsers(connection, getId());
        }
    }


    /**
     * Check to see if a user already exists in the database.
     *
     * @param connection A connection to the mysql database.
     * @param email The user email.
     *
     * @exception SQLException if there was a problem with the query or database.
     *
     * @return true if the user exists in the database, false otherwise.
     */
    public static boolean exists(Connection connection, String email) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id FROM user WHERE email = ?");
        query.setString(1, email);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        return resultSet.next(); // returns true if there was something in the result set (i.e., there was a user)
    }


    /**
     * Updates the profile information for a user in the database.
     *
     * @param connection A connection to the mysql database.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param ciy The user's ciy (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     *
     * @exception SQLException if there was a problem with the SQL query or the user already exists in the database.
     */
    public void updateProfile(Connection connection, String firstName, String lastName, String country, String state, String city, String address, String phoneNumber, String zipCode) throws SQLException {

        this.firstName = firstName;
        this.lastName = lastName;
        this.country = country;
        this.state = state;
        this.city = city;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.zipCode = zipCode;

        PreparedStatement query = connection.prepareStatement("UPDATE user SET first_name = ?, last_name = ?, country = ?, state = ?, city = ?, address = ?, phone_number = ?, zip_code = ? WHERE id = ?");
        query.setString(1, this.firstName);
        query.setString(2, this.lastName);
        query.setString(3, this.country);
        query.setString(4, this.state);
        query.setString(5, this.city);
        query.setString(6, this.address);
        query.setString(7, this.phoneNumber);
        query.setString(8, this.zipCode);
        query.setInt(9, this.id);

        LOG.info(query.toString());
        query.executeUpdate();
    }

 
    /**
     * Updates the password for a user in the database.
     *
     * @param connection A connection to the mysql database.
     * @param emailAddress The user's email address.
     * @param password The user's first name (optional, may be null).
     *
     * @exception SQLException if there was a problem with the SQL query or the user already exists in the database.
     */
    public static void updatePassword(Connection connection, String emailAddress, String newPassword) throws SQLException {
        String passwordToken = new PasswordAuthentication().hash(newPassword.toCharArray());

        PreparedStatement query = connection.prepareStatement("UPDATE user SET password_token = ?, reset_phrase = NULL WHERE email = ?");
        query.setString(1, passwordToken);
        query.setString(2, emailAddress);

        LOG.info(query.toString());
        query.executeUpdate();
    }

 
    /**
     * Updates the password for a user in the database.
     *
     * @param connection A connection to the mysql database.
     * @param newPassword the new password
     *
     * @exception SQLException if there was a problem with the SQL query or the user already exists in the database.
     */
    public void updatePassword(Connection connection, String newPassword) throws SQLException {
        String passwordToken = new PasswordAuthentication().hash(newPassword.toCharArray());

        PreparedStatement query = connection.prepareStatement("UPDATE user SET password_token = ? WHERE id = ?");
        query.setString(1, passwordToken);
        query.setInt(2, this.id);

        LOG.info(query.toString());
        query.executeUpdate();
    }


    /**
     * Creates a new user in the database, given the specified parameters. Used by the other static create methods. Note that the {@link #exists(Connection, String)} method should be called prior to calling this method to check and see if the user already exists.
     *
     * @param connection A connection to the mysql database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and stored in the database as a token (not plaintext). Required, may not be null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param city The user's city (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     *
     * @exception SQLException if there was a problem with the SQL query or the user already exists in the database.
     * @exception AccountException if there was an error getting the id of the new user row created in the database.
     *
     * @return A user object if it was sucessfully created and added to the database.
     */
    private static User create(Connection connection, String email, String password, String firstName, String lastName, String country, String state, String city, String address, String phoneNumber, String zipCode) throws SQLException, AccountException {
        //exists should be checked before calling this method
        User user = new User();

        user.email = email;
        String passwordToken = new PasswordAuthentication().hash(password.toCharArray());
        user.firstName = firstName;
        user.lastName = lastName;
        user.country = country;
        user.state = state;
        user.city = city;
        user.address = address;
        user.phoneNumber = phoneNumber;
        user.zipCode = zipCode;

        PreparedStatement query = connection.prepareStatement("INSERT INTO user SET email = ?, password_token = ?, first_name = ?, last_name = ?, country = ?, state = ?, city = ?, address = ?, phone_number = ?, zip_code = ?, registration_time = NOW()", Statement.RETURN_GENERATED_KEYS);
        query.setString(1, user.email);
        query.setString(2, passwordToken);
        query.setString(3, user.firstName);
        query.setString(4, user.lastName);
        query.setString(5, user.country);
        query.setString(6, user.state);
        query.setString(7, user.city);
        query.setString(8, user.address);
        query.setString(9, user.phoneNumber);
        query.setString(10, user.zipCode);

        LOG.info(query.toString());
        query.executeUpdate();

        ResultSet resultSet = query.getGeneratedKeys();
        if (resultSet.next()) {
            user.id = resultSet.getInt(1);
        } else {
            LOG.severe("Database Error: Error happened getting id for new user on database insert.");
            throw new AccountException("Database Error", "Error happened getting id for new user on database insert.");
        }

        return user;
    }

    /**
     * Creates a new user as manager of a new fleet in the database, given the specified parameters.
     *
     * @param connection A connection to the mysql database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and stored in the database as a token (not plaintext). Required, may not be null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param city The user's city (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     * @param fleetName The name of the new fleet. Required, may not be null.
     *
     * @exception SQLException if there was a problem with any SQL queries.
     * @exception AccountException if the user or fleet already existed in the database.
     *
     * @return A user object if it was sucessfully created and added to the database.
     */
    public static User createNewFleetUser(Connection connection, String email, String password, String firstName, String lastName, String country, String state, String city, String address, String phoneNumber, String zipCode, String fleetName) throws SQLException, AccountException {
        //TODO: double check all the passed in strings with regexes to make sure they're valid
        //validateUserInformation(email, password, firstName, lastName, country, state, address, phoneNumber, zipCode, fleetName);

        //check to see if the user already exists in the database
        if (User.exists(connection, email)) {
            throw new AccountException("Account Creation Error", "Could not create account for a user, as a user with the email '" + email + "' already exists in the database.");
        }

        //check and see if the fleet name already exists in the database, if it does NOT then throw an exception
        if (Fleet.exists(connection, fleetName)) {
            throw new AccountException("Account Creation Error", "Could not create account for a user with a new fleet. The fleet '" + fleetName + "' already exists in the database.");
        }

        //create the user in the database
        User user = create(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber, zipCode);

        //create the new fleet
        user.fleet = Fleet.create(connection, fleetName);

        //give use manager access of this fleet
        user.fleetAccess = FleetAccess.create(connection, user.getId(), user.fleet.getId(), FleetAccess.MANAGER);

        if (user.fleetAccess.isManager()) {
            user.fleet.populateUsers(connection, user.getId());
        }   

        return user;
    }

    /**
     * Creates a new user in the database and requests access to an already existing fleet, given the specified parameters.
     *
     * @param connection A connection to the mysql database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and stored in the database as a token (not plaintext). Required, may not be null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param city The user's city (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     * @param fleetName The name of the new fleet. Required, may not be null.
     *
     * @exception SQLException if there was a problem with any SQL queries.
     * @exception AccountException if the user already exists in the database or if the fleet does not exist in the database.
     *
     * @return A user object if it was sucessfully created and added to the database.
     */
    public static User createExistingFleetUser(Connection connection, String email, String password, String firstName, String lastName, String country, String state, String city, String address, String phoneNumber, String zipCode, String fleetName) throws SQLException, AccountException {
        //TODO: double check all the passed in strings with regexes to make sure they're valid
        //validateUserInformation(email, password, firstName, lastName, country, state, address, phoneNumber, zipCode, fleetName);

        //check to see if the user already exists in the database
        if (User.exists(connection, email)) {
            throw new AccountException("Account Creation Error", "Could not create account for a user, as a user with the email '" + email + "' already exists in the database.");
        }

        //check and see if the fleet name already exists in the database, if it does NOT then throw an exception
        if (!Fleet.exists(connection, fleetName)) {
            throw new AccountException("Account Creation Error", "Could not create account for a user with an existing fleet. The fleet '" + fleetName + "' does not exist in the database.");
        }

        //create the user in the database
        User user = create(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber, zipCode);

        //set user access to the fleet as awaiting confirmation
        user.fleet = Fleet.get(connection, fleetName);

        //set the user as waiting for access to be approved
        user.fleetAccess = FleetAccess.create(connection, user.getId(), user.fleet.getId(), FleetAccess.WAITING);

        return user;
    }

    public void updateLastLoginTimeStamp(Connection connection) throws SQLException {

        String updateQueryStr = "UPDATE user SET last_login_time = ? WHERE id = ?";

        PreparedStatement query = connection.prepareStatement(updateQueryStr);
        Date currentTimeStamp = new Date();
        query.setTimestamp(1, new Timestamp(currentTimeStamp.getTime()));
        query.setInt(2, this.id);
        LOG.info(query.toString());
        query.executeUpdate();

    }
}

