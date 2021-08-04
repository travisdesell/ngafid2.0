package org.ngafid;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightWarning;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.StringTimeSeries;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Conditional;
import org.ngafid.filters.Filter;
import org.ngafid.filters.Pair;

import org.ngafid.airports.Airports;

public class FixCHT1Divergence {

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
                            String chtNames[] = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"};
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
                    System.out.println(ps.toString());
                    ps.executeUpdate();
                    ps.close();

                    System.out.println();
                    count++;
                    total++;
                }

                System.out.println("processed " + count + " flights, total flights processed: " + total);
            }

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("finished!");
        System.exit(1);
    }
}
