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
            Map<Integer, EventStatistics.EventCount> counts = EventStatistics.getEventCountsFast(TestEventCounts.connection, 1, null, null);
            for (Map.Entry<Integer, EventStatistics.EventCount> entry : counts.entrySet()) {
                System.out.println("" + entry.getKey() + " : " + entry.getValue().eventDefinition.getName() + " : " + entry.getValue().count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
