package org.ngafid.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class SeriesNames {


    public static String getDoubleName(Connection connection, int id) {
    }

    public static String getStringName(Connection connection, int id) {
    }

    public static int getStringNameId(Connection connection, String name) throws SQLException {
        String queryString = "SELECT id FROM string_series_names WHERE name = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setString(1, this.name);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        } else {
            queryString = "INSERT INTO string_series_names(name) VALUES(?)";

            query = connection.prepareStatement(queryString);
            query.setString(1, this.name);

            if (query.executeUpdate() == 1) {
                return getSeriesNameId(connection, flightId);
            }
        }
        return -1;
    }

    public static int getDoubleNameId(Connection connection, String name) throws SQLException {
        String queryString = "SELECT id FROM double_series_names WHERE name = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setString(1, this.name);

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        } else {
            queryString = "INSERT INTO double_series_names(name) VALUES(?)";

            query = connection.prepareStatement(queryString);
            query.setString(1, this.name);

            if (query.executeUpdate() == 1) {
                return getSeriesNameId(connection, flightId);
            }
        }
        return -1;
    }


}
