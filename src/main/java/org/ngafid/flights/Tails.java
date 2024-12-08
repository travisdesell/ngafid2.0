package org.ngafid.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class Tails {
    private static final Logger LOG = Logger.getLogger(Tails.class.getName());

    private static class FleetInstance {
        int fleetId;

        HashMap<String, String> idMap = new HashMap<>(); // tail to systemId
        HashMap<String, String> tailMap = new HashMap<>(); // systemId to tail
        HashMap<String, Boolean> confirmedMap = new HashMap<>(); // systemId to tailConfirmed

        FleetInstance(int fleetId) {
            this.fleetId = fleetId;
        }

        public String getId(Connection connection, String tail) throws SQLException {
            String id = idMap.get(tail);

            if (id != null) {
                return id;

            } else {
                // id wasn't in the hashmap, look it up
                String queryString = "SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?";
                try (PreparedStatement query = connection.prepareStatement(queryString)) {
                    query.setInt(1, fleetId);
                    query.setString(2, tail);

                    try (ResultSet resultSet = query.executeQuery()) {
                        if (resultSet.next()) {
                            // tail existed in the database, return the id
                            String systemId = resultSet.getString(1);
                            idMap.put(tail, systemId);

                            return systemId;
                        } else {
                            // tail did not exist in the database, this should not happen -- return null
                            return null;
                        }
                    }
                }
            }
        }

        public String getTail(Connection connection, String systemId) throws SQLException {
            String tail = tailMap.get(systemId);

            if (tail != null) {
                return tail;
            } else {
                // id wasn't in the hashmap, look it up
                String queryString = "SELECT tail FROM tails WHERE fleet_id = ? AND system_id = ?";
                try (PreparedStatement query = connection.prepareStatement(queryString)) {
                    query.setInt(1, fleetId);
                    query.setString(2, systemId);

                    // LOG.info(query.toString());
                    try (ResultSet resultSet = query.executeQuery()) {
                        if (resultSet.next()) {
                            // tail existed in the database, return the id
                            tail = resultSet.getString(1);
                            tailMap.put(systemId, tail);
                            return tail;
                        } else {
                            // system id did not exist in the database, this should not happen -- return null
                            return null;
                        }
                    }
                }
            }
        }

        public Boolean getConfirmed(Connection connection, String systemId) throws SQLException {
            Boolean confirmed = confirmedMap.get(systemId);

            if (confirmed != null) {
                return confirmed;
            } else {
                // id wasn't in the hashmap, look it up
                String queryString = "SELECT confirmed FROM tails WHERE fleet_id = ? AND system_id = ?";

                try (PreparedStatement query = connection.prepareStatement(queryString)) {
                    query.setInt(1, fleetId);
                    query.setString(2, systemId);

                    LOG.fine(query.toString());
                    try (ResultSet resultSet = query.executeQuery()) {
                        if (resultSet.next()) {
                            LOG.fine("confirmed exists!");
                            // confirmed existed in the database, return the id
                            confirmed = resultSet.getBoolean(1);
                            LOG.fine("confirmed: " + confirmed);

                            confirmedMap.put(systemId, confirmed);
                            return confirmed;

                        } else {
                            LOG.fine("confirmed does not exist!");
                            // system id did not exist in the database, this should not happen -- return null
                            return null;
                        }
                    }
                }
            }
        }
    }

    private static HashMap<Integer, FleetInstance> fleetMaps = new HashMap<>();

    public static void setSuggestedTail(Connection connection, int fleetId, String systemId, String suggestedTail)
            throws SQLException {
        String queryString = """
                    INSERT IGNORE INTO tails SET system_id = ?, fleet_id = ?, tail = ?, confirmed = false
                """;

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setString(1, systemId);
            query.setInt(2, fleetId);
            query.setString(3, suggestedTail == null ? "" : suggestedTail);

            LOG.fine(query.toString());
            query.executeUpdate();
        }
    }

    public static void updateTail(Connection connection, int fleetId, String systemId, String tail)
            throws SQLException {
        String queryString = "UPDATE tails SET tail = ?, confirmed = 1 WHERE fleet_id = ? AND system_id = ?";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setString(1, tail);
            query.setInt(2, fleetId);
            query.setString(3, systemId);

            LOG.fine(query.toString());

            query.executeUpdate();
        }
    }

    public static String getId(Connection connection, int fleetId, String tail) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null)
            fleet = new FleetInstance(fleetId);

        String systemId = fleet.getId(connection, tail);
        return systemId;
    }

    public static String getTail(Connection connection, int fleetId, String systemId) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null)
            fleet = new FleetInstance(fleetId);

        return fleet.getTail(connection, systemId);
    }

    public static Boolean getConfirmed(Connection connection, int fleetId, String systemId) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null)
            fleet = new FleetInstance(fleetId);

        return fleet.getConfirmed(connection, systemId);
    }

    /**
     * Gets an ArrayList of all the tails in the database for the given fleet, creating a Tail object for each.
     *
     * @param connection is a connection to the database
     * @param fleetId    is the fleet for the tails
     *
     * @return an array list of Tail for each tail in this fleet
     */
    public static ArrayList<Tail> getAll(Connection connection, int fleetId) throws SQLException {
        ArrayList<Tail> tails = new ArrayList<>();

        String queryString = "SELECT * FROM tails WHERE fleet_id = " + fleetId + " ORDER BY tail";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {
            while (resultSet.next()) {
                // tail existed in the database, return the id
                tails.add(new Tail(resultSet));
            }

            return tails;
        }
    }

    /**
     * Gets an ArrayList of all the tails in the database, creating a Tail object for each.
     *
     * @param connection is a connection to the database
     *
     * @return an array list of Tail for each tail in this fleet
     */
    public static ArrayList<Tail> getAll(Connection connection) throws SQLException {
        ArrayList<Tail> tails = new ArrayList<>();

        String queryString = "SELECT * FROM tails ORDER BY tail";
        PreparedStatement query = connection.prepareStatement(queryString);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //tail existed in the database, return the id
            tails.add(new Tail(resultSet));
        }
        resultSet.close();
        query.close();

        return tails;
    }

    /**
     * Gets an ArrayList of all the tails in the database, creating a Tail object for each.
     *
     * @param connection is a connection to the database
     *
     * @return an array list of Tail for each tail in this fleet
     */
    public static ArrayList<Tail> getAll(Connection connection) throws SQLException {
        ArrayList<Tail> tails = new ArrayList<>();

        String queryString = "SELECT * FROM tails ORDER BY tail";
        PreparedStatement query = connection.prepareStatement(queryString);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //tail existed in the database, return the id
            tails.add(new Tail(resultSet));
        }
        resultSet.close();
        query.close();

        return tails;
    }

    /**
     * Gets a List of all the tail numbers that are AirSync equipped in the database for the given fleet, as Strings
     *
     * @param connection is a connection to the database
     * @param fleetId    is the fleet for the tails
     *
     * @return an array list of tail numbers for each tail in this fleet
     */
    public static List<Tail> getAirSyncTails(Connection connection, int fleetId) throws SQLException {
        List<Tail> tails = new ArrayList<>();

        String queryString = "SELECT * FROM tails WHERE fleet_id = ? AND airsync_equipped = ?";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);
            query.setBoolean(2, true);

            try (ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    tails.add(new Tail(resultSet));
                }

                return tails;
            }
        }
    }

    /**
     * Gets an ArrayList of all the tail numbers in the database for the given fleet, as Strings
     *
     * @param connection is a connection to the database
     * @param fleetId    is the fleet for the tails
     *
     * @return an array list of tail numbers for each tail in this fleet
     */
    public static ArrayList<String> getAllTails(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> tails = new ArrayList<>();

        String queryString = "SELECT tail FROM tails WHERE fleet_id = " + fleetId + " ORDER BY tail";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {

            while (resultSet.next()) {
                // tail existed in the database, return the id
                String tail = resultSet.getString(1);
                tails.add(tail);
            }

            return tails;
        }
    }

    /**
     * Gets an ArrayList of all the system ids in the database for this fleet, as Strings
     *
     * @param connection is a connection to the database
     * @param fleetId    is the fleet for the tails
     *
     * @return an array list of system ids for each tail in this fleet
     */
    public static ArrayList<String> getAllSystemIds(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> systemIds = new ArrayList<>();

        String queryString = "SELECT system_id FROM tails WHERE fleet_id = " + fleetId + " ORDER BY system_id";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {

            while (resultSet.next()) {
                // systemId existed in the database, return the id
                String systemId = resultSet.getString(1);
                systemIds.add(systemId);
            }

            return systemIds;
        }
    }

    /**
     * @param connection is a connection to the database
     * @param fleetId    is the fleet for the tails
     *
     * @return the number of unconfirmed tails for this fleet
     */
    public static int getUnconfirmedTailsCount(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT count(*) FROM tails WHERE fleet_id = " + fleetId + " AND confirmed = 0";
        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        }
    }

    /**
     * Gets the total number of tails in the NGAFID.
     *
     * @param connection is the database connection
     *
     * @return the number of different tails in the fleet
     */
    public static int getNumberTails(Connection connection) throws SQLException {
        return getNumberTails(connection, 0);
    }

    /**
     * Gets the total number of tails for a fleet.
     *
     * @param connection is the database connection
     * @param fleetId    is the id of the fleet, if the id <= 0 then get tails for the entire NGAFID
     *
     * @return the number of different tails in the fleet
     */
    public static int getNumberTails(Connection connection, int fleetId) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString;
        if (fleetId > 0) {
            queryString = "SELECT count(system_id) FROM tails WHERE fleet_id = " + fleetId;
        } else {
            queryString = "SELECT count(system_id) FROM tails";
        }

        LOG.fine(queryString);

        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {
            LOG.fine(query.toString());

            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public static void removeUnused(Connection connection) throws SQLException {
        String queryString = "DELETE FROM tails WHERE NOT EXISTS (SELECT id FROM flights WHERE flights.system_id = tails.system_id AND flights.fleet_id = tails.fleet_id);";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.executeUpdate();
        }
    }

}
