package org.ngafid.flights;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.*;
import java.util.logging.Logger;

public class FlightWarning {
    private static final Logger LOG = Logger.getLogger(FlightWarning.class.getName());

    private int id;
    private int uploadId;
    private int flightId;
    private String filename;
    private String message;
    private String stackTrace;

    public static void insertWarning(Connection connection, int flightId, String message) throws SQLException {
        PreparedStatement exceptionPreparedStatement = connection.prepareStatement("INSERT INTO flight_warnings (flight_id, message_id) VALUES (?, ?)");
        exceptionPreparedStatement.setInt(1, flightId);
        exceptionPreparedStatement.setInt(2, ErrorMessage.getMessageId(connection, message));

        LOG.info(exceptionPreparedStatement.toString());

        exceptionPreparedStatement.executeUpdate();
        exceptionPreparedStatement.close();
    }

    public static List<FlightWarning> getWarningsByFlight(Connection connection, int flightId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT flights.filename, flights.upload_id, flight_warnings.id, flight_warnings.message_id, flight_warnings.flight_id FROM flight_warnings, flights WHERE flights.id = ? AND flight_warnings.flight_id = flights.id");
                
        query.setInt(1, flightId);
        ResultSet resultSet = query.executeQuery();

        List<FlightWarning> warnings = new ArrayList<FlightWarning>();

        while (resultSet.next()) {
            warnings.add(new FlightWarning(connection, resultSet));
        }

        resultSet.close();
        query.close();
        return warnings;
    }

    public static ArrayList<FlightWarning> getFlightWarnings(Connection connection, int uploadId) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT flights.filename, flights.upload_id, flight_warnings.id, flight_warnings.message_id, flight_warnings.flight_id FROM flight_warnings, flights WHERE flights.upload_id = ? AND flight_warnings.flight_id = flights.id");
                
        query.setInt(1, uploadId);
        ResultSet resultSet = query.executeQuery();

        ArrayList<FlightWarning> warnings = new ArrayList<FlightWarning>();

        while (resultSet.next()) {
            warnings.add(new FlightWarning(connection, resultSet));
        }

        resultSet.close();
        query.close();
        return warnings;
    }
        
    /**
     * @param connection is the connection to the database
     * @param fleetId is the fleet's id
     *
     * @return the number of flight warnings for a fleet
     */
    public static int getCount(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT sum(n_warning_flights) FROM uploads";
        
        if (fleetId > 0)
            queryString += " WHERE fleet_id = " + fleetId;

        PreparedStatement query = connection.prepareStatement(queryString);
        LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();
        resultSet.next();

        int count = resultSet.getInt(1);

        resultSet.close();
        query.close();
        return count ;
    }

    public FlightWarning(Connection connection, ResultSet resultSet) throws SQLException {
        filename = resultSet.getString(1);
        uploadId = resultSet.getInt(2);
        id = resultSet.getInt(3);
        message = ErrorMessage.getMessage(connection, resultSet.getInt(4));
        flightId = resultSet.getInt(5);
    }
}
