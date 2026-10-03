package org.ngafid.core.flights;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.ngafid.core.util.ErrorMessage;

/**
 * A non-fatal warning raised while processing a flight, persisted in {@code flight_warnings}.
 *
 * <p>Each warning ties a flight to an interned message (stored by message id via {@code ErrorMessage}) and supports
 * batched inserts as well as per-flight and per-upload retrieval.
 */
public class FlightWarning {
    private static final Logger LOG = Logger.getLogger(FlightWarning.class.getName());

    @JsonProperty
    private int id;

    @JsonProperty
    private int uploadId;

    @JsonProperty
    private int flightId;

    @JsonProperty
    private String filename;

    @JsonProperty
    private String message;

    @JsonProperty
    private String stackTrace;

    /**
     * Creates a prepared statement for inserting a flight-warning row into {@code flight_warnings}.
     *
     * @param connection the database connection
     * @return a prepared statement for the flight-warning insert
     * @throws SQLException if the statement cannot be prepared
     */
    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO flight_warnings (flight_id, message_id) VALUES (?, ?)");
    }

    /**
     * Binds this warning to the given insert statement and adds it to the batch, resolving the warning's message text
     * to its interned message id first.
     *
     * @param connection the database connection used to resolve the message id
     * @param preparedStatement the insert statement (from {@link #createPreparedStatement}) to populate and batch
     * @param flightIdToAdd the flight id the warning belongs to
     * @throws SQLException if setting a parameter or adding the batch fails
     */
    public void addBatch(Connection connection, PreparedStatement preparedStatement, int flightIdToAdd)
            throws SQLException {
        preparedStatement.setInt(1, flightIdToAdd);
        preparedStatement.setInt(2, ErrorMessage.getMessageId(connection, message));
        preparedStatement.addBatch();
    }

    /**
     * Inserts a single warning message for a flight into the database.
     *
     * @param connection the database connection
     * @param flightId the flight the warning belongs to
     * @param message the warning message text
     * @throws SQLException if the insert fails
     */
    public static void insertWarning(Connection connection, int flightId, String message) throws SQLException {
        try (PreparedStatement exceptionPreparedStatement = createPreparedStatement(connection)) {
            new FlightWarning(message).addBatch(connection, exceptionPreparedStatement, flightId);
            exceptionPreparedStatement.executeBatch();
        }
    }

    /**
     * Loads all warnings recorded for a single flight, resolving each warning's message text.
     *
     * @param connection the database connection
     * @param flightId the flight whose warnings to load
     * @return the flight's warnings (empty if none)
     * @throws SQLException if the query fails
     */
    public static List<FlightWarning> getWarningsByFlight(Connection connection, int flightId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT flights.filename, flights.upload_id, flight_warnings.id, flight_warnings.message_id, "
                        + "flight_warnings.flight_id FROM flight_warnings, flights WHERE flights.id = ? AND "
                        + "flight_warnings.flight_id = flights.id")) {
            query.setInt(1, flightId);

            try (ResultSet resultSet = query.executeQuery()) {
                List<FlightWarning> warnings = new ArrayList<FlightWarning>();

                while (resultSet.next()) {
                    warnings.add(new FlightWarning(connection, resultSet));
                }

                return warnings;
            }
        }
    }

    /**
     * Loads all flight warnings across every flight in a given upload.
     *
     * @param connection the database connection
     * @param uploadId the upload whose flights' warnings to load
     * @return the warnings for all flights in the upload (empty if none)
     * @throws SQLException if the query fails
     */
    public static ArrayList<FlightWarning> getFlightWarnings(Connection connection, int uploadId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT flights.filename, flights.upload_id, flight_warnings.id, flight_warnings.message_id, "
                        + "flight_warnings.flight_id FROM flight_warnings, flights WHERE flights.upload_id = ? AND "
                        + "flight_warnings.flight_id = flights.id")) {
            query.setInt(1, uploadId);

            try (ResultSet resultSet = query.executeQuery()) {
                ArrayList<FlightWarning> warnings = new ArrayList<FlightWarning>();

                while (resultSet.next()) {
                    warnings.add(new FlightWarning(connection, resultSet));
                }

                return warnings;
            }
        }
    }

    /**
     * @param connection is the connection to the database
     * @param fleetId    is the fleet's id
     * @return the number of flight warnings for a fleet
     */
    public static int getCount(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT sum(n_warning_flights) FROM uploads";

        if (fleetId > 0) queryString += " WHERE fleet_id = " + fleetId;

        try (PreparedStatement query = connection.prepareStatement(queryString);
                ResultSet resultSet = query.executeQuery()) {
            LOG.info(query.toString());

            resultSet.next();

            int count = resultSet.getInt(1);

            return count;
        }
    }

    /**
     * Constructs a flight warning carrying only a message (used before it is persisted).
     *
     * @param message the warning message text
     */
    public FlightWarning(String message) {
        this.message = message;
    }

    /**
     * Reconstructs a flight warning from a joined {@code flight_warnings}/{@code flights} result row, resolving the
     * warning's message text from its interned message id.
     *
     * @param connection the database connection used to resolve the message text
     * @param resultSet the result set positioned on the row to read
     * @throws SQLException if reading the row or resolving the message fails
     */
    public FlightWarning(Connection connection, ResultSet resultSet) throws SQLException {
        filename = resultSet.getString(1);
        uploadId = resultSet.getInt(2);
        id = resultSet.getInt(3);
        message = ErrorMessage.getMessage(connection, resultSet.getInt(4));
        flightId = resultSet.getInt(5);
    }
}
