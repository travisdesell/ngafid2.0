package org.ngafid.flights;

import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class FlightError {
    private int id;
    private int uploadId;
    private String filename;
    private String message;
    private String stackTrace;

    public static ArrayList<FlightError> getFlightErrors(Connection connection, int uploadId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id, upload_id, filename, message, stack_trace FROM flight_errors WHERE upload_id = ?");
        query.setInt(1, uploadId);
        ResultSet resultSet = query.executeQuery();

        ArrayList<FlightError> errors = new ArrayList<FlightError>();

        while (resultSet.next()) {
            errors.add(new FlightError(resultSet));
        }

        return errors;
    }

    public FlightError(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        uploadId = resultSet.getInt(2);
        filename = resultSet.getString(3);
        message = resultSet.getString(4);
        stackTrace = resultSet.getString(5);
    }
}
