package org.ngafid.flights;



public class SeriesNames {


    public static int getDoubleNameId(Connection connection, String name) {
    }

    public static int getStringeNameId(Connection connection, String name) {
    }

    private int getSeriesNameId(Connection connection, int flightId) throws SQLException {
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

    private int getSeriesNameId(Connection connection, int flightId) throws SQLException {
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
