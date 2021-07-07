package org.ngafid;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ConvertDoubleSeriesNames {
    private static Connection connection = Database.getConnection();
    private static final Logger LOG = Logger.getLogger(ConvertDoubleSeriesNames.class.getName());

    //public static List<String> getDoubleSeriesNamesOriginal() {
        //String queryString = "SELECT UN";
    //}

    public static void checkTables() throws SQLException {
        String queryString = "SHOW TABLES LIKE 'double_series_names'";

        PreparedStatement query = connection.prepareStatement(queryString);

        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) {
            LOG.severe("double_series_names table not created, make sure it is created in the DB schema!");
            System.exit(1);
        }
    }

    public static void main(String [] args) {
        try {
            checkTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
