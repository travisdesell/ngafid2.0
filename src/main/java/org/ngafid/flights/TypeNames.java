package org.ngafid.flights;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.HashMap;



public class TypeNames {
    //keep HashMaps of the names to ids and ids to names so we can reduce
    //database access and speed things up
    private static HashMap<Integer, String> idToName = new HashMap<Integer, String>();
    private static HashMap<String, Integer> nameToId = new HashMap<String, Integer>();

    /**
     * Get a type name (for either a double or string series) given a particular id
     *
     * @param connection is the connection to the database
     * @param id is the id of the type name
     */
    public static String getName(Connection connection, int id) throws SQLException {
        String name = idToName.get(id);

        if (name == null) {
            //query the database for the name
            String queryString = "SELECT name FROM data_type_names WHERE id = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, id);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                name = resultSet.getString(1);

                //add it to *both* hashmaps, to save other lookups
                idToName.put(id, name);
                nameToId.put(name, id);

            } else {
                //there was no name for this given id, this should never happen
                System.err.println("ERROR: tried to get a data type name for id '" + id + "', but this id is not in the database. This should never happen.");
                System.exit(1);
            }

            resultSet.close();
            query.close();
        }

        return name;
    }

    /**
     * Get a type id (for either a double or string series) given a particular name
     *
     * @param connection is the connection to the database
     * @param name is the actual type name
     */
    public static int getId(Connection connection, String name) throws SQLException {
        Integer id = nameToId.get(name);

        if (id == null) {
            //query the database for the name
            String queryString = "SELECT id FROM data_type_names WHERE name = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, name);

            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                id = resultSet.getInt(1);

                //add it to *both* hashmaps, to save other lookups
                idToName.put(id, name);
                nameToId.put(name, id);

            } else {
                //create a new entry in the data_type_names table for this
                //previously unseen name

                String insertQueryString = "INSERT INTO data_type_names SET name = ?";
                PreparedStatement insertQuery = connection.prepareStatement(insertQueryString, Statement.RETURN_GENERATED_KEYS);
                insertQuery.setString(1, name);

                insertQuery.executeUpdate();

                ResultSet insertResultSet = insertQuery.getGeneratedKeys();
                insertResultSet.next();

                id = insertResultSet.getInt(1);

                //now that it was added to the database, add the name and id
                //to *both* hashmaps to save other lookups
                idToName.put(id, name);
                nameToId.put(name, id);

                insertResultSet.close();
                insertQuery.close();
            }

            resultSet.close();
            query.close();
        }

        return id;
    }
}
