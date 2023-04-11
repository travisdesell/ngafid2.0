package org.ngafid;

import java.time.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ConvertDoubleSeriesNames {
    private static Connection connection = Database.getConnection();
    private static final Logger LOG = Logger.getLogger(ConvertDoubleSeriesNames.class.getName());

    public static List<String> getDoubleSeriesNamesOriginal() throws SQLException {
        List<String> dsnOriginal = new ArrayList<>();
        String queryString = "SELECT UNIQUE name FROM double_series"; // TODO: This query looks like a problem

        PreparedStatement preparedStatement = connection.prepareStatement(queryString);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            dsnOriginal.add(resultSet.getString(1));
        }

        resultSet.close();

        return dsnOriginal;
    }

    public static Map<String, Integer> insertNames(List<String> dsnOriginal) throws SQLException {
        int len = dsnOriginal.size();

        Map<String, Integer> names = new HashMap<>();

        String queryString = "INSERT INTO double_series_names(name) VALUES(?)";
        for (int i = 0; i < len; i++) {
            String name = dsnOriginal.get(i);

            names.put(name, (i + 1));

            PreparedStatement preparedStatement = connection.prepareStatement(queryString);
            preparedStatement.setString(1, name);

            preparedStatement.executeUpdate();
        }

        return names;
    }

    public static void updateDoubleSeries(Map<String, Integer> names) throws SQLException {
        String updateQueryString = "ALTER TABLE double_series ADD COLUMN name_id INT(11) NOT NULL";
        PreparedStatement updateTableQuery = connection.prepareStatement(updateQueryString);

        if (updateTableQuery.executeUpdate() != 1) {
            LOG.info("double_series table alteration complete, updating ids...");
            Instant start = Instant.now();

            for (String name : names.keySet()) {
                String nameChangeQueryString = "UPDATE double_series SET name_id = ? WHERE name = ?";
                PreparedStatement nameChangeQuery = connection.prepareStatement(nameChangeQueryString);

                nameChangeQuery.setInt(1, names.get(name));
                nameChangeQuery.setString(2, name);

                if (nameChangeQuery.executeUpdate() == 1) {
                   LOG.severe("Unable to alter name \"" + name + "\" to its respective id: " + names.get(name));
                   LOG.severe("Aborting!");

                   connection.close();

                   System.exit(1);
                }

            }

            Instant end = Instant.now();
            long elapsedMillis = Duration.between(start, end).toMillis();
            double elapsedSeconds = ((double) elapsedMillis) / 1000;

            LOG.info("finished updating ids in: " + elapsedSeconds + "s");
            LOG.info("double_series table name conversion to id complete!");
            LOG.info("Deleting columns no longer needed...");

            updateQueryString = "ALTER TABLE double_series DROP COLUMN name";
            updateTableQuery = connection.prepareStatement(updateQueryString);

            if (updateTableQuery.executeUpdate() == 1) {
                LOG.severe("Unable to drop name column from double_series! Aborting!");
                connection.close();
                System.exit(1);
            } else {
                LOG.info("double_series table update complete!");
            }

        } else {
            LOG.severe("Unable to alter double_series table, aborting!");
            
            connection.close();
            System.exit(1);
        }
    }

    public static void checkTables() throws SQLException {
        String queryString = "SHOW TABLES LIKE 'double_series_names'";

        PreparedStatement query = connection.prepareStatement(queryString);

        ResultSet resultSet = query.executeQuery();

        if (!resultSet.next()) {
            LOG.severe("double_series_names table not created, make sure it is created in the DB schema!");
            
            connection.close();
            System.exit(1);
        }
    }

    public static void main(String [] args) {
        try {
            checkTables();

            List<String> dsnOriginal = getDoubleSeriesNamesOriginal();
            Map<String, Integer> names = insertNames(dsnOriginal);

            updateDoubleSeries(names);

            connection.close();
            System.exit(0);
        } catch (SQLException e) {
            e.printStackTrace();

            System.exit(1);
        }
    }
}
