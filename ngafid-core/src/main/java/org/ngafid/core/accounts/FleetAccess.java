package org.ngafid.core.accounts;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class FleetAccess implements Serializable {
    public static final String MANAGER = "MANAGER";
    public static final String UPLOAD = "UPLOAD";
    public static final String VIEW = "VIEW";
    public static final String WAITING = "WAITING";
    public static final String DENIED = "DENIED";

    protected static final Logger LOG = Logger.getLogger(FleetAccess.class.getName());

    /**
     * user id in the database
     */
    private int userId = -1;

    /**
     * fleet id in the database.
     */
    protected int fleetId = -1;

    /**
     * User's access type to the fleet. Can be "MANAGER", "UPLOAD", "VIEW",
     * "WAITING", or "DENIED"
     */
    private String accessType;

    /**
     * @return the user's id.
     */
    public int getUserId() {
        return userId;
    }

    /**
     * @return the fleet id the user has access to.
     */
    public int getFleetId() {
        return fleetId;
    }

    /**
     * @return the user's access level, see {@link #accessType}.
     */
    public String getAccessType() {
        return accessType;
    }

    /**
     * @return true if the user's access was denied
     */
    boolean isDenied() {
        return accessType.equals(DENIED);
    }

    /**
     * @return true if the user's access is still pending
     */
    boolean isWaiting() {
        return accessType.equals(WAITING);
    }

    /**
     * @return true if the user's access is manager level
     */
    boolean isManager() {
        return accessType.equals(MANAGER);
    }

    /**
     * @return true if the user's access is uploader level
     */
    boolean isUpload() {
        return accessType.equals(UPLOAD);
    }

    /**
     * @return true if the user's access is view level
     */
    boolean isView() {
        return accessType.equals(VIEW);
    }

    /**
     * Black constructor so static methods can create a new object
     */
    private FleetAccess() {
    }

    protected FleetAccess(int fleetId, int userId, String accessType) {
        this.fleetId = fleetId;
        this.userId = userId;
        this.accessType = accessType;
    }

    /**
     * Creates a FleetAccess object from a result set
     *
     * @param resultSet ResultSet object from a database query
     */
    public FleetAccess(ResultSet resultSet) throws SQLException {
        this.userId = resultSet.getInt(1);
        this.fleetId = resultSet.getInt(2);
        this.accessType = resultSet.getString(3);
    }

    /**
     * Gets all entries of a user's fleet access from the database given a user id.
     * Currently a user should only have access to one fleet however this may change
     * in the future.
     *
     * @param connection The database connection.
     * @param userId     The id of the user.
     * @return An array list of all fleet access entries in the database. Can be
     * empty if there are none.
     * @throws SQLException If there was a query/database problem.
     */

    public static ArrayList<FleetAccess> get(Connection connection, int userId) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("SELECT user_id, fleet_id, type FROM fleet_access WHERE user_id = " + userId);
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            ArrayList<FleetAccess> allAccess = new ArrayList<FleetAccess>();

            while (resultSet.next()) {
                allAccess.add(new FleetAccess(resultSet));
            }

            return allAccess;
        }
    }

    /**
     * Gets a user's fleet access from the database given a user id and fleet id.
     *
     * @param connection The database connection.
     * @param userId     The id of the user.
     * @param fleetId    The id of the fleet.
     * @return The fleet access object if it exists in the database, null otherwise.
     * @throws SQLException If there was a query/database problem.
     */
    public static FleetAccess get(Connection connection, int userId, int fleetId) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement(
                        "SELECT type FROM fleet_access WHERE user_id = " + userId + " AND fleet_id = " + fleetId);
             ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            if (!resultSet.next())
                return null;

            return new FleetAccess(fleetId, userId, resultSet.getString(1));
        }
    }

    /**
     * Creates a new fleet access entry in the database.
     *
     * @param connection The database connection.
     * @param userId     The id of the user getting access.
     * @param fleetId    The id of the fleet the user has access to.
     * @param accessType the type of access the user has, specified by
     *                   {@link #accessType}
     * @return A fleet access object if it was successfully created in the database.
     * @throws SQLException     If there was a query/database problem.
     * @throws AccountException If the user already has access to this fleet in
     *                          the database.
     */
    public static FleetAccess create(Connection connection, int userId, int fleetId, String accessType)
            throws SQLException, AccountException {
        if (FleetAccess.get(connection, userId, fleetId) != null) {
            throw new AccountException("Fleet Access Creation Error",
                    "Could not create fleet access '" + accessType + "' for user " + userId + " on fleet " + fleetId
                            + ", beacuse user already has access to that fleet in the database.");
        }

        try (PreparedStatement query = connection
                .prepareStatement("INSERT INTO fleet_access SET user_id = ?, fleet_id = ?, type = ?")) {
            query.setInt(1, userId);
            query.setInt(2, fleetId);
            query.setString(3, accessType);

            LOG.info(query.toString());
            query.executeUpdate();

            return new FleetAccess(fleetId, userId, accessType);
        }
    }

    /**
     * Updates the access type of a user for the specified fleet.
     *
     * @param connection is the mysql database connection
     * @param userId     is the id of the user with access to the fleet
     * @param fleetId    is the fleet id
     * @param accessType is the new access type
     */
    public static void update(Connection connection, int userId, int fleetId, String accessType) throws SQLException {
        try (PreparedStatement query = connection
                .prepareStatement("UPDATE fleet_access SET type = ? WHERE user_id = ? AND fleet_id = ?")) {
            query.setString(1, accessType);
            query.setInt(2, userId);
            query.setInt(3, fleetId);

            LOG.info(query.toString());
            query.executeUpdate();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FleetAccess fa)) return false;

        return this.userId == fa.userId && this.fleetId == fa.fleetId && this.accessType.equals(fa.accessType);
    }
}
