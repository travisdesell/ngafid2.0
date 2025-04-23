package org.ngafid.core.flights;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ngafid.core.util.ErrorMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class FlightError {
    private static final Logger LOG = Logger.getLogger(FlightError.class.getName());

    @JsonProperty
    private int id;
    @JsonProperty
    private int uploadId;
    @JsonProperty
    private String filename;
    @JsonProperty
    private String message;
    @JsonProperty
    private String stackTrace;

    public static void insertError(Connection connection, int uploadId, String filename, String message)
            throws SQLException {
        try (PreparedStatement exceptionPreparedStatement = connection
                .prepareStatement("INSERT INTO flight_errors (upload_id, filename, message_id) VALUES (?, ?, ?)")) {
            exceptionPreparedStatement.setInt(1, uploadId);
            exceptionPreparedStatement.setString(2, filename);
            exceptionPreparedStatement.setInt(3, ErrorMessage.getMessageId(connection, message));

            exceptionPreparedStatement.executeUpdate();
        }
    }

    public static ArrayList<FlightError> getFlightErrors(Connection connection, int uploadId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT id, upload_id, filename, message_id FROM flight_errors WHERE upload_id = " + uploadId);
             ResultSet resultSet = query.executeQuery()) {
            ArrayList<FlightError> errors = new ArrayList<FlightError>();

            while (resultSet.next()) {
                errors.add(new FlightError(connection, resultSet));
            }

            return errors;
        }
    }

    /**
     * @param connection is the connection to the database
     * @param fleetId    is the fleet's id
     * @return the number of flight errors for a fleet
     */
    public static int getCount(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT sum(n_error_flights) FROM uploads";
        if (fleetId > 0)
            queryString += " WHERE fleet_id = " + fleetId;

        try (PreparedStatement query = connection.prepareStatement(queryString);
             ResultSet resultSet = query.executeQuery()) {
            resultSet.next();

            int count = resultSet.getInt(1);

            return count;
        }
    }

    public FlightError(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        uploadId = resultSet.getInt(2);
        filename = resultSet.getString(3);
        message = ErrorMessage.getMessage(connection, resultSet.getInt(4));
    }
}
