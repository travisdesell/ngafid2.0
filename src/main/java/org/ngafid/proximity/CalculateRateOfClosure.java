package org.ngafid.proximity;

import org.ngafid.Database;
import org.ngafid.events.RateOfClosure;
import org.ngafid.flights.Flight;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CalculateRateOfClosure {
    private CalculateRateOfClosure() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void main(String[] args) {
        try (Connection connection = Database.getConnection()) {
            String queryString = "SELECT e.flight_id as flight_id, e.other_flight_id as " +
                    "otherFlightId, e.severity as severity, e.fleet_id AS fleet_id,"
                    +
                    " e.start_line as startLine, e.end_line as endLine, e.id as eventId FROM events AS e " +
                    " WHERE e.event_definition_id = -1 AND severity > 0 " +
                    "AND e.id NOT IN (SELECT event_id FROM rate_of_closure)";
            PreparedStatement query = connection.prepareStatement(queryString);
            ResultSet resultSet = query.executeQuery();
            while (resultSet.next()) {

                int flightId = resultSet.getInt(1);
                int otherFlightId = resultSet.getInt(2);
                double severity = resultSet.getDouble(3);
                int fleetId = resultSet.getInt(4);
                int startLine = resultSet.getInt(5);
                int endLine = resultSet.getInt(6);
                int eventId = resultSet.getInt(7);
                String otherEventQueryString = "SELECT e.fleet_id AS fleet_id,e.start_line AS " +
                        "startLine, e.end_line AS endLine, e.id AS otherEventId from events AS e " +
                        "WHERE e.event_definition_id = -1 AND e.severity = ? " +
                        "AND e.flight_id = ? AND e.other_flight_id = ?";
                PreparedStatement otherEventQuery = connection.prepareStatement(otherEventQueryString);
                otherEventQuery.setDouble(1, severity);
                otherEventQuery.setInt(2, otherFlightId);
                otherEventQuery.setInt(3, flightId);
                ResultSet otherResultSet = otherEventQuery.executeQuery();

                Flight flight = Flight.getFlight(connection, flightId);
                Flight otherFlight = Flight.getFlight(connection, otherFlightId);

                if (otherResultSet.next()) {
                    int otherFleetId = otherResultSet.getInt(1);
                    int otherStartLine = otherResultSet.getInt(2);
                    int otherEndLine = otherResultSet.getInt(3);
                    int otherEventId = otherResultSet.getInt(4);

                    FlightTimeLocation flightInfo = new FlightTimeLocation(connection, fleetId, flightId,
                            flight.getAirframeNameId(), flight.getStartDateTime(), flight.getEndDateTime());
                    FlightTimeLocation otherFlightInfo = new FlightTimeLocation(connection, otherFleetId,
                            otherFlightId, otherFlight.getAirframeNameId(), otherFlight.getStartDateTime(),
                            otherFlight.getEndDateTime());

                    // These both should already have series data and be valid since they had a
                    // proximity event calculated
                    flightInfo.getSeriesData(connection);
                    otherFlightInfo.getSeriesData(connection);

                    double[] rateOfClosureArray = CalculateProximity.calculateRateOfClosure(flightInfo,
                            otherFlightInfo, startLine, endLine, otherStartLine, otherEndLine);
                    RateOfClosure roc = new RateOfClosure(rateOfClosureArray);
                    roc.updateDatabase(connection, eventId);
                }
                otherResultSet.close();
                otherEventQuery.close();
                System.out.println(
                        "Fleet Id : " + fleetId + " flight id : " + flightId + " other flight id : " + otherFlightId);
            }
            resultSet.close();
            query.close();
            connection.close();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Calculating Rate of Closure for all proximity events complete.");
        System.exit(0);
    }
}
