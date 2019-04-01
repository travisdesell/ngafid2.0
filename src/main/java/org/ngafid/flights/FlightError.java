package org.ngafid.flights;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.logging.Logger;

public class FlightError {
    private static final Logger LOG = Logger.getLogger(FlightError.class.getName());

    private int id;
    private int uploadId;
    private String filename;
    private String message;
    private String stackTrace;

    public static void insertError(Connection connection, int uploadId, String filename, String message) throws SQLException {
        PreparedStatement exceptionPreparedStatement = connection.prepareStatement("INSERT INTO flight_errors (upload_id, filename, message_id) VALUES (?, ?, ?)");
        exceptionPreparedStatement.setInt(1, uploadId);
        exceptionPreparedStatement.setString(2, filename);
        exceptionPreparedStatement.setInt(3, ErrorMessage.getMessageId(connection, message));

        LOG.info(exceptionPreparedStatement.toString());

        exceptionPreparedStatement.executeUpdate();
    }


    public static ArrayList<FlightError> getFlightErrors(Connection connection, int uploadId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id, upload_id, filename, message_id FROM flight_errors WHERE upload_id = ?");
        query.setInt(1, uploadId);
        ResultSet resultSet = query.executeQuery();

        ArrayList<FlightError> errors = new ArrayList<FlightError>();

        while (resultSet.next()) {
            errors.add(new FlightError(connection, resultSet));
        }

        return errors;
    }

    public FlightError(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        uploadId = resultSet.getInt(2);
        filename = resultSet.getString(3);
        message = ErrorMessage.getMessage(connection, resultSet.getInt(4));
    }
}
