package org.ngafid;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.*;

import org.ngafid.events.EventStatistics;

public class TestEventCounts {

    public static void main(String[] arguments) {
        try (Connection connection = Database.getConnection()) {
            Map<String, EventStatistics.EventCounts> counts = EventStatistics.getEventCounts(connection,
                    null, null);
            for (Map.Entry<String, EventStatistics.EventCounts> entry : counts.entrySet()) {
                List<String> names = entry.getValue().names;

                for (int i = 0; i < names.size(); i++) {
                    System.out.println("" + entry.getKey() + " : " + names.get(i) + " : "
                            + entry.getValue().aggregateFlightsWithEventCounts[i]);
                }
            }

            EventStatistics.FlightCounts flightCounts = EventStatistics.getFlightCounts(connection,
                    null, null);
            for (Map.Entry<Integer, Integer> entry : flightCounts.getAggregateCounts().entrySet()) {
                System.out.println("airframe " + entry.getKey() + " has " + entry.getValue() + " flights total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
