package org.ngafid.core.accounts;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

public class FleetAccessNamed extends FleetAccess implements Serializable {
    
    private String fleetName;

    public String getFleetName() {
        return fleetName;
    }

    public void updateFleetName(Connection connection) throws SQLException, AccountException {

        fleetName = Fleet.get(connection, fleetId).getName();

    }

    private FleetAccessNamed(int fleetId, int userId, String fleetAccess) throws SQLException, AccountException {
        super(fleetId, userId, fleetAccess);
    }

    /**
     * Creates a FleetAccess object from a result set
     *
     * @param resultSet ResultSet object from a database query
     */
    private FleetAccessNamed(ResultSet resultSet) throws SQLException, AccountException {
        super(resultSet);
    }


    /**
     * Gets all entries of a user's fleet access from the database given a user id.
     *
     * @param connection The database connection.
     * @param userId     The id of the user.
     * @return An array list of all fleet access entries in the database. Can be
     * empty if there are none.
     * @throws SQLException If there was a query/database problem.
     */

    public static ArrayList<FleetAccess> getAllFleetAccessEntries(Connection connection, int userId) throws SQLException {

        String sql = "SELECT user_id, fleet_id, type FROM fleet_access WHERE user_id = ?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {

            query.setInt(1, userId);
            LOG.info(query.toString());

            ArrayList<FleetAccess> allAccess = new ArrayList<>();
            try (ResultSet resultSet = query.executeQuery()) {

                while (resultSet.next()) {
                    try {
                        FleetAccessNamed fleetAccessNamed = new FleetAccessNamed(resultSet);
                        fleetAccessNamed.updateFleetName(connection);
                        allAccess.add(fleetAccessNamed);
                    } catch (AccountException ex) {
                        LOG.log(Level.WARNING, "Account Exception -- Skipping row: {0}", ex.getMessage());
                    }
                }

            }

            return allAccess;

        }

    }



}