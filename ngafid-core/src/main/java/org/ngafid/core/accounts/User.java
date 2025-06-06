package org.ngafid.core.accounts;

import org.apache.commons.lang3.RandomStringUtils;
import org.ngafid.core.flights.Tails;
import org.ngafid.core.util.SendEmail;

import java.io.Serializable;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.logging.Logger;


public final class User implements Serializable {
    private static final Logger LOG = Logger.getLogger(User.class.getName());

    /**
     * The following variables hold all the user information that is stored in the
     * 'user'
     * table in the database.
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
    private String passwordToken;

    private UserEmailPreferences userEmailPreferences;

    /**
     * The following are references to the fleet the user has access to (if it has
     * approved access
     * or management rights to a fleet), and the access level the user has on that
     * fleet.
     */
    private Fleet fleet;
    private FleetAccess fleetAccess;

    private User() {
    }

    public User(Connection connection, int id, String email, String firstName, String lastName, String address, String city, String country, String state, String zipCode, String phoneNumber,
                boolean admin, boolean aggregateView, int fleetId) throws SQLException, AccountException {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.country = country;
        this.state = state;
        this.city = city;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.zipCode = zipCode;
        this.admin = admin;
        this.aggregateView = aggregateView;
        this.passwordToken = "";

        this.fleet = Fleet.get(connection, fleetId);
        this.fleetAccess = FleetAccess.get(connection, id, fleetId);
    }

    private User(Connection connection, int fleetId, ResultSet resultSet) throws SQLException, AccountException {
        this(resultSet);

        // get the access level of the user for this fleet
        fleetAccess = FleetAccess.get(connection, id, fleetId);
        fleet = Fleet.get(connection, fleetId);
    }

    private User(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        email = resultSet.getString(2);
        firstName = resultSet.getString(3);
        lastName = resultSet.getString(4);
        country = resultSet.getString(5);
        state = resultSet.getString(6);
        city = resultSet.getString(7);
        address = resultSet.getString(8);
        phoneNumber = resultSet.getString(9);
        zipCode = resultSet.getString(10);
        admin = resultSet.getBoolean(11);
        aggregateView = resultSet.getBoolean(12);
        passwordToken = resultSet.getString(13);
    }

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
     * @param connection A connection to the database.
     * @return the user's email preferences
     */
    public UserEmailPreferences getUserEmailPreferences(Connection connection) throws SQLException {
        return User.getUserEmailPreferences(connection, id);
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
    public int getWaitingUserCount(Connection connection) throws SQLException {
        return fleet.getWaitingUserCount(connection);
    }

    /**
     * @param connection A connection to the db
     * @return the number of unconfirmed tails for this user's fleet
     */
    public int getUnconfirmedTailsCount(Connection connection) throws SQLException {
        return Tails.getUnconfirmedTailsCount(connection, fleet.getId());
    }

    /**
     * Checks to see if the user has access to a particular flight. To have access,
     * the user must not be waiting on fleet access, and the user must have access
     * to the fleet of the flight.
     *
     * @param connection A connection to the database.
     * @param flightId   the id of the flight.
     * @return true if the user has access to the flight.
     */
    public boolean hasFlightAccess(Connection connection, int flightId) throws SQLException {
        if (fleetAccess.isWaiting() || fleetAccess.isDenied())
            return false;

        try (PreparedStatement query = connection
                .prepareStatement("SELECT id FROM flights WHERE fleet_id = " + getFleetId() + " and id = " + flightId);
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            // if there was a flight in the result set then the user had access (i.e., the
            // flight was in the user's fleet).
            return resultSet.next();
        }
    }

    /**
     * Checks to see if the user is a manager of a particular fleet. Typically used
     * to allow access to modify that fleet.
     *
     * @param fleetId the fleet the access check is being made on
     * @return true if the user has access to that fleet.
     */
    public boolean managesFleet(int fleetId) {
        return fleet.getId() == fleetId && fleetAccess.isManager();
    }

    /**
     * Checks to see if the user can upload flights for a fleet.
     *
     * @param fleetId the fleet the access check is being made on
     * @return true if the user has access to that fleet.
     */
    public boolean hasUploadAccess(int fleetId) {
        return fleet.getId() == fleetId && (fleetAccess.isManager() || fleetAccess.isUpload());
    }

    /**
     * Checks to see if the user can view flights for a fleet.
     *
     * @param fleetId the fleet the access check is being made on
     * @return true if the user has access to that fleet.
     */
    public boolean hasViewAccess(int fleetId) {
        return fleet.getId() == fleetId && (fleetAccess.isManager() || fleetAccess.isUpload() || fleetAccess.isView());
    }

    /**
     * Return the number of users in the NGAFID
     *
     * @param connection A connection to the database.
     * @param fleetId    The fleet ID of all the users.
     * @return the number of users in the NGAFID
     * @throws SQLException if there was a problem with the query or database.
     */
    public static int getNumberUsers(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT count(id) FROM user";

        if (fleetId > 0)
            queryString = queryString
                    + " INNER JOIN fleet_access ON user.id = fleet_access.user_id AND fleet_access.fleet_id = "
                    + fleetId;

        try (PreparedStatement query = connection.prepareStatement(queryString);
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            if (resultSet.next()) {
                int numberUsers = resultSet.getInt(1);
                return numberUsers;
            } else {
                return 0;
            }
        }
    }

    private static final String USER_ROW_QUERY = """
                SELECT
                    id, email, first_name, last_name, country, state, city,
                    address, phone_number, zip_code, admin, aggregate_view, password_token
                FROM
                    user
            """;

    /**
     * Get a user from the database based on the user's id.
     *
     * @param connection A connection to the database.
     * @param userId     The user's id.
     * @param fleetId    The fleet ID of the user
     * @return A user object for the user with that id, null if the user did not
     * exist.
     * @throws SQLException if there was a problem with the SQL query.
     */
    public static User get(Connection connection, int userId, int fleetId) throws SQLException, AccountException {
        try (PreparedStatement query = connection.prepareStatement(USER_ROW_QUERY + " WHERE id = " + userId);
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            if (!resultSet.next())
                return null;

            return new User(connection, fleetId, resultSet);
        }
    }

    /**
     * dd
     * Queries the users preferences from the database
     *
     * @param connection A connection to the database
     * @param userId     the userId to query for
     * @return an instance of {@link UserPreferences} with all the user's preferences
     * and settings
     */
    public static UserPreferences getUserPreferences(Connection connection, int userId) throws SQLException {
        int decimalPrecision = 1;
        try (PreparedStatement query = connection
                .prepareStatement("SELECT decimal_precision FROM user_preferences WHERE user_id = ?")) {

            query.setInt(1, userId);
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                decimalPrecision = resultSet.getInt(1);
            }
        }

        UserPreferences userPreferences = null;

        try (PreparedStatement query = connection.prepareStatement(
                "SELECT dsn.name FROM user_preferences_metrics AS upm " +
                        "INNER JOIN double_series_names AS dsn ON dsn.id = upm.metric_id WHERE upm.user_id = "
                        + userId + " ORDER BY dsn.name");
             ResultSet resultSet = query.executeQuery()) {
            List<String> metricNames = new ArrayList<>();

            while (resultSet.next()) {
                metricNames.add(resultSet.getString(1));
            }

            if (metricNames.isEmpty()) {
                userPreferences = UserPreferences.defaultPreferences(userId);
                storeUserPreferences(connection, userId, userPreferences);
            } else {
                userPreferences = new UserPreferences(userId, decimalPrecision, metricNames);
            }

            return userPreferences;
        }
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection      A connection to the database
     * @param userId          the userId to update for
     * @param userPreferences the {@link UserPreferences} instance to store
     */
    public static void storeUserPreferences(Connection connection, int userId, UserPreferences userPreferences)
            throws SQLException {
        String queryString = "INSERT INTO user_preferences (user_id, decimal_precision) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), decimal_precision = VALUES(decimal_precision)";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, userId);
            query.setInt(2, userPreferences.getDecimalPrecision());

            query.executeUpdate();

            for (String metric : userPreferences.getFlightMetrics()) {
                addUserPreferenceMetric(connection, userId, metric);
            }
        }
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection A connection to the database
     * @param userId     the userId to update for
     * @param metricName the metric name to add
     */
    public static void addUserPreferenceMetric(Connection connection, int userId, String metricName)
            throws SQLException {
        String queryString = "INSERT INTO user_preferences_metrics (user_id, metric_id) VALUES " +
                "(?, (SELECT id FROM double_series_names WHERE name = ?))";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, userId);
            query.setString(2, metricName);

            query.executeUpdate();
        }
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection A connection to the database
     * @param userId     the userId to update for
     * @param metricName the metric name to remove
     */
    public static void removeUserPreferenceMetric(Connection connection, int userId, String metricName)
            throws SQLException {
        String queryString = "DELETE FROM user_preferences_metrics WHERE user_id = ? AND metric_id = " +
                "(SELECT id FROM double_series_names WHERE name = ?)";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, userId);
            query.setString(2, metricName);

            query.executeUpdate();
        }
    }

    /**
     * Updates the users preferences in the database
     *
     * @param connection       A connection to the database
     * @param userId           the userId to update for
     * @param decimalPrecision the new decimal precision value to store
     * @return an instance of {@link UserPreferences} with all the user's preferences
     */
    public static UserPreferences updateUserPreferencesPrecision(Connection connection, int userId,
                                                                 int decimalPrecision) throws SQLException {
        String queryString = "UPDATE user_preferences SET decimal_precision = ? WHERE user_id = ?";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, decimalPrecision);
            query.setInt(2, userId);

            query.executeUpdate();
        }

        return getUserPreferences(connection, userId);
    }

    /**
     * Queries the user email preferences from the database
     *
     * @param connection A connection to the database
     * @param userId     the userId to query for
     * @return an instance of {@link UserPreferences} with all the user's preferences
     * and settings
     */
    public static UserEmailPreferences getUserEmailPreferences(Connection connection, int userId) throws SQLException {

        // Check if the user has any email preferences first...
        try (PreparedStatement queryInitial = connection
                .prepareStatement("SELECT COUNT(*) FROM email_preferences WHERE user_id = " + userId);
             ResultSet resultSetInitial = queryInitial.executeQuery()) {
            int emailPreferencesCount = 0;

            if (resultSetInitial.next()) {
                emailPreferencesCount = resultSetInitial.getInt(1);
            }

            int emailTypeCountExpected = EmailType.getEmailTypeCountNonForced();

            LOG.info("Checking email preferences for user with ID (" + userId + ")... Expected: "
                    + emailTypeCountExpected
                    + " / Actual: " + emailPreferencesCount);

            // The user mismatched email preference count, reinsert
            if (emailPreferencesCount != emailTypeCountExpected) {
                LOG.severe(
                        "User with ID (" + userId
                                + ") has a mismatched number of email types, attempting reinsertion...");
                EmailType.insertEmailTypesIntoDatabase(connection, userId);
            }
        }

        // Get the user's email preferences...
        try (PreparedStatement query = connection
                .prepareStatement("SELECT email_type, enabled FROM email_preferences WHERE user_id = " + userId);
             ResultSet resultSet = query.executeQuery()) {
            HashMap<String, Boolean> emailPreferences = new HashMap<>();

            while (resultSet.next()) {
                String emailType = resultSet.getString(1);
                boolean enabled = resultSet.getBoolean(2);

                emailPreferences.put(emailType, enabled);
            }

            return new UserEmailPreferences(userId, emailPreferences);
        }
    }

    /**
     * Updates the user email preferences in the database
     *
     * @param connection       A connection to the database
     * @param userId           the userId to update for
     * @param emailPreferences the {@link UserEmailPreferences} instance to store
     * @return an instance of {@link UserEmailPreferences} with all the user's email
     */
    public static UserEmailPreferences updateUserEmailPreferences(Connection connection, int userId, Map<String, Boolean> emailPreferences) throws SQLException {

        final String queryString =
            "INSERT INTO email_preferences (user_id, email_type, enabled) VALUES (?, ?, ?)"
            + " ON DUPLICATE KEY UPDATE email_type = VALUES(email_type), enabled = VALUES(enabled)"
        ;

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            for (Map.Entry<String, Boolean> entry : emailPreferences.entrySet()) {
                query.setInt(1, userId);
                query.setString(2, entry.getKey());
                query.setBoolean(3, entry.getValue());

                query.addBatch();
            }

            query.executeBatch();

            return User.getUserEmailPreferences(connection, userId);
        }
    }

    public void setEmailPreferences(UserEmailPreferences updatedEmailPreferences) {
        this.userEmailPreferences = updatedEmailPreferences;
    }

    /**
     * Checks to see if the passphrase provided matches the password reset
     * passphrase for this user
     *
     * @param connection   A connection to the database.
     * @param emailAddress user's email address
     * @param passphrase   generated passphrase for the password reset
     * @return true if the password hashes correctly to the user's password token.
     */
    public static boolean validatePassphrase(Connection connection, String emailAddress, String passphrase)
            throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("SELECT id FROM user WHERE email = ? AND reset_phrase = ?")) {
            query.setString(1, emailAddress);
            query.setString(2, passphrase);

            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Checks to see if this password validates against the user's password token
     *
     * @param connection A connection to the database.
     * @param password   the password to be tested
     * @return true if the password hashes correctly to the user's password token.
     */
    public boolean validate(Connection connection, String password) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("SELECT password_token FROM user WHERE id = " + id);
             ResultSet resultSet = query.executeQuery()) {
            if (!resultSet.next())
                return false;

            return new PasswordAuthentication().authenticate(password.toCharArray(), resultSet.getString(1));
        }
    }

    /**
     * Get a user from the database based on the user email and password. The
     * password will be hashed to see
     * if it was correct.
     *
     * @param connection A connection to the database.
     * @param email      The user's email.
     * @param pass       The password entered by the user for login.
     * @return A user object if the password for that email address was correct.
     * null if the user did not exist.
     * @throws SQLException     if there was a problem with the SQL query.
     * @throws AccountException if the password was incorrect.
     */
    public static User get(Connection connection, String email, String pass) throws SQLException, AccountException {
        User user = User.get(connection, email);

        if (user == null) {
            return null;
        }

        if (!new PasswordAuthentication().authenticate(pass.toCharArray(), user.passwordToken)) {
            LOG.info("User password was incorrect.");
            throw new AccountException("Login Error", "Incorrect password.");
        }

        // for now, it should be just one user per fleet
        ArrayList<FleetAccess> allFleets = FleetAccess.get(connection, user.getId());

        if (allFleets.size() > 1) {
            LOG.severe("ERROR: user had access to multiple fleets (" + allFleets.size()
                    + "), this should never happen (yet)!.");
            throw new AccountException("Fleet Error", "User is associated with multiple fleets");
        } else if (allFleets.isEmpty()) {
            LOG.severe("ERROR: user did not have access to ANY fleet. This should never happen.");
            return null;
        }

        user.fleetAccess = allFleets.get(0);

        user.fleet = Fleet.get(connection, user.fleetAccess.getFleetId());

        // Get the email preferences for this user
        user.userEmailPreferences = getUserEmailPreferences(connection, user.getId());

        return user;
    }


    public static User get(Connection connection, String email) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                USER_ROW_QUERY + " WHERE email = ?")) {
            query.setString(1, email);

            try (ResultSet resultSet = query.executeQuery()) {
                if (!resultSet.next())
                    return null;

                return new User(resultSet);
            }
        }
    }

    /**
     * This is called if the fleet info (like waiting users) is modified, and it
     * will re-calculate
     * the fleet information for a user.
     *
     * @param connection is the connection to the database
     */
    public void updateFleet(Connection connection) throws SQLException, AccountException {
        fleet = Fleet.get(connection, fleetAccess.getFleetId());
    }

    /**
     * Check to see if a user already exists in the database.
     *
     * @param connection A connection to the database.
     * @param email      The user email.
     * @return true if the user exists in the database, false otherwise.
     * @throws SQLException if there was a problem with the query or database.
     */
    public static boolean exists(Connection connection, String email) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT id FROM user WHERE email = ?")) {
            query.setString(1, email);
            try (ResultSet resultSet = query.executeQuery()) {
                LOG.info(query.toString());

                return resultSet.next();
            }
        }
    }

    /**
     * Updates the profile information for a user in the database.
     *
     * @param connection     A connection to the database.
     * @param newFirstName   The user's first name (optional, may be null).
     * @param newLastName    The user's last name (optional, may be null).
     * @param newCountry     The user's country (optional, may be null).
     * @param newState       The user's state (optional, may be null).
     * @param newCity        The user's ciy (optional, may be null).
     * @param newAddress     The user's address (optional, may be null).
     * @param newPhoneNumber The user's phone number (optional, may be null).
     * @param newZipCode     The user's zip code (optional, may be null).
     * @throws SQLException if there was a problem with the SQL query or the user
     *                      already exists in the database.
     */
    public void updateProfile(Connection connection, String newFirstName, String newLastName,
                              String newCountry, String newState, String newCity, String newAddress,
                              String newPhoneNumber, String newZipCode) throws SQLException {

        this.firstName = newFirstName;
        this.lastName = newLastName;
        this.country = newCountry;
        this.state = newState;
        this.city = newCity;
        this.address = newAddress;
        this.phoneNumber = newPhoneNumber;
        this.zipCode = newZipCode;

        try (PreparedStatement query = connection.prepareStatement(
                "UPDATE user SET first_name = ?, last_name = ?, country = ?, state = ?, city = ?, address = ?, " +
                        "phone_number = ?, zip_code = ? WHERE id = ?")) {
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
    }

    /**
     * Updates the password for a user in the database.
     *
     * @param connection   A connection to the database.
     * @param emailAddress The user's email address.
     * @param newPassword  The user's updated password.
     * @throws SQLException if there was a problem with the SQL query or the user
     *                      already exists in the database.
     */
    public static void updatePassword(Connection connection, String emailAddress, String newPassword)
            throws SQLException {
        String passwordToken = new PasswordAuthentication().hash(newPassword.toCharArray());

        try (PreparedStatement query = connection
                .prepareStatement("UPDATE user SET password_token = ?, reset_phrase = NULL WHERE email = ?")) {
            query.setString(1, passwordToken);
            query.setString(2, emailAddress);

            LOG.info(query.toString());
            query.executeUpdate();
        }
    }

    public static void updateResetPhrase(Connection connection, String email, String resetPhrase) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("UPDATE user SET reset_phrase = ? WHERE email = ?")) {
            query.setString(1, resetPhrase);
            query.setString(2, email);
            LOG.info(query.toString());
            query.executeUpdate();
        }
    }

    /**
     * Updates the password for a user in the database.
     *
     * @param connection  A connection to the database.
     * @param newPassword the new password
     * @throws SQLException if there was a problem with the SQL query or the user
     *                      already exists in the database.
     */
    public void updatePassword(Connection connection, String newPassword) throws SQLException {
        String passwordToken = new PasswordAuthentication().hash(newPassword.toCharArray());

        try (PreparedStatement qry = connection.prepareStatement("UPDATE user SET password_token = ? WHERE id = ?")) {
            qry.setString(1, passwordToken);
            qry.setInt(2, this.id);

            LOG.info(qry.toString());
            qry.executeUpdate();
        }
    }

    /**
     * Creates a new user in the database, given the specified parameters. Used by
     * the other static create methods. Note that the
     * {@link #exists(Connection, String)} method should be called prior to calling
     * this method to check and see if the user already exists.
     *
     * @param connection  A connection to the database.
     * @param email       The user's email. Required, may not be null.
     * @param password    The password entered by the user. This is hashed and
     *                    stored in the database as a token (not plaintext).
     *                    Required, may not be null.
     * @param firstName   The user's first name (optional, may be null).
     * @param lastName    The user's last name (optional, may be null).
     * @param country     The user's country (optional, may be null).
     * @param state       The user's state (optional, may be null).
     * @param city        The user's city (optional, may be null).
     * @param address     The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode     The user's zip code (optional, may be null).
     * @return A user object if it was successfully created and added to the
     * database.
     * @throws SQLException     if there was a problem with the SQL query or the
     *                          user already exists in the database.
     * @throws AccountException if there was an error getting the id of the new
     *                          user row created in the database.
     */
    //CHECKSTYLE:OFF
    private static User create(Connection connection, String email, String password, String firstName, String lastName,
                               String country, String state, String city, String address, String phoneNumber, String zipCode)
            throws SQLException, AccountException {
        //CHECKSTYLE:ON
        // exists should be checked before calling this method
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

        try (PreparedStatement query = connection.prepareStatement(
                "INSERT INTO user SET email = ?, password_token = ?, first_name = ?, last_name = ?," +
                        " country = ?, state = ?, city = ?, address = ?, phone_number = ?, zip_code = ?," +
                        " registration_time = NOW()", Statement.RETURN_GENERATED_KEYS)) {
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

            try (ResultSet resultSet = query.getGeneratedKeys()) {
                resultSet.next();
                user.id = resultSet.getInt(1);
            }
        }

        return user;
    }

    /**
     * Creates a new user as manager of a new fleet in the database, given the
     * specified parameters.
     *
     * @param connection  A connection to the database.
     * @param email       The user's email. Required, may not be null.
     * @param password    The password entered by the user. This is hashed and
     *                    stored in the database as a token (not plaintext).
     *                    Required, may not be null.
     * @param firstName   The user's first name (optional, may be null).
     * @param lastName    The user's last name (optional, may be null).
     * @param country     The user's country (optional, may be null).
     * @param state       The user's state (optional, may be null).
     * @param city        The user's city (optional, may be null).
     * @param address     The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode     The user's zip code (optional, may be null).
     * @param fleetName   The name of the new fleet. Required, may not be null.
     * @return A user object if it was successfully created and added to the
     * database.
     * @throws SQLException     if there was a problem with any SQL queries.
     * @throws AccountException if the user or fleet already existed in the
     *                          database.
     */
    //CHECKSTYLE:OFF
    public static User createNewFleetUser(Connection connection, String email, String password, String firstName,
                                          String lastName, String country, String state, String city, String address, String phoneNumber,
                                          String zipCode, String fleetName) throws SQLException, AccountException {
        //CHECKSTYLE:ON
        // TODO: double check all the passed in strings with regexes to make sure
        // they're valid
        // validateUserInformation(email, password, firstName, lastName, country, state,
        // address, phoneNumber, zipCode, fleetName);

        // check to see if the user already exists in the database
        if (User.exists(connection, email)) {
            throw new AccountException("Account Creation Error",
                    "Could not create account for a user, as a user with the email '" + email
                            + "' already exists in the database.");
        }

        // check and see if the fleet name already exists in the database, if it does
        // NOT then throw an exception
        if (Fleet.exists(connection, fleetName)) {
            throw new AccountException("Account Creation Error",
                    "Could not create account for a user with a new fleet. The fleet '" + fleetName
                            + "' already exists in the database.");
        }

        // create the user in the database
        User user = create(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber,
                zipCode);

        // create the new fleet
        user.fleet = Fleet.create(connection, fleetName);

        // give use manager access of this fleet
        user.fleetAccess = FleetAccess.create(connection, user.getId(), user.fleet.getId(), FleetAccess.MANAGER);

        return user;
    }

    /**
     * Creates a new user in the database and requests access to an already existing
     * fleet, given the specified parameters.
     *
     * @param connection  A connection to the database.
     * @param email       The user's email. Required, may not be null.
     * @param password    The password entered by the user. This is hashed and
     *                    stored in the database as a token (not plaintext).
     *                    Required, may not be null.
     * @param firstName   The user's first name (optional, may be null).
     * @param lastName    The user's last name (optional, may be null).
     * @param country     The user's country (optional, may be null).
     * @param state       The user's state (optional, may be null).
     * @param city        The user's city (optional, may be null).
     * @param address     The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode     The user's zip code (optional, may be null).
     * @param fleetName   The name of the new fleet. Required, may not be null.
     * @return A user object if it was successfully created and added to the
     * database.
     * @throws SQLException     if there was a problem with any SQL queries.
     * @throws AccountException if the user already exists in the database or if
     *                          the fleet does not exist in the database.
     */
    //CHECKSTYLE:OFF
    public static User createExistingFleetUser(Connection connection, String email, String password, String firstName,
                                               String lastName, String country, String state, String city, String address, String phoneNumber,
                                               String zipCode, String fleetName) throws SQLException, AccountException {
        //CHECKSTYLE:ON
        // TODO: double check all the passed in strings with regexes to make sure
        // they're valid
        // validateUserInformation(email, password, firstName, lastName, country, state,
        // address, phoneNumber, zipCode, fleetName);

        // check to see if the user already exists in the database
        if (User.exists(connection, email)) {
            throw new AccountException("Account Creation Error",
                    "Could not create account for a user, as a user with the email '" + email
                            + "' already exists in the database.");
        }

        // check and see if the fleet name already exists in the database, if it does
        // NOT then throw an exception
        if (!Fleet.exists(connection, fleetName)) {
            throw new AccountException("Account Creation Error",
                    "Could not create account for a user with an existing fleet. The fleet '" + fleetName
                            + "' does not exist in the database.");
        }

        // create the user in the database
        User user = create(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber,
                zipCode);

        // set user access to the fleet as awaiting confirmation
        user.fleet = Fleet.get(connection, fleetName);

        // set the user as waiting for access to be approved
        user.fleetAccess = FleetAccess.create(connection, user.getId(), user.fleet.getId(), FleetAccess.WAITING);

        return user;
    }

    public static void sendPasswordResetEmail(Connection connection, String email) throws SQLException {
        int resetPhraseLength = 10;
        boolean useLetters = true;
        boolean useDigits = true;
        String resetPhrase = RandomStringUtils.random(resetPhraseLength, useLetters, useDigits);
        updateResetPhrase(connection, email, resetPhrase);
        String resetPasswordUrl = "https://ngafid.org/reset_password?resetPhrase=" + resetPhrase;
        System.out.println("Reset Password URl : " + resetPasswordUrl);
        ArrayList<String> recipients = new ArrayList<>();
        recipients.add(email);
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<p>Hi,<p><br>");
        body.append("<p>A password reset was requested for your account<p>");
        body.append("<p>Please click the below link to change your password.<p>");
        body.append("<p> Password Reset Link : <a href=")
                .append(resetPasswordUrl)
                .append(">Reset Password</a></p><br>");
        body.append("</body></html>");
        ArrayList<String> bccRecipients = new ArrayList<>();
        SendEmail.sendEmail(recipients, bccRecipients, "NGAFID Password Reset Information", body.toString(),
                EmailType.PASSWORD_RESET);
    }

    public void updateLastLoginTimeStamp(Connection connection) throws SQLException {
        String updateQueryStr = "UPDATE user SET last_login_time = ? WHERE id = ?";
        try (PreparedStatement query = connection.prepareStatement(updateQueryStr)) {
            Date currentTimeStamp = new Date();
            query.setTimestamp(1, new Timestamp(currentTimeStamp.getTime()));
            query.setInt(2, this.id);

            LOG.info(query.toString());
            query.executeUpdate();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User u)) {
            return false;
        }

        return this.id == u.id && this.email.equals(u.email) && this.firstName.equals(u.firstName) && this.lastName.equals(u.lastName) && this.country.equals(u.country) && this.state.equals(u.state)
                && this.city.equals(u.city) && this.address.equals(u.address) & this.phoneNumber.equals(u.phoneNumber) && this.zipCode.equals(u.zipCode) && this.admin == u.admin && this.aggregateView == u.aggregateView
                && this.fleetAccess.equals(u.fleetAccess) && this.fleet.equals(u.fleet);
    }
}
