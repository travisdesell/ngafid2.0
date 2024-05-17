package org.ngafid;

import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.events.EventStatistics;
import org.ngafid.flights.calculations.TurnToFinal;

public class TestEventCounts {
    private static Connection connection = Database.getConnection();

    public static void main(String[] arguments) {
        try {
            Map<String, EventStatistics.EventCounts> counts = EventStatistics.getEventCounts(TestEventCounts.connection, null, null);
            for (Map.Entry<String, EventStatistics.EventCounts> entry : counts.entrySet()) {
                List<String> names = entry.getValue().names;

                for (int i = 0; i < names.size(); i++) {
                    System.out.println("" + entry.getKey() + " : " + names.get(i) + " : " + entry.getValue().aggregateFlightsWithEventCounts[i]);
                }
            }

            EventStatistics.FlightCounts flightCounts = EventStatistics.getFlightCounts(TestEventCounts.connection, null, null);
            for (Map.Entry<Integer, Integer> entry : flightCounts.getAggregateCounts().entrySet()) {
                System.out.println("airframe " + entry.getKey() + " has " + entry.getValue() + " flights total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
