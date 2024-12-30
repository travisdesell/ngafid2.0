package org.ngafid;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightWarning;
import org.ngafid.flights.MalformedFlightFileException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public final class FixCHT1Divergence {
    private FixCHT1Divergence() {
        throw new UnsupportedOperationException("Utility class not meant to be instantiated");
    }

    public static void main(String[] arguments) {
        try {
            Connection connection = Database.getConnection();

            int total = 0;
            boolean found = true;
            while (found) {
                ArrayList<Flight> flights = Flight.getFlights(connection, "(airframe_id = 2 OR airframe_id = 10) AND NOT (processing_status & " + Flight.CHT_DIVERGENCE_CALCULATED + ")", 100);

                System.out.println("found " + flights.size() + " flights with CHT divergence not processed");
                found = flights.size() > 0;

                int count = 0;
                for (Flight flight : flights) {
                    int flightId = flight.getId();
                    System.out.println("fixing flight id: " + flightId);

                    DoubleTimeSeries divergenceSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "E1 CHT Divergence");
                    if (flight.getDoubleTimeSeries(connection, "E1 CHT Divergence") != null) {
                        System.out.println("had CHT1 divergence!");
                    } else {
                        System.out.println("DID NOT have CHT1 divergence!");

                        flight.getDoubleTimeSeries(connection, "E1 CHT1");
                        flight.getDoubleTimeSeries(connection, "E1 CHT2");
                        flight.getDoubleTimeSeries(connection, "E1 CHT3");
                        flight.getDoubleTimeSeries(connection, "E1 CHT4");

                        try {
                            String[] chtNames = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"};
                            flight.calculateDivergence(connection, chtNames, "E1 CHT Divergence", "deg F");
                            DoubleTimeSeries chtDivergence = flight.getDoubleTimeSeries("E1 CHT Divergence");
                            chtDivergence.updateDatabase(connection, flightId);
                            System.out.println("Calculated CHT!");
                        } catch (MalformedFlightFileException e) {
                            System.out.println("ERROR: calculating CHT divergence: " + e.getMessage());
                            FlightWarning.insertWarning(connection, flightId, e.getMessage());
                        }
                    }

                    PreparedStatement ps = connection.prepareStatement("UPDATE flights SET processing_status = processing_status | ? WHERE id = ?");
                    ps.setLong(1, Flight.CHT_DIVERGENCE_CALCULATED);
                    ps.setInt(2, flightId);
                    System.out.println(ps);
                    ps.executeUpdate();
                    ps.close();

                    System.out.println();
                    count++;
                    total++;
                }

                System.out.println("processed " + count + " flights, total flights processed: " + total);
            }

            connection.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("finished!");
        System.exit(1);
    }
}
