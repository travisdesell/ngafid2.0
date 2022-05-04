package org.ngafid;

import org.ngafid.flights.Flight;
import org.ngafid.flights.TurnToFinal;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.NIFA;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class CalculateNIFA {
    private static Connection connection = Database.getConnection();

    public static void main(String[] arguments) throws SQLException {
        int nifaAirframeID = Airframes.getNameId(connection, "BE-GPS-2200");
        String condition =
                "insert_completed = 1 AND airframe_id = " + nifaAirframeID + " AND processing_status & " + Flight.NIFA_EVENTS_CALCULATED + " != 0";

        while (true) {
            connection = Database.resetConnection();

            Instant start = Instant.now();
            int total = 0;
            int flights_processed = 1;
            while (flights_processed > 0) {
                flights_processed = 0;
                try {
                    ArrayList<Flight> flights = Flight.getFlights(connection, condition, 100);

                    while (flights.size() > 0) {
                        Flight flight = flights.remove(flights.size() - 1);
                        NIFA.processFlight(connection, flight);
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
