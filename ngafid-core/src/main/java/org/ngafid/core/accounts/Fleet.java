package org.ngafid.core.accounts;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class Fleet implements Serializable {
    private static final Logger LOG = Logger.getLogger(Fleet.class.getName());
    private final String name;
    /**
     * A list of all users who have access (or are requesting access) to this fleet.
     */
    private ArrayList<User> users;
    private int id = -1;

    public Fleet(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Return the number of fleets in the NGAFID
     *
     * @param connection A connection to the mysql database.
     * @return the number of fleets in the NGAFID
     * @throws SQLException if there was a problem with the query or database.
     */
    public static int getNumberFleets(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT count(id) FROM fleet");
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        }
    }

    /**
     * Gets a fleet from the database given a fleet id.
     *
     * @param connection The database connection.
     * @param id         The id of the fleet
     * @return The fleet if it exists in the database, null otherwise
     * @throws SQLException If there was a query/database problem.
     */
    public static Fleet get(Connection connection, int id) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT fleet_name FROM fleet WHERE id = " + id);
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            if (!resultSet.next())
                return null;

            return new Fleet(id, resultSet.getString(1));
        }
    }

    /**
     * Gets a fleet from the database given a fleet id.
     *
     * @param connection The database connection.
     * @param name       The name of the fleet
     * @return The fleet if it exists in the database, null otherwise
     * @throws SQLException If there was a query/database problem.
     */
    public static Fleet get(Connection connection, String name) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT id FROM fleet WHERE fleet_name = ?")) {
            query.setString(1, name);

            LOG.info(query.toString());
            try (ResultSet resultSet = query.executeQuery()) {

                if (!resultSet.next())
                    return null;

                return new Fleet(resultSet.getInt(1), name);
            }
        }
    }

    /**
     * Gets all the fleets in this database
     *
     * @param connection the database connection
     * @return a {@link List} of all the Fleets
     */
    public static List<Fleet> getAllFleets(Connection connection) throws SQLException {
        String queryString = "SELECT id FROM fleet";

        try (PreparedStatement ps = connection.prepareStatement(queryString); ResultSet rs = ps.executeQuery()) {
            List<Fleet> fleets = new ArrayList<>();

            while (rs.next()) {
                fleets.add(get(connection, rs.getInt(1)));
            }

            return fleets;
        }
    }

    /**
     * Checks to see a fleet with the supplied name already exists in the database.
     *
     * @param connection A connection to the mysql database.
     * @param name       The name of the fleet.
     * @return true if the fleet exists in the database, false otherwise.
     * @throws SQLException is thrown if there is a problem with the query or
     *                      database.
     */
    public static boolean exists(Connection connection, String name) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT id FROM fleet WHERE fleet_name = ?")) {
            query.setString(1, name);

            LOG.info(query.toString());
            try (ResultSet resultSet = query.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Creates a new fleet in the database. The {@link #exists(Connection, String)}
     * method should be called prior to this method.
     *
     * @param connection The database connection.
     * @param name       The name of the fleet.
     * @return A fleet object if it was successfully created in the database.
     * @throws SQLException     If there was a query/database problem or the
     *                          fleet does not exist in the database.
     * @throws AccountException If there was an error getting the id of the new
     *                          fleet from the database.
     */
    public static Fleet create(Connection connection, String name) throws SQLException, AccountException {
        // check and see if the fleet already exists in the database, if it does then
        // throw an exception
        try (
                PreparedStatement query = connection.prepareStatement("INSERT INTO fleet SET fleet_name = ?",
                        Statement.RETURN_GENERATED_KEYS)) {
            query.setString(1, name);
            query.executeUpdate();

            LOG.info(query.toString());

            try (ResultSet resultSet = query.getGeneratedKeys()) {
                Fleet fleet = null;

                if (resultSet.next()) {
                    fleet = new Fleet(resultSet.getInt(1), name);
                } else {
                    LOG.severe("Database Error: Could not get id of new fleet from database after insert.");
                    throw new AccountException("Database Error",
                            "Could not get id of new fleet from database after insert.");
                }

                return fleet;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new AccountException("Cannot create name with duplicate name", e.getMessage());
        }
    }

    /**
     * The id of the fleet in the database.
     *
     * @return the id of the fleet.
     */
    public int getId() {
        return id;
    }

    /**
     * @return the name of the fleet.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the number of users waiting for access to this fleet.
     */
    public int getWaitingUserCount() {
        int waitingUserCount = 0;
        for (User user : users) {
            if (user.getFleetAccessType().equals(FleetAccess.WAITING)) {
                waitingUserCount++;
            }
        }
        return waitingUserCount;
    }

    /**
     * Populates the list of users with access to this fleet, except for user with id `populatorId`
     *
     * @param connection  is a connection to the mysql database.
     * @param populatorId is the id of the user whose populating the list of user
     *                    for this fleet. we should not add this user to the list
     *                    (or get them from the database) as it might end up in an
     *                    infinite loop.
     */

    public void populateUsers(Connection connection, int populatorId) throws SQLException {
        LOG.info("populating users with access to fleet '" + name + "'");
        users = new ArrayList<User>();

        // get all the users waiting in this fleet's queue
        try (PreparedStatement query = connection
                .prepareStatement("SELECT user_id FROM fleet_access WHERE fleet_id = ? AND user_id != ?")) {
            query.setInt(1, this.id);
            query.setInt(2, populatorId);

            try (ResultSet resultSet = query.executeQuery()) {

                while (resultSet.next()) {
                    int userId = resultSet.getInt(1);

                    users.add(User.get(connection, userId, this.id));
                }
            }
        }
    }

    /**
     * Used to determine if AirSync-specific features apply to this fleet.
     *
     * @param connection the DBMS connection
     * @return true if there is a tuple in the `airsync_fleet_info` table,
     * this indicates that the fleet is AirSync-ready
     * @throws SQLException if there are DBMS issues
     */
    public boolean hasAirsync(Connection connection) throws SQLException {
        String sql = "SELECT 1 FROM airsync_fleet_info WHERE fleet_id = ?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, this.id);

            try (ResultSet rs = query.executeQuery()) {
                return rs.next();
            }
        }

    }

    public String toString() {
        return "Fleet id: " + this.getId() + " name: " + this.getName() + ";";
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof Fleet otherFleet)) {
            return false;
        }

        return id == otherFleet.getId() && name.equals(otherFleet.getName());
    }
}
