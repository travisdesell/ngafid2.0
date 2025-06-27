package org.ngafid.core.proximity;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.util.Compression;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.time.ZoneOffset;
import java.time.LocalDateTime;

public class ProximityPointsProcessor {
    private static final Logger LOG = Logger.getLogger(ProximityPointsProcessor.class.getName());

    private static final int PROXIMITY_EVENT_DEFINITION_ID = -1;
    /**
     * Populates the proximity_points table for a list of proximity events.
     * Each event should be a Map with keys: id, flightId, otherFlightId, startTime, endTime
     */
    public static void addProximityPoints(List<Map<String, Object>> events) {
        LOG.info("Populating proximity_points for " + events.size() + " events...");
        int totalInserted = 0;
        LOG.info("Before connection ");
        try (Connection connection = Database.getConnection()) {
            LOG.info("After connection ");

            String insertSQL = "INSERT INTO proximity_points (event_id, flight_id, latitude, longitude, timestamp, altitude_agl) VALUES (?, ?, ?, ?, FROM_UNIXTIME(?), ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                for (Map<String, Object> event : events) {
                    int eventId = (int) event.get("id");
                    int flightId1 = (int) event.get("flightId");
                    int flightId2 = (int) event.get("otherFlightId");
                    Timestamp startTime = (Timestamp) event.get("startTime");
                    Timestamp endTime = (Timestamp) event.get("endTime");

                    // Convert to LocalDateTime (no timezone)
                    LocalDateTime startLocal = startTime.toLocalDateTime();
                    LocalDateTime endLocal = endTime.toLocalDateTime();

                    // Assign UTC
                    ZonedDateTime startUTC = startLocal.atZone(ZoneOffset.UTC);
                    ZonedDateTime endUTC = endLocal.atZone(ZoneOffset.UTC);

                    long startUnix = startUTC.toEpochSecond();
                    long endUnix = endUTC.toEpochSecond();

                    System.out.println("Raw startTime from DB: " + event.get("startTime"));
                    System.out.println("Raw endTime from DB: " + event.get("endTime"));
                    System.out.println("startTime.toInstant(): " + startTime.toInstant());

                    totalInserted += insertPointsForFlight(connection, insertStmt, eventId, flightId1, startUnix, endUnix);
                    totalInserted += insertPointsForFlight(connection, insertStmt, eventId, flightId2, startUnix, endUnix);
                }
                LOG.info("Inserted " + totalInserted + " proximity points.");

            }
        } catch (Exception e) {
            LOG.severe("Exception in addProximityPoints: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Get all proximity events across all fleets
     * @return a list of all proximity events
     * @throws SQLException if there is an error accessing the database
     */
    public static List<Map<String, Object>> getProximityEvents() throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {

            String query = """
                SELECT e.id, e.flight_id, e.other_flight_id, e.start_time, e.end_time, 
                       e.severity,
                       f1.system_id as flight_system_id,
                       f2.system_id as other_flight_system_id,
                       f1.fleet_id,
                       a1.airframe as flight_airframe,
                       a2.airframe as other_flight_airframe,
                       em1.value as lateral_distance,
                       em2.value as vertical_distance
                FROM events e
                JOIN flights f1 ON e.flight_id = f1.id
                JOIN flights f2 ON e.other_flight_id = f2.id
                JOIN airframes a1 ON f1.airframe_id = a1.id
                JOIN airframes a2 ON f2.airframe_id = a2.id
                LEFT JOIN event_metadata em1 ON e.id = em1.event_id AND em1.key_id = (SELECT id FROM event_metadata_keys WHERE name = 'lateral_distance')
                LEFT JOIN event_metadata em2 ON e.id = em2.event_id AND em2.key_id = (SELECT id FROM event_metadata_keys WHERE name = 'vertical_distance')
                WHERE e.event_definition_id = ?
                ORDER BY e.start_time DESC
            """;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, PROXIMITY_EVENT_DEFINITION_ID);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Map<String, Object> event = new HashMap<>();
                        event.put("id", resultSet.getInt("id"));
                        event.put("flightId", resultSet.getInt("flight_id"));
                        event.put("otherFlightId", resultSet.getInt("other_flight_id"));
                        event.put("startTime", resultSet.getTimestamp("start_time"));
                        event.put("endTime", resultSet.getTimestamp("end_time"));
                        event.put("severity", resultSet.getInt("severity"));
                        event.put("flightSystemId", resultSet.getString("flight_system_id"));
                        event.put("otherFlightSystemId", resultSet.getString("other_flight_system_id"));
                        event.put("fleetId", resultSet.getInt("fleet_id"));
                        event.put("flightAirframe", resultSet.getString("flight_airframe"));
                        event.put("otherFlightAirframe", resultSet.getString("other_flight_airframe"));
                        event.put("lateralDistance", resultSet.getDouble("lateral_distance"));
                        event.put("verticalDistance", resultSet.getDouble("vertical_distance"));
                        events.add(event);
                    }
                }
            }
        }

        return events;
    }


    public static void insertProximityPointsForEvents(Connection connection, List<Event> events, Map<Event, List<ProximityPointData>> proximityPointsMap) throws SQLException {
        String sql = "INSERT INTO proximity_points (event_id, flight_id, latitude, longitude, timestamp, altitude_agl, lateral_distance, vertical_distance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int inserted = 0;
            for (Event event : events) {
                List<ProximityPointData> points = proximityPointsMap.get(event);
                if (points == null) continue;

                for (ProximityPointData point : points) {
                    stmt.setInt(1, event.getId());
                    stmt.setInt(2, event.getFlightId());
                    stmt.setDouble(3, point.getLatitude());
                    stmt.setDouble(4, point.getLongitude());
                    stmt.setTimestamp(5, Timestamp.from(point.getTimestamp().toInstant()));
                    stmt.setDouble(6, point.getAltitudeAGL());
                    stmt.setDouble(7, point.getLateralDistance());
                    stmt.setDouble(8, point.getVerticalDistance());
                    stmt.addBatch();
                    inserted++;
                }
            }
            stmt.executeBatch();
            LOG.info("Inserted " + inserted + " proximity points.");
        }
    }



    // Helper to extract and insert points for a single flight in an event
    private static int insertPointsForFlight(Connection connection, PreparedStatement insertStmt, int eventId, int flightId, long startUnix, long endUnix) {
        int inserted = 0;
        LOG.warning("insertPointsForFlight called for eventId=" + eventId + ", flightId=" + flightId);

        try {
            LOG.warning("Preparing to query double_series for flightId=" + flightId);
            String query = "SELECT ds1.data as latitude_data, ds2.data as longitude_data, ds3.data as timestamp_data, ds4.data as agl_data, ds1.length as length " +
                    "FROM double_series ds1 " +
                    "JOIN double_series ds2 ON ds1.flight_id = ds2.flight_id " +
                    "JOIN double_series ds3 ON ds1.flight_id = ds3.flight_id " +
                    "JOIN double_series ds4 ON ds1.flight_id = ds4.flight_id " +
                    "JOIN double_series_names dsn1 ON ds1.name_id = dsn1.id " +
                    "JOIN double_series_names dsn2 ON ds2.name_id = dsn2.id " +
                    "JOIN double_series_names dsn3 ON ds3.name_id = dsn3.id " +
                    "JOIN double_series_names dsn4 ON ds4.name_id = dsn4.id " +
                    "WHERE ds1.flight_id = ? " +
                    "AND dsn1.name = 'Latitude' " +
                    "AND dsn2.name = 'Longitude' " +
                    "AND dsn3.name = 'Unix Time Seconds' " +
                    "AND dsn4.name = 'AltAGL'";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, flightId);
                try (ResultSet rs = stmt.executeQuery()) {
                    LOG.warning("Query executed for flightId=" + flightId);
                    if (rs.next()) {
                        byte[] latitudeData = rs.getBytes("latitude_data");
                        byte[] longitudeData = rs.getBytes("longitude_data");
                        byte[] timestampData = rs.getBytes("timestamp_data");
                        byte[] aglData = rs.getBytes("agl_data");
                        int size = rs.getInt("length");
                        try {
                            double[] latitudes = Compression.inflateDoubleArray(latitudeData, size);
                            double[] longitudes = Compression.inflateDoubleArray(longitudeData, size);
                            double[] timestampsArray = Compression.inflateDoubleArray(timestampData, size);
                            double[] aglArray = Compression.inflateDoubleArray(aglData, size);
                            long[] timestampsLongArray = new long[size];
                            for (int i = 0; i < size; i++) {
                                timestampsLongArray[i] = (long) timestampsArray[i];
                            }
                            // Print the min and max timestamp in the series
                            long minTimestamp = Long.MAX_VALUE;
                            long maxTimestamp = Long.MIN_VALUE;
                            for (long ts : timestampsLongArray) {
                                if (ts < minTimestamp) minTimestamp = ts;
                                if (ts > maxTimestamp) maxTimestamp = ts;
                            }

                            System.out.println("Event id : "+ eventId +  ". Time series for flightId: " + flightId  );
                            System.out.println("minTimestamp=" + minTimestamp + ", maxTimestamp=" + maxTimestamp);
                            System.out.println("Event time range: startUnix=" + startUnix + ", endUnix=" + endUnix);
                            System.out.println("***** UTC time *****");
                            ZonedDateTime minTime = Instant.ofEpochSecond(minTimestamp).atZone(ZoneId.of("UTC"));
                            System.out.println("Min UTC Time series: " + minTime);
                            ZonedDateTime maxTime = Instant.ofEpochSecond(maxTimestamp).atZone(ZoneId.of("UTC"));
                            System.out.println("Max UTC Time series: " + maxTime);
                            ZonedDateTime minEventTime= Instant.ofEpochSecond(startUnix).atZone(ZoneId.of("UTC"));
                            System.out.println("Min UTC Time event: " + minEventTime);
                            ZonedDateTime maxEventTime= Instant.ofEpochSecond(endUnix).atZone(ZoneId.of("UTC"));
                            System.out.println("!Max UTC Time event: " + maxEventTime);

                            int filtered = 0;
                            for (int i = 0; i < size; i++) {
                                long timestamp = timestampsLongArray[i];
                                if (timestamp >= startUnix && timestamp <= endUnix) {
                                    filtered++;
                                    double lat = latitudes[i];
                                    double lon = longitudes[i];
                                    double agl = aglArray[i];
                                    if (!Double.isNaN(lat) && !Double.isNaN(lon) && lat != 0.0 && lon != 0.0) {
                                        insertStmt.setInt(1, eventId);
                                        insertStmt.setInt(2, flightId);
                                        insertStmt.setDouble(3, lat);
                                        insertStmt.setDouble(4, lon);
                                        insertStmt.setLong(5, timestamp);
                                        insertStmt.setDouble(6, agl);
                                        insertStmt.addBatch();
                                        inserted++;
                                    }
                                }
                            }
                            System.out.println("Filtered: " + filtered);
                            System.out.println("Inserted: " + inserted);
                            System.out.println("***");
                            System.out.println();
                            System.out.println();
                            LOG.info("Points in time range: " + filtered);
                            insertStmt.executeBatch();
                        } catch (java.io.IOException e) {
                            LOG.severe("IO error decompressing data for flight " + flightId + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.severe("SQL error for flight " + flightId + ": " + e.getMessage());
        }
        return inserted;
    }

    public static List<Map<String, Object>> getAllProximityPoints() {
        List<Map<String, Object>> eventsList = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            String query = "SELECT pp.event_id, pp.latitude, pp.longitude, pp.timestamp, pp.flight_id, pp.altitude_agl, a.airframe as flight_airframe " +
                           "FROM proximity_points pp " +
                           "JOIN flights f ON pp.flight_id = f.id " +
                           "JOIN airframes a ON f.airframe_id = a.id " +
                           "ORDER BY pp.event_id, pp.flight_id, pp.timestamp";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    int currentEventId = -1;
                    int currentFlightId = -1;
                    Map<String, Object> eventMap = null;
                    List<Map<String, Object>> points = null;
                    String currentFlightAirframe = null;
                    while (rs.next()) {
                        int eventId = rs.getInt("event_id");
                        int flightId = rs.getInt("flight_id");
                        String flightAirframe = rs.getString("flight_airframe");
                        if (eventMap == null || eventId != currentEventId || flightId != currentFlightId) {
                            // Save previous eventMap if exists
                            if (eventMap != null) {
                                eventsList.add(eventMap);
                            }
                            // Start new eventMap
                            eventMap = new HashMap<>();
                            eventMap.put("event_id", eventId);
                            eventMap.put("flight_id", flightId);
                            eventMap.put("flight_airframe", flightAirframe);
                            // Fetch lateral/vertical distance
                            Map<String, Double> meta = getLateralAndVerticalDistance(eventId);
                            eventMap.put("lateral_distance", meta.getOrDefault("lateral_distance", null));
                            eventMap.put("vertical_distance", meta.getOrDefault("vertical_distance", null));
                            points = new ArrayList<>();
                            eventMap.put("points", points);
                            currentEventId = eventId;
                            currentFlightId = flightId;
                            currentFlightAirframe = flightAirframe;
                        }
                        Map<String, Object> point = new HashMap<>();
                        point.put("latitude", rs.getDouble("latitude"));
                        point.put("longitude", rs.getDouble("longitude"));
                        point.put("timestamp", rs.getTimestamp("timestamp").toString());
                        point.put("altitude_agl", rs.getDouble("altitude_agl"));
                        points.add(point);
                    }
                    // Add the last eventMap
                    if (eventMap != null) {
                        eventsList.add(eventMap);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return eventsList;
    }
    

    // Fetches lateral and vertical distance for a given event_id from event_metadata
    public static Map<String, Double> getLateralAndVerticalDistance(int eventId) {
        Map<String, Double> result = new HashMap<>();
        String query = "SELECT ek.name, em.value FROM event_metadata em " +
                       "JOIN event_metadata_keys ek ON em.key_id = ek.id " +
                       "WHERE em.event_id = ? AND (ek.name = 'lateral_distance' OR ek.name = 'vertical_distance')";
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    double value = rs.getDouble(2);
                    result.put(name, value);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Add methods here for population logic
} 