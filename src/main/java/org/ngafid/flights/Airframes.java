package org.ngafid.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class Airframes {
    private static final Logger LOG = Logger.getLogger(Airframes.class.getName());

    private static class FleetInstance {
        int fleetId;

        HashMap<String,Integer> idMap = new HashMap<>();
        HashMap<Integer,String> airframeMap = new HashMap<>();

        FleetInstance(int fleetId) {
            this.fleetId = fleetId;
        }

        public int getId(Connection connection, String airframe) throws SQLException {
            Integer id = idMap.get(airframe);

            if (id != null) {
                return id;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT id FROM airframes WHERE fleet_id = ? AND airframe = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setString(2, airframe);

                //LOG.info(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    //airframe existed in the database, return the id
                    int airframeId = resultSet.getInt(1);
                    idMap.put(airframe, airframeId);
                    return airframeId;

                } else {
                    //airframe did not exist in the database, insert it and return it's generated id
                    queryString = "INSERT INTO airframes SET fleet_id = ?, airframe = ?";
                    query = connection.prepareStatement(queryString);
                    query.setInt(1, fleetId);
                    query.setString(2, airframe);

                    //LOG.info(query.toString());
                    query.executeUpdate();

                    resultSet = query.getGeneratedKeys();
                    resultSet.next();

                    int airframeId = resultSet.getInt(1);
                    idMap.put(airframe, airframeId);

                    return airframeId;
                }
            }
        }

        public String getAirframe(Connection connection, int airframeId) throws SQLException {
            String airframe = airframeMap.get(airframeId);

            if (airframe != null) {
                return airframe;

            } else {
                //id wasn't in the hashmap, look it up
                String queryString = "SELECT airframe FROM airframes WHERE fleet_id = ? AND id = ?";
                PreparedStatement query = connection.prepareStatement(queryString);
                query.setInt(1, fleetId);
                query.setInt(2, airframeId);

                //LOG.info(query.toString());
                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    //airframe existed in the database, return the id
                    airframe = resultSet.getString(1);
                    airframeMap.put(airframeId, airframe);
                    return airframe;

                } else {
                    //airframe id did not exist in the database, this should not happen -- return null
                    return null;
                }
            }
        }
    }

    private static HashMap<Integer, FleetInstance> fleetMaps = new HashMap<>();

    public static int getId(Connection connection, int fleetId, String airframe) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        int airframeId = fleet.getId(connection, airframe);
        return airframeId;
    }

    public static String getAirframe(Connection connection, int fleetId, int airframeId) throws SQLException {
        FleetInstance fleet = fleetMaps.get(fleetId);
        if (fleet == null) fleet = new FleetInstance(fleetId);

        String name = fleet.getAirframe(connection, airframeId);
        return name;
    }

    public static ArrayList<String> getAll(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> airframes = new ArrayList<>();

        String queryString = "SELECT airframe FROM airframes WHERE fleet_id = ? ORDER BY airframe";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //airframe existed in the database, return the id
            String airframe = resultSet.getString(1);
            airframes.add(airframe);
        }

        return airframes;
    }

}
