package org.ngafid.core.accounts;

import org.apache.commons.lang3.RandomStringUtils;
import org.ngafid.core.flights.Tails;
import org.ngafid.core.util.SendEmail;

import java.io.Serializable;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class User implements Serializable {

    private static final Logger LOG = Logger.getLogger(User.class.getName());

    /**
     * The following variables hold all the user information that is stored in
     * the 'user' table in the database.
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
    
    // 2FA fields
    private boolean twoFactorEnabled = false;
    private String twoFactorSecret;
    private String backupCodes;
    private boolean twoFactorSetupComplete = false;

    private UserEmailPreferences userEmailPreferences;

    /**
     * The following are references to the fleet the user has access to (if it
     * has approved access or management rights to a fleet), and the access
     * level the user has on that fleet.
     */
    private Fleet fleet;
    private FleetAccess fleetAccess;

    private int fleetSelected;

    private User() {
    }

    public User(Connection connection, int id, String email, String firstName, String lastName, String address, String city, String country, String state, String zipCode, String phoneNumber,
            boolean admin, boolean aggregateView, int fleetId, int fleetSelected) throws SQLException, AccountException {
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
        this.fleetSelected = fleetSelected;

        LOG.log(Level.INFO, "Instantiated user with ID: {0}, Fleet ID: {1}, Fleet Access: {2}, Fleet Selected: {3}", new Object[]{this.id, this.fleet.getId(), this.fleetAccess.getAccessType(), this.fleetSelected});

    }

    private User(Connection connection, int fleetId, ResultSet resultSet) throws SQLException, AccountException {
        this(resultSet);

        // get the access level of the user for this fleet
        fleetAccess = FleetAccess.get(connection, id, fleetId);
        fleet = Fleet.get(connection, fleetId);

        LOG.log(Level.INFO, "Instantiated user with ID: {0}, Fleet ID: {1}, Fleet Access: {2}", new Object[]{this.id, this.fleet.getId(), this.fleetAccess.getAccessType()});
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
        fleetSelected = resultSet.getInt(14);

        // 2FA fields 
        twoFactorEnabled = false;
        twoFactorSecret = null;
        backupCodes = null;
        twoFactorSetupComplete = false;
        
        try {
            twoFactorEnabled = resultSet.getBoolean(15);
            twoFactorSecret = resultSet.getString(16);
            backupCodes = resultSet.getString(17);
            twoFactorSetupComplete = resultSet.getBoolean(18);
        } catch (SQLException e) {
            LOG.log(Level.INFO, "2FA columns not found in database schema, using default values: {0}", e.getMessage());
        }
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
     * Checks to see if the user has access to a particular flight. To have
     * access, the user must not be waiting on fleet access, and the user must
     * have access to the fleet of the flight.
     *
     * @param connection A connection to the database.
     * @param flightId the id of the flight.
     * @return true if the user has access to the flight.
     */
    public boolean hasFlightAccess(Connection connection, int flightId) throws SQLException {
        if (fleetAccess.isWaiting() || fleetAccess.isDenied()) {
            return false;
        }

        try (PreparedStatement query = connection
                .prepareStatement("SELECT id FROM flights WHERE fleet_id = " + getFleetId() + " and id = " + flightId); ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            // if there was a flight in the result set then the user had access (i.e., the
            // flight was in the user's fleet).
            return resultSet.next();
        }
    }

    /**
     * Checks to see if the user is a manager of a particular fleet. Typically
     * used to allow access to modify that fleet.
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
     * @param fleetId The fleet ID of all the users.
     * @return the number of users in the NGAFID
     * @throws SQLException if there was a problem with the query or database.
     */
    public static int getNumberUsers(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT count(id) FROM user";

        if (fleetId > 0) {
            queryString = queryString
                    + " INNER JOIN fleet_access ON user.id = fleet_access.user_id AND fleet_access.fleet_id = "
                    + fleetId;
        }

        try (PreparedStatement query = connection.prepareStatement(queryString); ResultSet resultSet = query.executeQuery()) {
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
                    address, phone_number, zip_code, admin, aggregate_view, password_token,
                    fleet_selected,
                    two_factor_enabled, two_factor_secret, backup_codes, two_factor_setup_complete
                FROM
                    user
            """;

    /**
     * Get a user from the database based on the user's id.
     *
     * @param connection A connection to the database.
     * @param userId The user's id.
     * @param fleetId The fleet ID of the user
     * @return A user object for the user with that id, null if the user did not
     * exist.
     * @throws SQLException if there was a problem with the SQL query.
     */
    public static User get(Connection connection, int userId, int fleetId) throws SQLException, AccountException {
        try (PreparedStatement query = connection.prepareStatement(USER_ROW_QUERY + " WHERE id = " + userId); ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            if (!resultSet.next()) {
                return null;
            }

            return new User(connection, fleetId, resultSet);
        }
    }

    /**
     * dd Queries the users preferences from the database
     *
     * @param connection A connection to the database
     * @param userId the userId to query for
     * @return an instance of {@link UserPreferences} with all the user's
     * preferences and settings
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
                "SELECT dsn.name FROM user_preferences_metrics AS upm "
                + "INNER JOIN double_series_names AS dsn ON dsn.id = upm.metric_id WHERE upm.user_id = "
                + userId + " ORDER BY dsn.name"); ResultSet resultSet = query.executeQuery()) {
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
     * @param connection A connection to the database
     * @param userId the userId to update for
     * @param userPreferences the {@link UserPreferences} instance to store
     */
    public static void storeUserPreferences(Connection connection, int userId, UserPreferences userPreferences)
            throws SQLException {
        String queryString = "INSERT INTO user_preferences (user_id, decimal_precision) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), decimal_precision = VALUES(decimal_precision)";

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
     * @param userId the userId to update for
     * @param metricName the metric name to add
     */
    public static void addUserPreferenceMetric(Connection connection, int userId, String metricName)
            throws SQLException {
        String queryString = "INSERT INTO user_preferences_metrics (user_id, metric_id) VALUES "
                + "(?, (SELECT id FROM double_series_names WHERE name = ?))";

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
     * @param userId the userId to update for
     * @param metricName the metric name to remove
     */
    public static void removeUserPreferenceMetric(Connection connection, int userId, String metricName)
            throws SQLException {
        String queryString = "DELETE FROM user_preferences_metrics WHERE user_id = ? AND metric_id = "
                + "(SELECT id FROM double_series_names WHERE name = ?)";

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
     * @param userId the userId to update for
     * @param decimalPrecision the new decimal precision value to store
     * @return an instance of {@link UserPreferences} with all the user's
     * preferences
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
     * @param userId the userId to query for
     * @return an instance of {@link UserPreferences} with all the user's
     * preferences and settings
     */
    public static UserEmailPreferences getUserEmailPreferences(Connection connection, int userId) throws SQLException {

        // Check if the user has any email preferences first...
        try (PreparedStatement queryInitial = connection
                .prepareStatement("SELECT COUNT(*) FROM email_preferences WHERE user_id = " + userId); ResultSet resultSetInitial = queryInitial.executeQuery()) {
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
                .prepareStatement("SELECT email_type, enabled FROM email_preferences WHERE user_id = " + userId); ResultSet resultSet = query.executeQuery()) {
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
     * @param connection A connection to the database
     * @param userId the userId to update for
     * @param emailPreferences the {@link UserEmailPreferences} instance to
     * store
     * @return an instance of {@link UserEmailPreferences} with all the user's
     * email
     */
    public static UserEmailPreferences updateUserEmailPreferences(Connection connection, int userId, Map<String, Boolean> emailPreferences) throws SQLException {

        final String queryString
                = "INSERT INTO email_preferences (user_id, email_type, enabled) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE email_type = VALUES(email_type), enabled = VALUES(enabled)";

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
     * @param connection A connection to the database.
     * @param emailAddress user's email address
     * @param passphrase generated passphrase for the password reset
     * @return true if the password hashes correctly to the user's password
     * token.
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
     * Checks to see if this password validates against the user's password
     * token
     *
     * @param connection A connection to the database.
     * @param password the password to be tested
     * @return true if the password hashes correctly to the user's password
     * token.
     */
    public boolean validate(Connection connection, String password) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("SELECT password_token FROM user WHERE id = " + id); ResultSet resultSet = query.executeQuery()) {
            if (!resultSet.next()) {
                return false;
            }

            return new PasswordAuthentication().authenticate(password.toCharArray(), resultSet.getString(1));
        }
    }

    /**
     * Get a user from the database based on the user email and password. The
     * password will be hashed to see if it was correct.
     *
     * @param connection A connection to the database.
     * @param email The user's email.
     * @param pass The password entered by the user for login.
     * @return A user object if the password for that email address was correct.
     * null if the user did not exist.
     * @throws SQLException if there was a problem with the SQL query.
     * @throws AccountException if the password was incorrect.
     */
    public static User get(Connection connection, String email, String pass) throws SQLException, AccountException {
        User user = User.get(connection, email);

        //User not found -> Null
        if (user == null) {
            LOG.log(Level.WARNING, "Failed to find an account with email: {0}", email);
            return null;
        }

        //Failed to authenticate -> AccountException
        if (!new PasswordAuthentication().authenticate(pass.toCharArray(), user.passwordToken)) {
            LOG.log(Level.INFO, "User password was incorrect for email: {0}", email);
            throw new AccountException("Login Error", "Incorrect password.");
        }

        //Get all the fleets this user has access to
        ArrayList<FleetAccess> allFleets = FleetAccess.getAllFleetAccessEntries(connection, user.getId());

        //User has no access to any fleet -> Null
        if (allFleets.isEmpty()) {
            LOG.severe("ERROR: user did not have access to ANY fleet. This should never happen.");
            return null;
        }

        final int USER_FLEET_SELECTED_NONE = -1;

        //Select this user's currently-selected fleet from the database [user -> fleet_selected]
        int selectedFleetId = USER_FLEET_SELECTED_NONE;
        final String selectedSql = "SELECT fleet_selected FROM user WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(selectedSql)) {
            statement.setInt(1, user.getId());
            LOG.info(statement.toString());
            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    selectedFleetId = resultSet.getInt(1);
                }

            }
        }

        LOG.log(Level.INFO, "Attempting to resolve Fleet Access for user with ID ({0}). Selected Fleet ID: {1}", new Object[]{user.getId(), selectedFleetId});

        FleetAccess fleetAccessResolved = null;

        //User has a selected fleet, see if they have access to it
        if (selectedFleetId != USER_FLEET_SELECTED_NONE) {

            for (FleetAccess fleetAccess : allFleets) {

                //Current fleetAccess matches the selectedFleetId, use it
                if (fleetAccess.getFleetId() == selectedFleetId) {
                    fleetAccessResolved = fleetAccess;
                    break;
                }

            }

            //Failed to find the user's selected fleet in their fleet access list
            if (fleetAccessResolved == null) {
                LOG.log(Level.WARNING, "User's selected fleet ({0}) was not found in their fleet access list.", selectedFleetId);
            }

        }

        //No resolved fleet access, default to the first accessible fleet
        if (fleetAccessResolved == null) {

            fleetAccessResolved = allFleets.get(0);

            LOG.log(Level.INFO, "Defaulting to first fleet in user's fleet access list with ID: {0}", fleetAccessResolved.getFleetId());

            //Update the user's selected fleet in the database
            final String updateSql = "UPDATE user SET fleet_selected = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setInt(1, fleetAccessResolved.getFleetId());
                statement.setInt(2, user.getId());
                LOG.info(statement.toString());
                statement.executeUpdate();
            }

        }

        //Apply the resolved fleet and fleet access to the user
        user.fleetAccess = fleetAccessResolved;
        user.fleet = Fleet.get(connection, fleetAccessResolved.getFleetId());

        LOG.log(
                Level.INFO,
                "Resolved fleet access for user with ID ({0}). Fleet ID: {1} / Access Type: {2}",
                new Object[]{user.getId(), user.fleet.getId(), user.fleetAccess.getAccessType()}
        );

        //Get the email preferences for this user
        user.userEmailPreferences = getUserEmailPreferences(connection, user.getId());

        return user;
    }

    public static User get(Connection connection, String email) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                USER_ROW_QUERY + " WHERE email = ?")) {
            query.setString(1, email);

            try (ResultSet resultSet = query.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new User(resultSet);
            }
        }
    }

    /**
     * Get's the currently-selected fleet ID for this user.
     *
     * @return the currently-selected fleet ID for this user
     */
    public Integer getSelectedFleetId() {
        return fleetSelected;
    }

    /**
     * Attempts to set the user's currently-selected fleet to the fleet with the
     * given ID. Their fleet and fleet access will also be updated to match the
     * newly selected fleet.
     *
     * @param connection is the connection to the database
     * @param fleetId is the ID of the fleet to select
     */
    public void setSelectedFleetId(Connection connection, int fleetId) throws SQLException, AccountException {

        LOG.log(Level.INFO, "Attempting to set selected fleet to fleet with ID: {0} for user with ID: {1}", new Object[]{fleetId, this.id});

        final String sql = "UPDATE user SET fleet_selected = ? WHERE id = ?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, fleetId);
            query.setInt(2, this.id);
            query.executeUpdate();
        }

        //Update fleet selected
        this.fleetSelected = fleetId;

        LOG.log(Level.INFO, "Updated selected fleet to fleet with ID: {0}", fleetId);

        //Update fleet and fleet access
        this.fleet = Fleet.get(connection, fleetId);
        this.fleetAccess = FleetAccess.get(connection, this.id, fleetId);

        LOG.log(Level.INFO, "Updated user's fleet access to fleet with ID: {0} / Access Type: {1}", new Object[]{this.fleet.getId(), this.fleetAccess.getAccessType()});

    }

    /**
     * Attempts to leave whatever the user's currently-selected fleet is. The
     * user will be switched to another available fleet (prioritizing fleets
     * with higher access levels). If a switch is not possible, then the user
     * will not be allowed to leave their current fleet. Managers cannot leave
     * their own fleet, and as an additional redundancy check, the fleet cannot
     * be left without at least one manager remaining.
     *
     * @param connection is the connection to the database
     */
    public void leaveSelectedFleet(Connection connection) throws SQLException {

        final boolean prevAutoCommit = connection.getAutoCommit();

        try {

            connection.setAutoCommit(false);

            final Set<String> fallbackAllowed = new HashSet<>(Arrays.asList(
                FleetAccess.MANAGER, FleetAccess.UPLOAD, FleetAccess.VIEW
            ));

            final int currentFleetId = this.fleet.getId();
            final int userId = this.id;

            LOG.log(
                Level.INFO,
                "Attempting to leave currently-selected fleet with ID: {0} for user with ID: {1}",
                new Object[]{currentFleetId, userId}
            );

            //User cannot be the last Manager of the current fleet
            final String guaranteeManagerSQL = """
                SELECT COUNT(*) FROM fleet_access
                WHERE fleet_id = ? AND type = 'MANAGER' AND user_id <> ?
                FOR UPDATE
            """;

            try (PreparedStatement ps = connection.prepareStatement(guaranteeManagerSQL)) {

                ps.setInt(1, currentFleetId);
                ps.setInt(2, userId);

                try (ResultSet rs = ps.executeQuery()) {

                    //No result, disallow leaving
                    if (!rs.next()) {
                        LOG.severe("Failed to verify manager count; disallowing leave.");
                        throw new SQLException("Unable to leave current fleet: manager verification failed.");
                    }

                    int managerCount = rs.getInt(1);

                    //No other managers, disallow leaving
                    if (managerCount == 0) {
                        LOG.severe("User attempted to leave their selected fleet, but they were the last manager of that fleet. Disallowed.");
                        throw new SQLException("Unable to leave the current fleet: no other managers would remain.");
                    }

                }

            }

            //Pick the next best fleet to switch to (based on access level priority)
            final String findNextFleetSQL = """
                SELECT fleet_id, type
                FROM fleet_access
                WHERE user_id = ? AND fleet_id <> ?
                ORDER BY CASE type
                    WHEN 'MANAGER' THEN 1
                    WHEN 'UPLOAD' THEN 2
                    WHEN 'VIEW' THEN 3
                    WHEN 'WAITING' THEN 4
                    WHEN 'DENIED' THEN 5
                    ELSE 6
                END
                FOR UPDATE
            """;

            Integer nextFleetId = null;
            String nextFleetType = null;
            try (PreparedStatement ps = connection.prepareStatement(findNextFleetSQL)) {

                ps.setInt(1, userId);
                ps.setInt(2, currentFleetId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        nextFleetId = rs.getInt("fleet_id");
                        nextFleetType = rs.getString("type");
                    }

                }

            }

            //Failed to find a next fleet, disallow leaving
            if (nextFleetId == null) {
                LOG.severe("User attempted to leave their selected fleet, but no other fleets were available to switch to. Disallowed.");
                throw new SQLException("Unable to leave the current fleet: no other fleets are available to switch to.");
            }

            //Next fleet found, but it is not an allowed access level to switch to, disallow leaving
            if (!fallbackAllowed.contains(nextFleetType)) {
                LOG.severe("User attempted to leave their selected fleet, but the only other fleets available were not allowed. Disallowed.");
                throw new SQLException("Unable to leave the current fleet: no allowed fallback fleets are available.");
            }

            LOG.log(
                Level.INFO,
                "Found fallback fleet with ID ({0}) and access level ({1}) to switch to after leaving current fleet.",
                new Object[]{nextFleetId, nextFleetType}
            );

            //First, attempt to switch to the chosen next fleet...
            try {
                this.setSelectedFleetId(connection, nextFleetId);
            } catch (AccountException e) {
                LOG.log(Level.SEVERE, "Failed to set selected fleet: {0}", e.getMessage());
                throw new SQLException("Failed to set selected fleet after leaving current fleet.", e);
            }

            //Then, attempt to remove access to the old selected fleet...
            final String removeSQL = "DELETE FROM fleet_access WHERE user_id = ? AND fleet_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(removeSQL)) {
                ps.setInt(1, userId);
                ps.setInt(2, currentFleetId);
                ps.executeUpdate();
            }

            //Operations were successful, commit the transaction
            connection.commit();
            LOG.info("Successfully left the previously-selected fleet.");

        } catch (SQLException e) {

            //Attempt to rollback the transaction
            try {
                connection.rollback();
            } catch (SQLException ignore) { /* ... */}

            throw e;

        } finally {

            //Restore the previous auto-commit state
            try {
                connection.setAutoCommit(prevAutoCommit);
            } catch (SQLException ignore) { /* ... */ }

        }

    }

    /**
     * This is called if the fleet info (like waiting users) is modified, and it
     * will re-calculate the fleet information for a user.
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
     * @param email The user email.
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
     * @param connection A connection to the database.
     * @param newFirstName The user's first name (optional, may be null).
     * @param newLastName The user's last name (optional, may be null).
     * @param newCountry The user's country (optional, may be null).
     * @param newState The user's state (optional, may be null).
     * @param newCity The user's ciy (optional, may be null).
     * @param newAddress The user's address (optional, may be null).
     * @param newPhoneNumber The user's phone number (optional, may be null).
     * @param newZipCode The user's zip code (optional, may be null).
     * @throws SQLException if there was a problem with the SQL query or the
     * user already exists in the database.
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
                "UPDATE user SET first_name = ?, last_name = ?, country = ?, state = ?, city = ?, address = ?, "
                + "phone_number = ?, zip_code = ? WHERE id = ?")) {
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
     * @param connection A connection to the database.
     * @param emailAddress The user's email address.
     * @param newPassword The user's updated password.
     * @throws SQLException if there was a problem with the SQL query or the
     * user already exists in the database.
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
     * @param connection A connection to the database.
     * @param newPassword the new password
     * @throws SQLException if there was a problem with the SQL query or the
     * user already exists in the database.
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
     * Creates a new user in the database, given the specified parameters. Used
     * by the other static create methods. Note that the
     * {@link #exists(Connection, String)} method should be called prior to
     * calling this method to check and see if the user already exists.
     *
     * @param connection A connection to the database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and
     * stored in the database as a token (not plaintext). Required, may not be
     * null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param city The user's city (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     * @return A user object if it was successfully created and added to the
     * database.
     * @throws SQLException if there was a problem with the SQL query or the
     * user already exists in the database.
     * @throws AccountException if there was an error getting the id of the new
     * user row created in the database.
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
                "INSERT INTO user SET email = ?, password_token = ?, first_name = ?, last_name = ?,"
                + " country = ?, state = ?, city = ?, address = ?, phone_number = ?, zip_code = ?,"
                + " registration_time = NOW()", Statement.RETURN_GENERATED_KEYS)) {
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
     * @param connection A connection to the database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and
     * stored in the database as a token (not plaintext). Required, may not be
     * null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param city The user's city (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     * @param fleetName The name of the new fleet. Required, may not be null.
     * @return A user object if it was successfully created and added to the
     * database.
     * @throws SQLException if there was a problem with any SQL queries.
     * @throws AccountException if the user or fleet already existed in the
     * database.
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
     * Creates a new user in the database and requests access to an already
     * existing fleet, given the specified parameters.
     *
     * @param connection A connection to the database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and
     * stored in the database as a token (not plaintext). Required, may not be
     * null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param city The user's city (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     * @param fleetName The name of the new fleet. Required, may not be null.
     * @return A user object if it was successfully created and added to the
     * database.
     * @throws SQLException if there was a problem with any SQL queries.
     * @throws AccountException if the user already exists in the database or if
     * the fleet does not exist in the database.
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

    // 2FA getter and setter methods
    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

    public String getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(String backupCodes) {
        this.backupCodes = backupCodes;
    }

    public boolean isTwoFactorSetupComplete() {
        return twoFactorSetupComplete;
    }

    public void setTwoFactorSetupComplete(boolean twoFactorSetupComplete) {
        this.twoFactorSetupComplete = twoFactorSetupComplete;
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
