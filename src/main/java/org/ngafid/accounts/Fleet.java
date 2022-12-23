package org.ngafid.accounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class Fleet {
    private static final Logger LOG = Logger.getLogger(Fleet.class.getName());

    private int id = -1;
    String name;

    public Fleet(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * A list of all users who have access (or are requesting access) to this fleet.
     */
    ArrayList<User> users;

    /**
     * The id of the fleet in the database.
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
     * Return the number of fleets in the NGAFID
     *
     * @param connection A connection to the mysql database.
     *
     * @exception SQLException if there was a problem with the query or database.
     *
     * @return the number of fleets in the NGAFID
     */
    public static int getNumberFleets(Connection connection) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT count(id) FROM fleet");

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            int numberFleets = resultSet.getInt(1);
            return numberFleets;
        } else {
            return 0;
        }
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
     * Populates the list of users with access to this fleet
     * 
     * @param connection is a connection to the mysql database.
     * @param populatorId is the id of the user whose populating the list of user for this fleet. we should not add this user to the list (or get them from the database) as it might end up in an infinite loop.
     */

    public void populateUsers(Connection connection, int populatorId) throws SQLException {
        LOG.info("populating users with access to fleet '" + name + "'");
        users = new ArrayList<User>();

        //get all the users waiting in this fleet's queue
        PreparedStatement query = connection.prepareStatement("SELECT user_id FROM fleet_access WHERE fleet_id = ? AND user_id != ?");
        query.setInt(1, this.id);
        query.setInt(2, populatorId);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            int userId = resultSet.getInt(1);

            users.add(User.get(connection, userId, this.id));
        }
    }

    /**
     * Gets a fleet from the database given a fleet id.
     *
     * @param connection The database connection.
     * @param id The id of the fleet
     *
     * @exception SQLException If there was a query/database problem.
     *
     * @return The fleet if it exists in the database, null otherwise
     */
    public static Fleet get(Connection connection, int id) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT fleet_name FROM fleet WHERE id = ?");
        query.setInt(1, id);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) return null;

        return new Fleet(id, resultSet.getString(1));
    }

    /**
     * Gets a fleet from the database given a fleet id.
     *
     * @param connection The database connection.
     * @param name The name of the fleet
     *
     * @exception SQLException If there was a query/database problem.
     *
     * @return The fleet if it exists in the database, null otherwise
     */
    public static Fleet get(Connection connection, String name) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id FROM fleet WHERE fleet_name = ?");
        query.setString(1, name);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) return null;

        return new Fleet(resultSet.getInt(1), name);
    }

    public static List<Fleet> getAirSyncFleets(Connection connection) {
        String queryString = "SELECT fleet_id FROM airsync";

        List<Fleet> fleets = new ArrayList<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(queryString);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                fleets.add(get(connection, rs.getInt(1)));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return fleets;
    }

    /**
     * Gets all the fleets in this database
     *
     * @param connection the database connection
     *
     * @return a {@link List} of all the Fleets
     */
    public static List<Fleet> getAllFleets(Connection connection) {
        String queryString = "SELECT id FROM fleet";

        List<Fleet> fleets = new ArrayList<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(queryString);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                fleets.add(get(connection, rs.getInt(1)));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return fleets;
    }

    /**
     * Checks to see if the fleet already exists in the database.
     *
     * @param connection A connection to the mysql database.
     * @param name The name of the fleet.
     *
     * @exception SQLException is thrown if there is a problem with the query or database.
     *
     * @return true if the fleet exists in the database, false otherwise.
     */

    public static boolean exists(Connection connection, String name) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id FROM fleet WHERE fleet_name = ?");
        query.setString(1, name);

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        return resultSet.next();
    }

    /**
     * Creates a new fleet in the database. The {@link #exists(Connection, String)} method should be called prior to this method.
     *
     * @param connection The database connection.
     * @param name The name of the fleet.
     *
     * @exception SQLException If there was a query/database problem or the fleet does not exist in the database.
     * @exception AccountException If there was an error getting the id of the new fleet from the database.
     *
     * @return A fleet object if it was successfully created in the database.
     */

    public static Fleet create(Connection connection, String name) throws SQLException, AccountException {
        //check and see if the fleet already exists in the database, if it does then throw an exception
        PreparedStatement query = connection.prepareStatement("INSERT INTO fleet SET fleet_name = ?", Statement.RETURN_GENERATED_KEYS);
        query.setString(1, name);

        LOG.info(query.toString());
        query.executeUpdate();

        ResultSet resultSet = query.getGeneratedKeys();

        Fleet fleet = null;

        if (resultSet.next()) {
            fleet = new Fleet(resultSet.getInt(1), name);
        } else {
            LOG.severe("Database Error: Could not get id of new fleet from database after insert.");
            throw new AccountException("Database Error", "Could not get id of new fleet from database after insert.");
        }

        return fleet;
    }

    public String toString() {
        return "Fleet id: " + this.getId() + " name: " + this.getName() + ";";
    }
}
