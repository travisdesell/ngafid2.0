package org.ngafid;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.ngafid.flights.*;
import org.ngafid.flights.calculations.TurnToFinal;

public class CalculateTTF {
    private CalculateTTF() {
        throw new UnsupportedOperationException("Utility class not meant to be instantiated");
    }

    /**
     * Calculate the Turn-To-Final for all flights in the database that have not been calculated yet
     * @param connection Database connection
     * @param uploadId ID of the upload
     * @param uploadProcessedEmail Email object to send the results to
     * @throws SQLException SQL Exception
     */
    public static void calculateTTF(Connection connection, int uploadId, UploadProcessedEmail uploadProcessedEmail)
            throws SQLException {
        Instant start = Instant.now();

        int total = 0;

        // Grab flights that have not been inserted at all, or flights that have an old
        // version of TTF
        String condition = "upload_id = " + uploadId + " AND insert_completed = 1 AND NOT EXISTS " +
                "(SELECT flight_id, version FROM turn_to_final where flight_id = id and version = "
                + TurnToFinal.serialVersionUID + ")";
        ArrayList<Flight> flights = Flight.getFlights(connection, condition);
        while (!flights.isEmpty()) {
            Flight flight = flights.remove(flights.size() - 1);
            // This function automatically saves the calculated TTF object to the database
            try {
                TurnToFinal.calculateFlightTurnToFinals(connection, flight);
            } catch (IOException e) {
                System.err.println("IOException: " + e);
                e.printStackTrace();

                uploadProcessedEmail.addTTFError(flight.getFilename(),
                        "Had an IOException when calculating turn-to-final: " + e.toString());
            }
            total++;
        }

        Instant end = Instant.now();
        double elapsedMillis = (double) Duration.between(start, end).toMillis();
        double elapsedSeconds = Math.round(elapsedMillis) / 1000;
        System.err.println("calculated TTF for " + total + " flight(s) in " + elapsedSeconds + "s");
        uploadProcessedEmail.setTTFElapsedTime(elapsedSeconds);
    }

    /**
     * Drop old TTF data from the database
     * @param connection Database connection
     * @param flights List of flights to drop TTF data from
     * @throws SQLException SQL Exception
     */
    private static void dropOldTTF(Connection connection, ArrayList<Flight> flights) throws SQLException {
        String query = String.format("DELETE FROM turn_to_final WHERE flight_id IN (%s)",
                flights.stream()
                        .map(f -> "?")
                        .collect(Collectors.joining(", ")));

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < flights.size(); i++) {
                stmt.setInt(i + 1, flights.get(i).getId());
            }
            stmt.executeUpdate();
        }
    }

    public static void main(String[] arguments) throws SQLException {
        while (true) {
            try (Connection connection = Database.getConnection()) {
                Instant start = Instant.now();
                int total = 0;

                int flightsProcessed = 0;
                do {
                    String condition = "insert_completed = 1 AND NOT EXISTS " +
                            "(SELECT flight_id, version FROM turn_to_final where flight_id = id and version = "
                            + TurnToFinal.serialVersionUID + ")";
                    ArrayList<Flight> flights = Flight.getFlights(connection, condition, 100);
                    if (flights.isEmpty()) {
                        continue;
                    }

                    dropOldTTF(connection, flights);

                    for (Flight f : flights)
                        TurnToFinal.calculateFlightTurnToFinals(connection, f);

                    flightsProcessed = flights.size();

                    total += flightsProcessed;
                } while (flightsProcessed > 0);

                Instant end = Instant.now();
                double elapsedMillis = (double) Duration.between(start, end).toMillis();
                double elapsedSeconds = (double) Math.round(elapsedMillis) / 1000;
                System.err.println("calculated TTF for " + total + " flight(s) in " + elapsedSeconds + "s");

                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                System.err.println("Failed...");
                return;
            }
        }
    }
}
