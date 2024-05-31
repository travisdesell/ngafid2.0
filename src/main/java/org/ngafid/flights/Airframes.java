package org.ngafid.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

public class Airframes {
    private static final Logger LOG = Logger.getLogger(Airframes.class.getName());

    private static HashMap<String,Integer> nameIdMap = new HashMap<>();
    private static HashMap<Integer,String> airframeNameMap = new HashMap<>();
    private static HashMap<String,Integer> typeIdMap = new HashMap<>();
    private static HashMap<Integer,String> airframeTypeMap = new HashMap<>();

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

    public static int getNameId(Connection connection, String airframeName) throws SQLException {
        Integer id = nameIdMap.get(airframeName);

        if (id != null) {
            return id;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT id FROM airframes WHERE airframe = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, airframeName);

            //LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //airframe existed in the database, return the id
                int airframeNameId = resultSet.getInt(1);
                nameIdMap.put(airframeName, airframeNameId);

                resultSet.close();
                query.close();
                return airframeNameId;

            } else {
                //airframe did not exist in the database, insert it and return it's generated id
                queryString = "INSERT INTO airframes SET airframe = ?";
                query = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS);
                query.setString(1, airframeName);

                //LOG.info(query.toString());
                query.executeUpdate();
                resultSet.close();

                resultSet = query.getGeneratedKeys();
                resultSet.next();

                int airframeNameId = resultSet.getInt(1);
                nameIdMap.put(airframeName, airframeNameId);

                resultSet.close();
                query.close();

                return airframeNameId;
            }
        }
    }

    public static String getAirframeName(Connection connection, int airframeNameId) throws SQLException {
        String airframeName = airframeNameMap.get(airframeNameId);

        if (airframeName != null) {
            return airframeName;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT airframe FROM airframes WHERE id = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, airframeNameId);

            //LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //airframeName existed in the database, return the id
                airframeName = resultSet.getString(1);
                airframeNameMap.put(airframeNameId, airframeName);

                resultSet.close();
                query.close();
                return airframeName;

            } else {
                //airframeName id did not exist in the database, this should not happen -- return null
                return null;
            }
        }
    }

    public static int getTypeId(Connection connection, String airframeType) throws SQLException {
        Integer id = typeIdMap.get(airframeType);

        if (id != null) {
            return id;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT id FROM airframe_types WHERE name = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, airframeType);

            //LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //airframe existed in the database, return the id
                int airframeTypeId = resultSet.getInt(1);
                typeIdMap.put(airframeType, airframeTypeId);

                resultSet.close();
                query.close();
                return airframeTypeId;

            } else {
                System.err.println("ERROR: tried to look up airframe type '" + airframeType + "' and it was not in the database.");
                System.err.println("Please update the airframe_types table with this new airframe type");
                System.exit(1);
                return -1;

                /*
                //airframe did not exist in the database, insert it and return it's generated id
                queryString = "INSERT INTO airframe_types SET name = ?";
                query = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS);
                query.setString(1, airframeType);

                //LOG.info(query.toString());
                query.executeUpdate();
                resultSet.close();

                resultSet = query.getGeneratedKeys();
                resultSet.next();

                int airframeTypeId = resultSet.getInt(1);
                typeIdMap.put(airframeType, airframeTypeId);

                resultSet.close();
                query.close();

                return airframeTypeId;
                */
            }
        }
    }

    public static String getAirframeType(Connection connection, int airframeTypeId) throws SQLException {
        String airframeType = airframeTypeMap.get(airframeTypeId);

        if (airframeType != null) {
            return airframeType;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT name FROM airframe_types WHERE id = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, airframeTypeId);

            //LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //airframeType existed in the database, return the id
                airframeType = resultSet.getString(1);
                airframeTypeMap.put(airframeTypeId, airframeType);

                resultSet.close();
                query.close();
                return airframeType;

            } else {
                //airframeType id did not exist in the database, this should not happen -- return null
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


    /**
     * Queries the database for all airframe names and their ids and returns a
     * hashmap of them.
     *
     * @param connection is a connection to the database
     * @return a HashMap of all airframe name ids to their names
     */

    public static HashMap<Integer, String> getIdToNameMap(Connection connection) throws SQLException {
        HashMap<Integer,String> idToNameMap = new HashMap<Integer,String>();

        String queryString = "SELECT id, airframe FROM airframes ORDER BY id";
        PreparedStatement query = connection.prepareStatement(queryString);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //airframe existed in the database, return the id
            int id = resultSet.getInt(1);
            String airframe = resultSet.getString(2);
            idToNameMap.put(id, airframe);
        }
        resultSet.close();
        query.close();


        return idToNameMap;
    }

    /**
     * Queries the database for all airframe names and their ids and returns a
     * hashmap of them.
     *
     * @param connection is a connection to the database
     * @param fleetId is the id of the fleet for the airframes
     * @return a HashMap of all airframe name ids to their names
     */

    public static HashMap<Integer,String> getIdToNameMap(Connection connection, int fleetId) throws SQLException {
        HashMap<Integer,String> idToNameMap = new HashMap<Integer,String>();

        String queryString = "SELECT id, airframe FROM airframes INNER JOIN fleet_airframes ON airframes.id = fleet_airframes.airframe_id WHERE fleet_airframes.fleet_id = ? ORDER BY airframe";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        //LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            //airframe existed in the database, return the id
            int id = resultSet.getInt(1);
            String airframe = resultSet.getString(2);
            idToNameMap.put(id, airframe);
        }
        resultSet.close();
        query.close();


        return idToNameMap;
    }


}
