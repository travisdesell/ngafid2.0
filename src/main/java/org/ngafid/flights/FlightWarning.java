package org.ngafid.flights;

import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class FlightWarning {
    private int id;
    private int uploadId;
    private int flightId;
    private String filename;
    private String message;
    private String stackTrace;

    public static ArrayList<FlightWarning> getFlightWarnings(Connection connection, int uploadId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT flights.filename, flights.upload_id, flight_warnings.id, flight_warnings.message, flight_warnings.stack_trace, flight_warnings.flight_id FROM flight_warnings, flights WHERE flights.upload_id = ? AND flight_warnings.flight_id = flights.id");
                
        query.setInt(1, uploadId);
        ResultSet resultSet = query.executeQuery();

        ArrayList<FlightWarning> warnings = new ArrayList<FlightWarning>();

        while (resultSet.next()) {
            warnings.add(new FlightWarning(resultSet));
        }

        return warnings;
    }

    public FlightWarning(ResultSet resultSet) throws SQLException {
        filename = resultSet.getString(1);
        uploadId = resultSet.getInt(2);
        id = resultSet.getInt(3);
        message = resultSet.getString(4);
        stackTrace = resultSet.getString(5);
        flightId = resultSet.getInt(6);
    }
}
