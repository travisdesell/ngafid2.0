package org.ngafid.core.proximity;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ProximityPointsProcessor {
    private static final Logger LOG = Logger.getLogger(ProximityPointsProcessor.class.getName());

    private static final int PROXIMITY_EVENT_DEFINITION_ID = -1;

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


    public static void insertProximityPointsForEvents(Connection connection, List<Event> events, Map<Event, List<ProximityPointData>> mainFlightPointsMap, Map<Event, List<ProximityPointData>> otherFlightPointsMap) throws SQLException {
        String sql = "INSERT INTO proximity_points (event_id, flight_id, latitude, longitude, timestamp, altitude_agl, lateral_distance, vertical_distance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int inserted = 0;
            for (Event event : events) {
                List<ProximityPointData> mainPoints = mainFlightPointsMap.get(event);
                if (mainPoints != null) {
                    for (ProximityPointData point : mainPoints) {
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
                List<ProximityPointData> otherPoints = otherFlightPointsMap.get(event);
                if (otherPoints != null) {
                    for (ProximityPointData point : otherPoints) {
                        stmt.setInt(1, event.getId());
                        stmt.setInt(2, event.getOtherFlightId());
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
            }
            stmt.executeBatch();
            LOG.info("Inserted " + inserted + " proximity points (duplicated for both flights per event).");
        }
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

    // Fetches proximity points for a given event_id and flight_id
    public static Map<String, Object> getProximityPointsForEventAndFlight(int eventId, int flightId) {
        Map<String, Object> eventMap = new HashMap<>();
        List<Map<String, Object>> points = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            String query = "SELECT pp.event_id, pp.latitude, pp.longitude, pp.timestamp, pp.flight_id, pp.altitude_agl, a.airframe as flight_airframe, pp.lateral_distance, pp.vertical_distance " +
                           "FROM proximity_points pp " +
                           "JOIN flights f ON pp.flight_id = f.id " +
                           "JOIN airframes a ON f.airframe_id = a.id " +
                           "WHERE pp.event_id = ? AND pp.flight_id = ? " +
                           "ORDER BY pp.timestamp";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, eventId);
                stmt.setInt(2, flightId);
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (first) {
                            eventMap.put("event_id", rs.getInt("event_id"));
                            eventMap.put("flight_id", rs.getInt("flight_id"));
                            eventMap.put("flight_airframe", rs.getString("flight_airframe"));
                            eventMap.put("lateral_distance", rs.getDouble("lateral_distance"));
                            eventMap.put("vertical_distance", rs.getDouble("vertical_distance"));
                            first = false;
                        }
                        Map<String, Object> point = new HashMap<>();
                        point.put("latitude", rs.getDouble("latitude"));
                        point.put("longitude", rs.getDouble("longitude"));
                        point.put("timestamp", rs.getTimestamp("timestamp").toString());
                        point.put("altitude_agl", rs.getDouble("altitude_agl"));
                        points.add(point);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        eventMap.put("points", points);
        return eventMap;
    }

    private static Timestamp parseDateOrTimestamp(String value, boolean endOfDay) {
        if (value == null || value.isEmpty()) return null;
        try {
            // Try full timestamp first
            return Timestamp.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Try as date only
            try {
                return Timestamp.valueOf(value + (endOfDay ? " 23:59:59" : " 00:00:00"));
            } catch (IllegalArgumentException ex) {
                LOG.severe("Failed to parse date/timestamp: " + value);
                return null;
            }
        }
    }

    /**
     * Get all proximity events within a bounding box
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLon minimum longitude
     * @param maxLon maximum longitude
     * @param startTime optional start time
     * @param endTime optional end time
     * @param minSeverity optional minimum severity
     * @param maxSeverity optional maximum severity
     * @return a list of proximity events within the bounding box
     * @throws SQLException if there is an error accessing the database
     */
    public static List<Map<String, Object>> getProximityEventsInBox(double minLat, double maxLat, double minLon, double maxLon, String startTime, String endTime, Double minSeverity, Double maxSeverity) throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            StringBuilder query = new StringBuilder("""
                SELECT e.id, e.flight_id, e.other_flight_id, e.start_time, e.end_time, 
                       e.severity,
                       f1.system_id as flight_system_id,
                       f2.system_id as other_flight_system_id,
                       f1.fleet_id,
                       a1.airframe as flight_airframe,
                       a2.airframe as other_flight_airframe,
                       em1.value as lateral_distance,
                       em2.value as vertical_distance,
                       e.min_latitude, e.max_latitude, e.min_longitude, e.max_longitude
                FROM events e
                JOIN flights f1 ON e.flight_id = f1.id
                JOIN flights f2 ON e.other_flight_id = f2.id
                JOIN airframes a1 ON f1.airframe_id = a1.id
                JOIN airframes a2 ON f2.airframe_id = a2.id
                LEFT JOIN event_metadata em1 ON e.id = em1.event_id AND em1.key_id = (SELECT id FROM event_metadata_keys WHERE name = 'lateral_distance')
                LEFT JOIN event_metadata em2 ON e.id = em2.event_id AND em2.key_id = (SELECT id FROM event_metadata_keys WHERE name = 'vertical_distance')
                WHERE e.event_definition_id = ?
                  AND e.min_latitude >= ? AND e.max_latitude <= ?
                  AND e.min_longitude >= ? AND e.max_longitude <= ?
            """);
            if (startTime != null && !startTime.isEmpty()) {
                query.append(" AND e.start_time >= ?");
            }
            if (endTime != null && !endTime.isEmpty()) {
                query.append(" AND e.end_time <= ?");
            }
            if (minSeverity != null) {
                query.append(" AND e.severity >= ?");
            }
            if (maxSeverity != null) {
                query.append(" AND e.severity <= ?");
            }
            query.append(" ORDER BY e.start_time DESC");
            try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
                int idx = 1;
                statement.setInt(idx++, PROXIMITY_EVENT_DEFINITION_ID);
                statement.setDouble(idx++, minLat);
                statement.setDouble(idx++, maxLat);
                statement.setDouble(idx++, minLon);
                statement.setDouble(idx++, maxLon);
                if (startTime != null && !startTime.isEmpty()) {
                    Timestamp ts = parseDateOrTimestamp(startTime, false);
                    if (ts != null) statement.setTimestamp(idx++, ts);
                }
                if (endTime != null && !endTime.isEmpty()) {
                    Timestamp ts = parseDateOrTimestamp(endTime, true);
                    if (ts != null) statement.setTimestamp(idx++, ts);
                }
                if (minSeverity != null) {
                    statement.setDouble(idx++, minSeverity);
                }
                if (maxSeverity != null) {
                    statement.setDouble(idx++, maxSeverity);
                }
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
                        event.put("minLatitude", resultSet.getDouble("min_latitude"));
                        event.put("maxLatitude", resultSet.getDouble("max_latitude"));
                        event.put("minLongitude", resultSet.getDouble("min_longitude"));
                        event.put("maxLongitude", resultSet.getDouble("max_longitude"));
                        events.add(event);
                    }
                }
            }
        }
        return events;
    }

} 