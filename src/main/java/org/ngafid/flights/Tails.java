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

        HashMap<String,String> idMap = new HashMap<>(); //tail to systemId
        HashMap<String,String> tailMap = new HashMap<>(); //systemId to tail
        HashMap<String,Boolean> confirmedMap = new HashMap<>(); //systemId to tailConfirmed

        FleetInstance(int fleetId) {
            this.fleetId = fleetId;
        }

        public String getId(Connection connection, String tail) throws SQLException {
            String id = idMap.get(tail);

            if (id != null) {
                return id;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setString(2, tail);

                //LOG.info(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    //tail existed in the database, return the id
                    String systemId = resultSet.getString(1);
                    idMap.put(tail, systemId);

                    resultSet.close();
                    query.close();

                    return systemId;

                } else {
                    //tail did not exist in the database, this should not happen -- return null
                    resultSet.close();
                    query.close();
                    return null;
                }
            }
        }

        public String getTail(Connection connection, String systemId) throws SQLException {
            String tail = tailMap.get(systemId);

            if (tail != null) {
                return tail;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT tail FROM tails WHERE fleet_id = ? AND system_id = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setString(2, systemId);

                //LOG.info(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    //tail existed in the database, return the id
                    tail = resultSet.getString(1);
                    tailMap.put(systemId, tail);
                    resultSet.close();
                    query.close();
                    return tail;

                } else {
                    //system id did not exist in the database, this should not happen -- return null
                    resultSet.close();
                    query.close();
                    return null;
                }
            }
        }

        public Boolean getConfirmed(Connection connection, String systemId) throws SQLException {
            Boolean confirmed = confirmedMap.get(systemId);

            if (confirmed != null) {
                return confirmed;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT confirmed FROM tails WHERE fleet_id = ? AND system_id = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setString(2, systemId);

                LOG.fine(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    LOG.fine("confirmed exists!");
                    //confirmed existed in the database, return the id
                    confirmed = resultSet.getBoolean(1);
                    LOG.fine("confirmed: " + confirmed);

                    confirmedMap.put(systemId, confirmed);
                    resultSet.close();
                    query.close();
                    return confirmed;

                } else {
                    LOG.fine("confirmed does not exist!");
                    //system id did not exist in the database, this should not happen -- return null
                    resultSet.close();
                    query.close();
                    return null;
                }
            }
        }
    }

    private static HashMap<Integer, FleetInstance> fleetMaps = new HashMap<>();

    public static void setSuggestedTail(Connection connection, int fleetId, String systemId, String suggestedTail) throws SQLException {
        //check to see if this tail entry exists
        //if it does, do nothing
        //otherwise, add this to tails with confirmed = false

        String queryString = "SELECT tail, confirmed FROM tails WHERE fleet_id = ? AND system_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        query.setString(2, systemId);

        LOG.fine(query.toString());
        ResultSet resultSet = query.executeQuery();

        String tail = null;
        boolean confirmed = false;
        if (resultSet.next()) {
            //system id existed in the database, get its tail number and if it was confirmed
            tail = resultSet.getString(1);
            confirmed = resultSet.getBoolean(2);
            LOG.fine("tail was not in db!");
        }
        resultSet.close();
        query.close();

        //tail was not set in the database, set it with a suggested value since we have one
        if (tail == null) {
            queryString = "INSERT INTO tails SET tail = ?, fleet_id = ?, system_id = ?, confirmed = false";
            query = connection.prepareStatement(queryString);
            if (suggestedTail == null) {
                query.setString(1, "");
            } else {
                // TODO: This is a hack. Probably should change the DB or change how suggestedTail is generated
                if (suggestedTail.length() > 16)
                    suggestedTail = suggestedTail.substring(0, 16);
                query.setString(1, suggestedTail);
            }
            query.setInt(2, fleetId);
            query.setString(3, systemId);

            LOG.fine("suggestedTail = '" + suggestedTail + "'");
            LOG.fine(query.toString());
            query.executeUpdate();

            query.close();
        }
    }

    public static void updateTail(Connection connection, int fleetId, String systemId, String tail) throws SQLException {
        String queryString = "UPDATE tails SET tail = ?, confirmed = 1 WHERE fleet_id = ? AND system_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setString(1, tail);
        query.setInt(2, fleetId);
        query.setString(3, systemId);

        LOG.fine(query.toString());

        query.executeUpdate();

        query.close();
    }


    public static String getId(Connection connection, int fleetId, String tail) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        String systemId = fleet.getId(connection, tail);
        return systemId;
    }

    public static String getTail(Connection connection, int fleetId, String systemId) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        return fleet.getTail(connection, systemId);
    }

    public static Boolean getConfirmed(Connection connection, int fleetId, String systemId) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        return fleet.getConfirmed(connection, systemId);
    }

    /**
     * Gets an ArrayList of all the tails in the database for the given fleet, creating a Tail object for each.
     *
     * @param connection is a connection to the database
     * @param fleetId is the fleet for the tails
     *
     * @return an array list of Tail for each tail in this fleet
     */
    public static ArrayList<Tail> getAll(Connection connection, int fleetId) throws SQLException {
        ArrayList<Tail> tails = new ArrayList<>();

        String queryString = "SELECT * FROM tails WHERE fleet_id = ? ORDER BY tail";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

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
     * @param fleetId is the fleet for the tails
     *
     * @return an array list of tail numbers for each tail in this fleet
     */
    public static List<Tail> getAirSyncTails(Connection connection, int fleetId) throws SQLException {
        List<Tail> tails = new ArrayList<>();

        String queryString = "SELECT * FROM tails WHERE fleet_id = ? AND airsync_equipped = ?";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, fleetId);
        query.setBoolean(2, true);

        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            tails.add(new Tail(resultSet));
        }

        resultSet.close();
        query.close();

        return tails;
    }

    /**
     * Gets an ArrayList of all the tail numbers in the database for the given fleet, as Strings
     *
     * @param connection is a connection to the database
     * @param fleetId is the fleet for the tails
     *
     * @return an array list of tail numbers for each tail in this fleet
     */
    public static ArrayList<String> getAllTails(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> tails = new ArrayList<>();

        String queryString = "SELECT tail FROM tails WHERE fleet_id = ? ORDER BY tail";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //tail existed in the database, return the id
            String tail = resultSet.getString(1);
            tails.add(tail);
        }
        resultSet.close();
        query.close();

        return tails;
    }

    /**
     * Gets an ArrayList of all the system ids in the database for this fleet, as Strings
     *
     * @param connection is a connection to the database
     * @param fleetId is the fleet for the tails
     *
     * @return an array list of system ids for each tail in this fleet
     */
    public static ArrayList<String> getAllSystemIds(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> systemIds = new ArrayList<>();

        String queryString = "SELECT system_id FROM tails WHERE fleet_id = ? ORDER BY system_id";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //systemId existed in the database, return the id
            String systemId = resultSet.getString(1);
            systemIds.add(systemId);
        }
        resultSet.close();
        query.close();

        return systemIds;
    }


    /**
     * @param connection is a connection to the database
     * @param fleetId is the fleet for the tails
     *
     * @return the number of unconfirmed tails for this fleet
     */
    public static int getUnconfirmedTailsCount(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT count(*) FROM tails WHERE fleet_id = ? AND confirmed = 0";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        int count = 0;
        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            count = resultSet.getInt(1);
        }
        resultSet.close();
        query.close();

        return count;
    }

    /**
     *  Gets the total number of tails in the NGAFID.
     *
     *  @param connection is the database connection
     *
     *  @return the number of different tails in the fleet
     */
    public static int getNumberTails(Connection connection) throws SQLException {
        return getNumberTails(connection, 0);
    }


    /**
     *  Gets the total number of tails for a fleet.
     *
     *  @param connection is the database connection
     *  @param fleetId is the id of the fleet, if the id <= 0 then get tails for the entire NGAFID
     *
     *  @return the number of different tails in the fleet
     */
    public static int getNumberTails(Connection connection, int fleetId) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString;
        if (fleetId > 0) {
            queryString = "SELECT count(system_id) FROM tails WHERE fleet_id = ?";
        } else {
            queryString = "SELECT count(system_id) FROM tails";
        }

        LOG.fine(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        if (fleetId > 0) query.setInt(1, fleetId);

        LOG.fine(query.toString());
        ResultSet resultSet = query.executeQuery();

        resultSet.next();
        int numberTails = resultSet.getInt(1);
        System.out.println("number of tails is: " + numberTails);

        resultSet.close();
        query.close();

        return numberTails;
    }


    public static void removeUnused(Connection connection) throws SQLException {
        String queryString = "DELETE FROM tails WHERE NOT EXISTS (SELECT id FROM flights WHERE flights.system_id = tails.system_id AND flights.fleet_id = tails.fleet_id);";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.executeUpdate();
        query.close();
    }


}
