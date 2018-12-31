package org.ngafid.accounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.logging.Logger;


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
    private String address;
    private String phoneNumber;
    private String zipCode;

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
     * Checks to see if the user has access to a particular flight. To have access, the user must not be waiting on fleet access, and the user must have access to the fleet of the flight.
     * @param flightId the id of the flight.
     *
     * @returns true if the user has access to the flight.
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
        PreparedStatement query = connection.prepareStatement("SELECT id, email, first_name, last_name, country, state, address, phone_number, zip_code FROM user WHERE id = ?");
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
        user.address = resultSet.getString(7);
        user.phoneNumber = resultSet.getString(8);
        user.zipCode = resultSet.getString(8);

        //get the access level of the user for this fleet
        user.fleetAccess = FleetAccess.get(connection, user.id, fleetId);

        //do not need to get the fleet as this is called from populateUsers

        return user;
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
        PreparedStatement query = connection.prepareStatement("SELECT id, password_token, first_name, last_name, country, state, address, phone_number, zip_code FROM user WHERE email = ?");
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
        user.address = resultSet.getString(7);
        user.phoneNumber = resultSet.getString(8);
        user.zipCode = resultSet.getString(8);

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
     * Creates a new user in the database, given the specified parameters. Used by the other static create methods. Note that the {@link #exists(Connection, String)} method should be called prior to calling this method to check and see if the user already exists.
     *
     * @param connection A connection to the mysql database.
     * @param email The user's email. Required, may not be null.
     * @param password The password entered by the user. This is hashed and stored in the database as a token (not plaintext). Required, may not be null.
     * @param firstName The user's first name (optional, may be null).
     * @param lastName The user's last name (optional, may be null).
     * @param country The user's country (optional, may be null).
     * @param state The user's state (optional, may be null).
     * @param address The user's address (optional, may be null).
     * @param phoneNumber The user's phone number (optional, may be null).
     * @param zipCode The user's zip code (optional, may be null).
     *
     * @exception SQLException if there was a problem with the SQL query or the user already exists in the database.
     * @exception AccountException if there was an error getting the id of the new user row created in the database.
     *
     * @return A user object if it was sucessfully created and added to the database.
     */
    private static User create(Connection connection, String email, String password, String firstName, String lastName, String country, String state, String address, String phoneNumber, String zipCode) throws SQLException, AccountException {
        //exists should be checked before calling this method
        User user = new User();

        user.email = email;
        String passwordToken = new PasswordAuthentication().hash(password.toCharArray());
        user.firstName = firstName;
        user.lastName = lastName;
        user.country = country;
        user.state = state;
        user.address = address;
        user.phoneNumber = phoneNumber;
        user.zipCode = zipCode;

        PreparedStatement query = connection.prepareStatement("INSERT INTO user SET email = ?, password_token = ?, first_name = ?, last_name = ?, country = ?, state = ?, address = ?, phone_number = ?, zip_code = ?");
        query.setString(1, user.email);
        query.setString(2, passwordToken);
        query.setString(3, user.firstName);
        query.setString(4, user.lastName);
        query.setString(5, user.country);
        query.setString(6, user.state);
        query.setString(7, user.address);
        query.setString(8, user.phoneNumber);
        query.setString(9, user.zipCode);

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
    public static User createNewFleetUser(Connection connection, String email, String password, String firstName, String lastName, String country, String state, String address, String phoneNumber, String zipCode, String fleetName) throws SQLException, AccountException {
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
        User user = create(connection, email, password, firstName, lastName, country, state, address, phoneNumber, zipCode);

        //create the new fleet
        user.fleet = Fleet.create(connection, fleetName);

        //give use manager access of this fleet
        user.fleetAccess = FleetAccess.create(connection, user.getId(), user.fleet.getId(), FleetAccess.MANAGER);

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
    public static User createExistingFleetUser(Connection connection, String email, String password, String firstName, String lastName, String country, String state, String address, String phoneNumber, String zipCode, String fleetName) throws SQLException, AccountException {
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
        User user = create(connection, email, password, firstName, lastName, country, state, address, phoneNumber, zipCode);

        //set user access to the fleet as awaiting confirmation
        user.fleet = Fleet.get(connection, fleetName);

        //set the user as waiting for access to be approved
        user.fleetAccess = FleetAccess.create(connection, user.getId(), user.fleet.getId(), FleetAccess.WAITING);

        return user;
    }
}

