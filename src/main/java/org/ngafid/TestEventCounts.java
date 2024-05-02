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
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.events.EventStatistics;
import org.ngafid.flights.calculations.TurnToFinal;

public class TestEventCounts {
    private static Connection connection = Database.getConnection();

    public static void main(String[] arguments) {
        try {
            Map<EventStatistics.AirframeEventCount, EventStatistics.EventCount> counts = EventStatistics.getEventCountsFast(TestEventCounts.connection, null, null);
            for (Map.Entry<EventStatistics.AirframeEventCount, EventStatistics.EventCount> entry : counts.entrySet()) {
                System.out.println("" + entry.getKey() + " : " + entry.getValue().eventDefinition.getName() + " : " + entry.getValue().toString());
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
