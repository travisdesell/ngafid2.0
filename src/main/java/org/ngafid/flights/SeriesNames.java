package org.ngafid.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.HashMap;


public class SeriesNames {
    private static HashMap<Integer, String> doubleIdToName = new HashMap<Integer, String>();
    private static HashMap<String, Integer> doubleNameToId = new HashMap<String, Integer>();

    private static HashMap<Integer, String> stringIdToName = new HashMap<Integer, String>();
    private static HashMap<String, Integer> stringNameToId = new HashMap<String, Integer>();

    public static String getStringName(Connection connection, int id) throws SQLException {
        String name = stringIdToName.get(id);

        if (name == null) {
            //query the database for the name
            String queryString = "SELECT name FROM string_series_names WHERE id = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, id);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                name = resultSet.getString(1);

                //add it to *both* hashmaps, to save other lookups
                stringIdToName.put(id, name);
                stringNameToId.put(name, id);

            } else {
                //there was no name for this given id, this should never happen
                System.err.println("ERROR: tried to get a string series column name for id '" + id + "', but this id is not in the database. This should never happen.");
                System.exit(1);
            }

            resultSet.close();
            query.close();
        }

        return name;
    }

    public static int getStringNameId(Connection connection, String name) throws SQLException {
        Integer id = stringNameToId.get(name);

        if (id == null) {
            //query the database for the name
            String queryString = "SELECT id FROM string_series_names WHERE name = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, name);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                id = resultSet.getInt(1);

                //add it to *both* hashmaps, to save other lookups
                stringIdToName.put(id, name);
                stringNameToId.put(name, id);

            } else {
                //create a new entry in the data_type_names table for this
                //previously unseen name

                String insertQueryString = "INSERT INTO string_series_names SET name = ?";
                PreparedStatement insertQuery = connection.prepareStatement(insertQueryString, Statement.RETURN_GENERATED_KEYS);
                insertQuery.setString(1, name);

                insertQuery.executeUpdate();

                ResultSet insertResultSet = insertQuery.getGeneratedKeys();
                insertResultSet.next();

                id = insertResultSet.getInt(1);

                //now that it was added to the database, add the name and id
                //to *both* hashmaps to save other lookups
                stringIdToName.put(id, name);
                stringNameToId.put(name, id);

                insertResultSet.close();
                insertQuery.close();
            }

            resultSet.close();
            query.close();
        }

        return id;
    }

    public static String getDoubleName(Connection connection, int id) throws SQLException {
        String name = doubleIdToName.get(id);

        if (name == null) {
            //query the database for the name
            String queryString = "SELECT name FROM double_series_names WHERE id = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, id);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                name = resultSet.getString(1);

                //add it to *both* hashmaps, to save other lookups
                doubleIdToName.put(id, name);
                doubleNameToId.put(name, id);

            } else {
                //there was no name for this given id, this should never happen
                System.err.println("ERROR: tried to get a double series column name for id '" + id + "', but this id is not in the database. This should never happen.");
                System.exit(1);
            }

            resultSet.close();
            query.close();
        }

        return name;
    }

    public static int getDoubleNameId(Connection connection, String name) throws SQLException {
        Integer id = doubleNameToId.get(name);

        if (id == null) {
            //query the database for the name
            String queryString = "SELECT id FROM double_series_names WHERE name = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, name);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                id = resultSet.getInt(1);

                //add it to *both* hashmaps, to save other lookups
                doubleIdToName.put(id, name);
                doubleNameToId.put(name, id);

            } else {
                //create a new entry in the data_type_names table for this
                //previously unseen name

                String insertQueryString = "INSERT INTO double_series_names SET name = ?";
                PreparedStatement insertQuery = connection.prepareStatement(insertQueryString, Statement.RETURN_GENERATED_KEYS);
                insertQuery.setString(1, name);

                insertQuery.executeUpdate();

                ResultSet insertResultSet = insertQuery.getGeneratedKeys();
                insertResultSet.next();

                id = insertResultSet.getInt(1);

                //now that it was added to the database, add the name and id
                //to *both* hashmaps to save other lookups
                doubleIdToName.put(id, name);
                doubleNameToId.put(name, id);

                insertResultSet.close();
                insertQuery.close();
             }

            resultSet.close();
            query.close();
        }

        return id;
    }


}
