package org.ngafid;

import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.*;
import org.ngafid.flights.calculations.TurnToFinal;

public class CalculateTTF {
    private static Connection connection = Database.getConnection();

    public static void calculateTTF(Connection connection, int uploadId, UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();

        int total = 0;

        // Grab flights that have not been inserted at all, or flights that have an old version of TTF
        String condition = "upload_id = " + uploadId + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM turn_to_final where flight_id = id)";
        ArrayList<Flight> flights = Flight.getFlights(connection, condition);
        while (flights.size() > 0) {
            Flight flight = flights.remove(flights.size() - 1);
            // This function automatically saves the calculated TTF object to the database
            try {
                TurnToFinal.calculateFlightTurnToFinals(connection, flight);
            } catch (IOException e) {
                System.err.println("IOException: " + e);
                e.printStackTrace();

                uploadProcessedEmail.addTTFError(flight.getFilename(), "Had an IOException when calculating turn-to-final: " + e.toString());
            }
            total++;
        }

        Instant end = Instant.now();
        double elapsed_millis = (double) Duration.between(start, end).toMillis();
        double elapsed_seconds = Math.round(elapsed_millis) / 1000;
        System.err.println("calculated TTF for " + total + " flight(s) in " + elapsed_seconds + "s");
        uploadProcessedEmail.setTTFElapsedTime(elapsed_seconds);
    }


    public static void main(String[] arguments) {
        while (true) {
            connection = Database.resetConnection();

            Instant start = Instant.now();
            int total = 0;
            int flights_processed = 1;
            while (flights_processed > 0) {
                flights_processed = 0;
                try {
                    // Grab flights that have not been inserted at all, or flights that have an old version of TTF
                    String condition =
                            "insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM turn_to_final where flight_id = id)";
                    ArrayList<Flight> flights = Flight.getFlights(connection, condition, 100);
                    while (flights.size() > 0) {
                        Flight flight = flights.remove(flights.size() - 1);
                        // This function automatically saves the calculated TTF object to the database
                        TurnToFinal.calculateFlightTurnToFinals(connection, flight);
                        flights_processed += 1;
                    }
                } catch (SQLException | IOException e) {
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
