package org.ngafid.flights;

import com.mysql.jdbc.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class Tails {
    private static final Logger LOG = Logger.getLogger(Tails.class.getName());

    private static class FleetInstance {
        int fleetId;

        HashMap<String,Integer> idMap = new HashMap<>();
        HashMap<Integer,String> tailMap = new HashMap<>();

        FleetInstance(int fleetId) {
            this.fleetId = fleetId;
        }

        public int getId(Connection connection, String tail) throws SQLException {
            Integer id = idMap.get(tail);

            if (id != null) {
                return id;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT id FROM tails WHERE fleet_id = ? AND tail = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setString(2, tail);

                //LOG.info(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    //tail existed in the database, return the id
                    int tailId = resultSet.getInt(1);
                    idMap.put(tail, tailId);

                    resultSet.close();
                    query.close();

                    return tailId;

                } else {
                    //tail did not exist in the database, insert it and return it's generated id
                    queryString = "INSERT INTO tails SET fleet_id = ?, tail = ?";
                    query = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS);
                    query.setInt(1, fleetId);
                    query.setString(2, tail);

                    //LOG.info(query.toString());
                    query.executeUpdate();

                    resultSet = query.getGeneratedKeys();
                    resultSet.next();

                    int tailId = resultSet.getInt(1);
                    idMap.put(tail, tailId);

                    resultSet.close();
                    query.close();

                    return tailId;
                }
            }
        }

        public String getTail(Connection connection, int tailId) throws SQLException {
            String tail = tailMap.get(tailId);

            if (tail != null) {
                return tail;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT tail FROM tails WHERE fleet_id = ? AND id = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setInt(2, tailId);

                //LOG.info(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    //tail existed in the database, return the id
                    tail = resultSet.getString(1);
                    tailMap.put(tailId, tail);
                    resultSet.close();
                    query.close();
                    return tail;

                } else {
                    //tail id did not exist in the database, this should not happen -- return null
                    resultSet.close();
                    query.close();
                    return null;
                }
            }
        }
    }

    private static HashMap<Integer, FleetInstance> fleetMaps = new HashMap<>();

    public static int getId(Connection connection, int fleetId, String tail) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        int tailId = fleet.getId(connection, tail);
        return tailId;
    }

    public static String getTail(Connection connection, int fleetId, int tailId) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        String tail = fleet.getTail(connection, tailId);
        return tail;
    }

    public static ArrayList<String> getAll(Connection connection, int fleetId) throws SQLException {
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


    public static void removeUnused(Connection connection) throws SQLException {
        String queryString = "DELETE FROM tails WHERE NOT EXISTS (SELECT id FROM flights WHERE flights.tail_id = tails.id AND flights.fleet_id = tails.fleet_id);";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.executeUpdate();
        query.close();
    }
}
