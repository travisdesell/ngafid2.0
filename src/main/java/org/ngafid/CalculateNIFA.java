package org.ngafid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.ngafid.events.Event;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.Flight;
import org.ngafid.flights.NIFA;

public class CalculateNIFA {
    private static Connection connection = Database.getConnection();

    public static void main(String[] arguments) throws SQLException {
        int nifaAirframeID = Airframes.getNameId(connection, "BE-GPS-2200");
        String condition =
                "insert_completed = 1 AND airframe_id = " + nifaAirframeID + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = -100 AND flight_processed.flight_id = flights.id)";

        while (true) {
            connection = Database.resetConnection();

            System.out.println("SIZE : " + Event.getAll(connection, 1).size());

            Instant start = Instant.now();
            int total = 0;
            int flights_processed = 1;
            while (flights_processed > 0) {
                flights_processed = 0;
                try {
                    ArrayList<Flight> flights = Flight.getFlights(connection, condition, 100);

                    while (flights.size() > 0) {
                        Flight flight = flights.remove(flights.size() - 1);
                        new NIFA(connection, flight);
                        PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = -100, count = 0, had_error = 1");
                        stmt.setInt(1, flight.getFleetId());
                        stmt.setInt(2, flight.getId());
                        //System.out.println(stmt.toString());
                        stmt.executeUpdate();
                        stmt.close();
                        flights_processed++;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                total += flights_processed;
            }

            Instant end = Instant.now();
            double elapsed_millis = (double) Duration.between(start, end).toMillis();
            double elapsed_seconds = Math.round(elapsed_millis) / 1000;
            System.err.println("calculated TTF for " + total + " flight(s) in " + elapsed_seconds + "s");

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }

        }

    }
}
