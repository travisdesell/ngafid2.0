package org.ngafid.flights;

import com.mysql.jdbc.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

public class Airframes {
    private static final Logger LOG = Logger.getLogger(Airframes.class.getName());

    private static HashMap<String,Integer> idMap = new HashMap<>();
    private static HashMap<Integer,String> airframeMap = new HashMap<>();

    private static HashSet<String> fleetAirframes = new HashSet<>();

    public static void setAirframeFleet(Connection connection, int airframeId, int fleetId) throws SQLException {
        String key = airframeId + "-" + fleetId;

        //this was already inserted to the database
        if (fleetAirframes.contains(key)) return; 
        else {
            String queryString = "REPLACE INTO fleet_airframes SET fleet_id = ?, airframe_id = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, fleetId);
            query.setInt(2, airframeId);

            //LOG.info(query.toString());
            query.executeUpdate();
            query.close();

            fleetAirframes.add(key);
        }
    }

    public static int getId(Connection connection, String airframe) throws SQLException {
        Integer id = idMap.get(airframe);

        if (id != null) {
            return id;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT id FROM airframes WHERE airframe = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, airframe);

            //LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //airframe existed in the database, return the id
                int airframeId = resultSet.getInt(1);
                idMap.put(airframe, airframeId);

                resultSet.close();
                query.close();
                return airframeId;

            } else {
                //airframe did not exist in the database, insert it and return it's generated id
                queryString = "INSERT INTO airframes SET airframe = ?";
                query = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS);
                query.setString(1, airframe);

                //LOG.info(query.toString());
                query.executeUpdate();
                resultSet.close();

                resultSet = query.getGeneratedKeys();
                resultSet.next();

                int airframeId = resultSet.getInt(1);
                idMap.put(airframe, airframeId);

                resultSet.close();
                query.close();

                return airframeId;
            }
        }
    }

    public static String getAirframe(Connection connection, int airframeId) throws SQLException {
        String airframe = airframeMap.get(airframeId);

        if (airframe != null) {
            return airframe;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT airframe FROM airframes WHERE id = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, airframeId);

            //LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //airframe existed in the database, return the id
                airframe = resultSet.getString(1);
                airframeMap.put(airframeId, airframe);

                resultSet.close();
                query.close();
                return airframe;

            } else {
                //airframe id did not exist in the database, this should not happen -- return null
                return null;
            }
        }
    }


    public static ArrayList<String> getAll(Connection connection, int fleetId) throws SQLException {
        ArrayList<String> airframes = new ArrayList<>();

        String queryString = "SELECT airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //airframe existed in the database, return the id
            String airframe = resultSet.getString(1);
            airframes.add(airframe);
        }
        resultSet.close();
        query.close();

        return airframes;
    }

    public static ArrayList<String> getAll(Connection connection) throws SQLException {
        ArrayList<String> airframes = new ArrayList<>();

        String queryString = "SELECT airframe FROM airframes ORDER BY airframe";
        PreparedStatement query = connection.prepareStatement(queryString);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //airframe existed in the database, return the id
            String airframe = resultSet.getString(1);
            airframes.add(airframe);
        }
        resultSet.close();
        query.close();

        return airframes;
    }


}
