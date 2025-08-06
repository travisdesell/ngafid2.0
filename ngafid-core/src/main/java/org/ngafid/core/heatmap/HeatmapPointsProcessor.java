package org.ngafid.core.heatmap;

import org.ngafid.core.Database;
import org.ngafid.core.Config;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventMetaData;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.agl_converter.MSLtoAGLConverter;


import java.sql.*;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import org.ngafid.core.util.TimeUtils;

public class HeatmapPointsProcessor {
    private static final Logger LOG = Logger.getLogger(HeatmapPointsProcessor.class.getName());

    /**
     * Main method for running the backfill process from command line.
     */
    public static void main(String[] args) {
        int batchSize = 100; // Default batch size
        Integer limit = null; // No limit by default

        System.out.println("Starting heatmap points backfill process...");

        try (Connection connection = Database.getConnection()) {
            List<Map<String, Object>> results = backfillAllMissingHeatmapPoints(
                connection, batchSize, limit);

            printResults(results);
        } catch (Exception e) {
            System.err.println("Error during backfill process: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printResults(List<Map<String, Object>> results) {
        int total = results.size();
        int success = 0;
        int skipped = 0;
        int error = 0;
        
        for (Map<String, Object> result : results) {
            String status = (String) result.get("status");
            switch (status) {
                case "success":
                    success++;
                    break;
                case "skipped":
                    skipped++;
                    break;
                case "error":
                    error++;
                    break;
            }
        }
        
        System.out.println("\n=== BACKFILL SUMMARY ===");
        System.out.println("Total events processed: " + total);
        System.out.println("Successful: " + success);
        System.out.println("Skipped (already had points): " + skipped);
        System.out.println("Errors: " + error);
        
        if (error > 0) {
            System.out.println("\n=== ERROR DETAILS ===");
            for (Map<String, Object> result : results) {
                if ("error".equals(result.get("status"))) {
                    System.out.println("Event " + result.get("event_id") + ": " + result.get("message"));
                }
            }
        }
    }

    /**
     * Converts MSL altitude to AGL altitude using terrain data.
     * This uses the lightweight MSLtoAGLConverter for accurate terrain elevation lookup.
     *
     * @param altitudeMSL MSL altitude in feet
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return AGL altitude in feet, or NaN if conversion not possible
     */
    private static double convertMSLToAGL(double altitudeMSL, double latitude, double longitude) {
        return  MSLtoAGLConverter.convertMSLToAGL(altitudeMSL, latitude, longitude);
    }

    /**
     * Get AGL by fetching MSL from database and converting it
     */
    private static double getAGLFromMSL(Connection connection, int flightId, double latitude, double longitude, OffsetDateTime timestamp) {
        try {
            // Get MSL value from the flight's time series at the given timestamp
            // Use approximate matching for coordinates and timestamp
            String sql = "SELECT altitude_msl FROM flight_data WHERE flight_id = ? " +
                        "AND ABS(latitude - ?) < 0.001 AND ABS(longitude - ?) < 0.001 " +
                        "AND utc_date_time = ? LIMIT 1";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, flightId);
                stmt.setDouble(2, latitude);
                stmt.setDouble(3, longitude);
                stmt.setString(4, timestamp.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double altitudeMSL = rs.getDouble("altitude_msl");
                        if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL) && altitudeMSL > 0) {
                            double convertedAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                            return convertedAGL;
                        }
                    } else {
                        LOG.info("No MSL data found for flight " + flightId + " at " + timestamp + " with coordinates " + latitude + ", " + longitude);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("Failed to get MSL for flight " + flightId + " at " + timestamp + ": " + e.getMessage());
        }
        return Double.NaN;
    }



    public static void insertCoordinatesForProximityEvents(Connection connection, List<Event> events, Map<Event, List<ProximityPointData>> mainFlightPointsMap, Map<Event, List<ProximityPointData>> otherFlightPointsMap) throws SQLException {

        String sql = "INSERT INTO heatmap_points (event_id, flight_id, latitude, longitude, timestamp, altitude_agl) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

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

                        // Handle altitude - try AGL first, fallback to MSL conversion if needed
                        double altitudeAGL = point.getAltitudeAGL();
                        if (Double.isNaN(altitudeAGL) || Double.isInfinite(altitudeAGL) || altitudeAGL <= 0.0) {
                            // AGL is invalid, try to get MSL from database and convert
                            double convertedAGL = getAGLFromMSL(connection, event.getFlightId(), point.getLatitude(), point.getLongitude(), point.getTimestamp());
                            if (!Double.isNaN(convertedAGL)) {
                                stmt.setDouble(6, convertedAGL);
                            } else {
                                stmt.setDouble(6, 0.0);
                            }
                        } else {
                            stmt.setDouble(6, altitudeAGL);
                        }

                        stmt.addBatch();
                        inserted++;
                    }
                }

                List<ProximityPointData> otherPoints = otherFlightPointsMap.get(event);


                if (otherPoints != null) {
                    for (ProximityPointData point : otherPoints) {
                        stmt.setInt(1, event.getId());
                        Integer otherFlightId = event.getOtherFlightId();
                        if (otherFlightId != null) {
                            stmt.setInt(2, otherFlightId);
                        } else {
                            stmt.setNull(2, java.sql.Types.INTEGER);
                        }
                        stmt.setDouble(3, point.getLatitude());
                        stmt.setDouble(4, point.getLongitude());
                        stmt.setTimestamp(5, Timestamp.from(point.getTimestamp().toInstant()));

                        // Handle altitude - try AGL first, fallback to MSL conversion if needed
                        double altitudeAGL = point.getAltitudeAGL();
                        if (Double.isNaN(altitudeAGL) || Double.isInfinite(altitudeAGL) || altitudeAGL <= 0.0) {
                            // AGL is invalid, try to get MSL from database and convert
                            double convertedAGL = getAGLFromMSL(connection, otherFlightId, point.getLatitude(), point.getLongitude(), point.getTimestamp());
                            if (!Double.isNaN(convertedAGL)) {
                                stmt.setDouble(6, convertedAGL);
                            } else {
                                stmt.setDouble(6, 0.0);
                            }
                        } else {
                            stmt.setDouble(6, altitudeAGL);
                        }

                        stmt.addBatch();
                        inserted++;
                    }
                }
            }
            stmt.executeBatch();
        }
    }


    /**
     * Insert coordinates for regular (non-proximity) events by extracting flight data points
     * during the event's time window.
     *
     * @param connection Database connection
     * @param events List of events to process
     * @param flight The flight containing the time series data
     * @throws SQLException if database operation fails
     */
    public static void insertCoordinatesForNonProximityEvents(Connection connection, List<Event> events, Flight flight) throws SQLException {

        String sql = "INSERT INTO heatmap_points (event_id, flight_id, latitude, longitude, timestamp, altitude_agl) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int inserted = 0;

            Map<String, DoubleTimeSeries> doubleTimeSeries = flight.getDoubleTimeSeriesMap();

            DoubleTimeSeries latitudeSeries = doubleTimeSeries.get(Parameters.LATITUDE);
            DoubleTimeSeries longitudeSeries = doubleTimeSeries.get(Parameters.LONGITUDE);
            DoubleTimeSeries altitudeAGLSeries = doubleTimeSeries.get(Parameters.ALT_AGL);
            DoubleTimeSeries altitudeMSLSeries = doubleTimeSeries.get(Parameters.ALT_MSL);

            Map<String, StringTimeSeries> stringTimeSeries = flight.getStringTimeSeriesMap();

            StringTimeSeries timestampSeries = stringTimeSeries.get(Parameters.UTC_DATE_TIME);

            if (latitudeSeries == null || longitudeSeries == null || timestampSeries == null) {
                return;
            }

            for (Event event : events) {
                // Extract points from startLine to endLine
                for (int i = event.getStartLine(); i <= event.getEndLine() && i < latitudeSeries.size() && i < longitudeSeries.size() && i < timestampSeries.size(); i++) {
                    double latitude = latitudeSeries.get(i);
                    double longitude = longitudeSeries.get(i);


                    // Skip invalid coordinates (0,0, NaN, or infinite values)
                    if (latitude == 0.0 && longitude == 0.0 ||
                        Double.isNaN(latitude) || Double.isNaN(longitude) ||
                        Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                        continue;
                    }

                    stmt.setInt(1, event.getId());
                    stmt.setInt(2, event.getFlightId());
                    stmt.setDouble(3, latitude);
                    stmt.setDouble(4, longitude);

                    // Convert timestamp string to Timestamp
                    String timestampStr = timestampSeries.get(i);
                    if (timestampStr != null) {
                        try {
                            // Parse ISO 8601 timestamp to Timestamp
                            OffsetDateTime odt = OffsetDateTime.parse(timestampStr);
                            stmt.setTimestamp(5, Timestamp.from(odt.toInstant()));
                        } catch (Exception e) {
                            continue;
                        }
                    } else {
                        stmt.setNull(5, java.sql.Types.TIMESTAMP);
                    }

                    // Set altitude - try AGL first, fallback to MSL conversion if AGL is not available
                    double altitude = Double.NaN;
                    if (altitudeAGLSeries != null && i < altitudeAGLSeries.size()) {
                        altitude = altitudeAGLSeries.get(i);
                        if (!Double.isNaN(altitude) && !Double.isInfinite(altitude) && altitude > 0.0) {
                            // AGL is available and valid (greater than 0)
                            stmt.setDouble(6, altitude);
                        } else if (altitudeMSLSeries != null && i < altitudeMSLSeries.size()) {
                            // AGL is invalid, try MSL conversion
                            double altitudeMSL = altitudeMSLSeries.get(i);
                            if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL)) {
                                // Convert MSL to AGL using approximation
                                double convertedAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                                if (!Double.isNaN(convertedAGL)) {
                                    stmt.setDouble(6, convertedAGL);
                                } else {
                                    stmt.setDouble(6, 0.0);
                                }
                            } else {
                                stmt.setDouble(6, 0.0);
                            }
                        } else {
                            stmt.setDouble(6, 0.0);
                        }
                    } else if (altitudeMSLSeries != null && i < altitudeMSLSeries.size()) {
                        // AGL not available, try MSL conversion
                        double altitudeMSL = altitudeMSLSeries.get(i);
                        if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL)) {
                            double convertedAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                            if (!Double.isNaN(convertedAGL)) {
                                stmt.setDouble(6, convertedAGL);
                            } else {
                                stmt.setDouble(6, 0.0);
                            }
                        } else {
                            stmt.setDouble(6, 0.0);
                        }
                    } else {
                        stmt.setDouble(6, 0.0);
                    }

                    stmt.addBatch();
                    inserted++;
                }
            }

            stmt.executeBatch();
        }
    }


    // Fetches proximity points for a given event_id and flight_id
    public static Map<String, Object> getCoordinates(int eventId, int flightId) {
        Map<String, Object> eventMap = new HashMap<>();
        List<Map<String, Object>> points = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            String query = "SELECT pp.event_id, pp.latitude, pp.longitude, pp.timestamp, pp.flight_id, pp.altitude_agl, a.airframe as flight_airframe " +
                           "FROM heatmap_points pp " +
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
            LOG.severe("SQL error in getCoordinates for event_id=" + eventId + ", flight_id=" + flightId + ": " + e.getMessage());
            e.printStackTrace();
        }
        eventMap.put("points", points);
        return eventMap;
    }


    /**
     * Gets the relevant column names for a given event ID and flight ID.
     * This method retrieves the event definition and returns the column_names
     * that are relevant for this type of event.
     *
     * @param eventId The event ID
     * @param flightId The flight ID
     * @return Map containing event information and relevant column names
     */
    public static Map<String, Object> getEventDefinitionColumns(int eventId, int flightId) {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = Database.getConnection()) {
            // First, get the event definition ID from the events table
            String eventQuery = "SELECT e.event_definition_id, e.start_line, e.end_line, ed.column_names " +
                               "FROM events e " +
                               "JOIN event_definitions ed ON e.event_definition_id = ed.id " +
                               "WHERE e.id = ? AND e.flight_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(eventQuery)) {
                stmt.setInt(1, eventId);
                stmt.setInt(2, flightId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int eventDefinitionId = rs.getInt("event_definition_id");
                        int startLine = rs.getInt("start_line");
                        int endLine = rs.getInt("end_line");
                        String columnNamesJson = rs.getString("column_names");
                        
                        // Parse the JSON column names
                        List<String> columnNames = new ArrayList<>();
                        if (columnNamesJson != null && !columnNamesJson.isEmpty()) {
                            try {
                                // Simple JSON array parsing for column names
                                // Remove brackets and split by comma, then clean up quotes
                                String cleaned = columnNamesJson.replaceAll("[\\[\\]\"]", "");
                                String[] names = cleaned.split(",");
                                for (String name : names) {
                                    String trimmed = name.trim();
                                    if (!trimmed.isEmpty()) {
                                        columnNames.add(trimmed);
                                    }
                                }
                            } catch (Exception e) {
                                // Failed to parse column_names JSON
                            }
                        }
                        
                        result.put("event_id", eventId);
                        result.put("flight_id", flightId);
                        result.put("event_definition_id", eventDefinitionId);
                        result.put("start_line", startLine);
                        result.put("end_line", endLine);
                        result.put("column_names", columnNames);
                    } else {
                        result.put("error", "Event not found");
                    }
                }
            }
        } catch (SQLException e) {
            LOG.severe("SQL error in getEventDefinitionColumns for event_id=" + eventId + ", flight_id=" + flightId + ": " + e.getMessage());
            e.printStackTrace();
            result.put("error", "Database error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Gets the relevant column names and their values for a given event ID, flight ID, and timestamp.
     * This method retrieves the event definition, finds the time index, and returns the values for the relevant columns.
     *
     * @param eventId The event ID
     * @param flightId The flight ID
     * @param timestamp The timestamp in ISO 8601 or MySQL format
     * @return Map containing event information, relevant column names, and their values
     */
    public static Map<String, Object> getEventColumnsValues(int eventId, int flightId, String timestamp) {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = Database.getConnection()) {
            // Get event definition and column names
            Map<String, Object> eventDefinition = getEventDefinitionColumns(eventId, flightId);
            
            if (eventDefinition.containsKey("error")) {
                result.put("error", eventDefinition.get("error"));
                return result;
            }
            
            @SuppressWarnings("unchecked")
            List<String> columnNames = (List<String>) eventDefinition.get("column_names");
            
            if (columnNames == null || columnNames.isEmpty()) {
                result.put("error", "No relevant columns found for this event");
                return result;
            }
            
            // Get event details to find the time range
            String eventQuery = "SELECT start_line, end_line FROM events WHERE id = ? AND flight_id = ?";
            int startLine = -1;
            int endLine = -1;
            
            try (PreparedStatement stmt = connection.prepareStatement(eventQuery)) {
                stmt.setInt(1, eventId);
                stmt.setInt(2, flightId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        startLine = rs.getInt("start_line");
                        endLine = rs.getInt("end_line");
                    } else {
                        result.put("error", "Event not found");
                        return result;
                    }
                }
            }
            
            // Find the time index within the event range
            int timeIndex = getTimeIndexInRange(connection, flightId, timestamp, startLine, endLine);
            
            if (timeIndex == -1) {
                // If exact match fails, use the middle of the event range as fallback
                timeIndex = startLine + (endLine - startLine) / 2;
            }
            
            // Get values for each column
            Map<String, Object> columnValues = new HashMap<>();
            for (String columnName : columnNames) {
                try {
                    DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, columnName);
                    if (series != null) {
                        if (timeIndex < series.size()) {
                            double value = series.get(timeIndex);
                            columnValues.put(columnName, value);
                        } else {
                            columnValues.put(columnName, null);
                        }
                    } else {
                        columnValues.put(columnName, null);
                    }
                } catch (Exception e) {
                    columnValues.put(columnName, null);
                }
            }
            
            result.put("event_id", eventId);
            result.put("flight_id", flightId);
            result.put("timestamp", timestamp);
            result.put("time_index", timeIndex);
            result.put("column_names", columnNames);
            result.put("column_values", columnValues);
            
            return result;
            
        } catch (SQLException e) {
            LOG.severe("SQL error in getEventColumnsValues for event_id=" + eventId + ", flight_id=" + flightId + ": " + e.getMessage());
            e.printStackTrace();
            result.put("error", "Database error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Gets the time index for a given timestamp within a specific event's time range.
     * This is a helper method to find the correct time index within the event's data.
     *
     * @param connection Database connection
     * @param flightId Flight ID
     * @param timestamp ISO 8601 timestamp string or MySQL format timestamp
     * @param startLine The start line of the event in the flight's data
     * @param endLine The end line of the event in the flight's data
     * @return Time index, or -1 if not found
     */
    private static int getTimeIndexInRange(Connection connection, int flightId, String timestamp, int startLine, int endLine) {
        try {
            // Get UTC_DATE_TIME series
            StringTimeSeries timestampSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, Parameters.UTC_DATE_TIME);
            if (timestampSeries == null) {
                return -1;
            }
            
            // Parse the input timestamp - try different formats
            OffsetDateTime targetTime = null;
            try {
                // First try ISO 8601 format
                targetTime = OffsetDateTime.parse(timestamp);
            } catch (Exception e1) {
                try {
                    // Try MySQL format (yyyy-MM-dd HH:mm:ss.S) and convert to ISO 8601
                    String cleanedTimestamp = timestamp.replaceAll("\\.0$", ""); // Remove .0 suffix
                    LocalDateTime localDateTime = LocalDateTime.parse(cleanedTimestamp, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    targetTime = localDateTime.atOffset(ZoneOffset.UTC);
                } catch (Exception e2) {
                    return -1;
                }
            }
            
            // Convert target time to ISO 8601 string format for comparison
            String targetTimeString = targetTime.format(TimeUtils.ISO_8601_FORMAT);
            
            // Find exact timestamp match within the event range
            for (int i = startLine; i <= endLine; i++) {
                String seriesTimestamp = timestampSeries.get(i);
                if (seriesTimestamp != null && !seriesTimestamp.isEmpty()) {
                    // Compare as strings first (exact match)
                    if (seriesTimestamp.equals(targetTimeString)) {
                        return i;
                    }
                }
            }
            
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }


    public static List<Map<String, Object>> getEvents(
        String airframe, List<Integer> eventDefinitionIds, java.sql.Date startDate, java.sql.Date endDate,
        double areaMinLat, double areaMaxLat, double areaMinLon, double areaMaxLon,
        Double minSeverity, Double maxSeverity
    ) throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT e.id, e.fleet_id, e.flight_id, e.event_definition_id, e.other_flight_id, e.start_line, e.end_line, e.start_time, e.end_time, e.severity, e.min_latitude, e.max_latitude, e.min_longitude, e.max_longitude, a.airframe as airframe_name, oa.airframe as other_airframe_name " +
                "FROM events e " +
                "JOIN flights f ON e.flight_id = f.id " +
                "JOIN airframes a ON f.airframe_id = a.id " +
                "LEFT JOIN flights ofl ON e.other_flight_id = ofl.id " +
                "LEFT JOIN airframes oa ON ofl.airframe_id = oa.id " +
                "WHERE DATE(e.start_time) BETWEEN ? AND ? " +
                "AND e.min_latitude <= ? AND e.max_latitude >= ? " +
                "AND e.min_longitude <= ? AND e.max_longitude >= ?"
            );
            if (airframe != null && !airframe.isEmpty() && !airframe.equals("All Airframes")) {
                sql.append(" AND a.airframe = ?");
            }
            if (eventDefinitionIds != null && !eventDefinitionIds.isEmpty()) {
                sql.append(" AND e.event_definition_id IN (");
                for (int i = 0; i < eventDefinitionIds.size(); i++) {
                    if (i > 0) sql.append(",");
                    sql.append("?");
                }
                sql.append(")");
            }
            if (minSeverity != null) {
                sql.append(" AND e.severity >= ?");
            }
            if (maxSeverity != null) {
                sql.append(" AND e.severity <= ?");
            }
            sql.append(" ORDER BY e.id");
            
            try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                int idx = 1;
                stmt.setDate(idx++, startDate);
                stmt.setDate(idx++, endDate);
                stmt.setDouble(idx++, areaMaxLat);
                stmt.setDouble(idx++, areaMinLat);
                stmt.setDouble(idx++, areaMaxLon);
                stmt.setDouble(idx++, areaMinLon);
                if (airframe != null && !airframe.isEmpty() && !airframe.equals("All Airframes")) {
                    stmt.setString(idx++, airframe);
                }
                if (eventDefinitionIds != null && !eventDefinitionIds.isEmpty()) {
                    for (Integer eventDefinitionId : eventDefinitionIds) {
                        stmt.setInt(idx++, eventDefinitionId);
                    }
                }
                if (minSeverity != null) {
                    stmt.setDouble(idx++, minSeverity);
                }
                if (maxSeverity != null) {
                    stmt.setDouble(idx++, maxSeverity);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> event = new HashMap<>();
                        event.put("id", rs.getInt("id"));
                        event.put("fleet_id", rs.getInt("fleet_id"));
                        event.put("flight_id", rs.getInt("flight_id"));
                        event.put("event_definition_id", rs.getInt("event_definition_id"));
                        event.put("other_flight_id", rs.getInt("other_flight_id"));
                        event.put("start_line", rs.getInt("start_line"));
                        event.put("end_line", rs.getInt("end_line"));
                        event.put("start_time", rs.getTimestamp("start_time"));
                        event.put("end_time", rs.getTimestamp("end_time"));
                        event.put("severity", rs.getDouble("severity"));
                        event.put("min_latitude", rs.getDouble("min_latitude"));
                        event.put("max_latitude", rs.getDouble("max_latitude"));
                        event.put("min_longitude", rs.getDouble("min_longitude"));
                        event.put("max_longitude", rs.getDouble("max_longitude"));
                        event.put("airframe", rs.getString("airframe_name"));
                        event.put("otherAirframe", rs.getString("other_airframe_name"));
                        events.add(event);
                    }
                }
            }
        }
        return events;
    }



    /**
     * Backfills heatmap points for all events that don't have heatmap points.
     * This method is designed for migrating existing events that don't have heatmap points.
     *
     * @param connection Database connection
     * @param batchSize Number of events to process in each batch (default: 100)
     * @param limit Maximum number of events to process (null for all)
     * @return List of results for each event
     * @throws SQLException if database operation fails
     */
    public static List<Map<String, Object>> backfillAllMissingHeatmapPoints(Connection connection, int batchSize, Integer limit) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Get all events that don't have heatmap points
        String sql = "SELECT e.id FROM events e " +
                     "LEFT JOIN heatmap_points hp ON e.id = hp.event_id " +
                     "WHERE hp.event_id IS NULL " +
                     "ORDER BY e.id";
        
        if (limit != null) {
            sql += " LIMIT " + limit;
        }
        
        List<Integer> eventIds = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    eventIds.add(rs.getInt("id"));
                }
            }
        }
        
        LOG.info("Found " + eventIds.size() + " events without heatmap points");
        
        if (eventIds.isEmpty()) {
            LOG.info("No events found that need backfilling");
            return results;
        }
        
        // Process events in batches
        for (int i = 0; i < eventIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, eventIds.size());
            List<Integer> batch = eventIds.subList(i, endIndex);
            
            LOG.info("Processing batch " + (i / batchSize + 1) + " with " + batch.size() + " events");
            
            for (Integer eventId : batch) {
                try {
                    Map<String, Object> result = backfillSingleEvent(connection, eventId);
                    results.add(result);
                    
                    // Log progress
                    String status = (String) result.get("status");
                    if ("success".equals(status)) {
                        LOG.info("Successfully backfilled event " + eventId);
                    } else if ("skipped".equals(status)) {
                        LOG.info("Skipped event " + eventId + " (already has points)");
                    } else {
                        LOG.warning("Failed to backfill event " + eventId + ": " + result.get("message"));
                    }
                    
                } catch (Exception e) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("event_id", eventId);
                    errorResult.put("status", "error");
                    errorResult.put("message", "Exception during backfill: " + e.getMessage());
                    errorResult.put("exception", e);
                    results.add(errorResult);
                    LOG.severe("Exception backfilling event " + eventId + ": " + e.getMessage());
                    e.printStackTrace(); // Add stack trace to see the full error
                }
            }
        }
        
        return results;
    }

    /**
     * Backfills heatmap points for a single event by checking if points exist and inserting them if missing.
     *
     * @param connection Database connection
     * @param eventId The event ID to backfill
     * @return Map containing the result of the backfill operation
     * @throws SQLException if database operation fails
     */
    private static Map<String, Object> backfillSingleEvent(Connection connection, int eventId) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // First, check if heatmap points already exist for this event
            String checkSql = "SELECT COUNT(*) as count FROM heatmap_points WHERE event_id = ?";
            int existingPoints = 0;
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, eventId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        existingPoints = rs.getInt("count");
                    }
                }
            }
            
            if (existingPoints > 0) {
                result.put("event_id", eventId);
                result.put("status", "skipped");
                result.put("message", "Heatmap points already exist for event " + eventId + " (" + existingPoints + " points)");
                result.put("existing_points", existingPoints);
                return result;
            }
            
            // Get event details
            String eventSql = "SELECT e.*, ed.id as event_def_id FROM events e " +
                             "LEFT JOIN event_definitions ed ON e.event_definition_id = ed.id " +
                             "WHERE e.id = ?";
            
            Event event = null;
            Flight flight = null;
            int eventDefinitionId = -1;
            
            try (PreparedStatement eventStmt = connection.prepareStatement(eventSql)) {
                eventStmt.setInt(1, eventId);
                try (ResultSet rs = eventStmt.executeQuery()) {
                    if (rs.next()) {
                        eventDefinitionId = rs.getInt("event_definition_id");
                        int flightId = rs.getInt("flight_id");
                        
                        // Load the flight
                        flight = Flight.getFlight(connection, flightId);
                        if (flight == null) {
                            result.put("event_id", eventId);
                            result.put("status", "error");
                            result.put("message", "Flight not found for event " + eventId);
                            return result;
                        }
                        
                        // Create event object using the constructor that takes all required fields
                        int fleetId = rs.getInt("fleet_id");
                        int startLine = rs.getInt("start_line");
                        int endLine = rs.getInt("end_line");
                        String startTime = rs.getString("start_time");
                        String endTime = rs.getString("end_time");
                        double severity = rs.getDouble("severity");
                        Integer otherFlightId = rs.getObject("other_flight_id") != null ? rs.getInt("other_flight_id") : null;
                        
                        // Convert MySQL datetime format to ISO 8601 format
                        String isoStartTime = convertToISO8601(startTime);
                        String isoEndTime = convertToISO8601(endTime);
                        
                        LOG.info("Creating Event with: eventId=" + eventId + ", fleetId=" + fleetId + ", flightId=" + flightId + 
                                ", eventDefinitionId=" + eventDefinitionId + ", startLine=" + startLine + ", endLine=" + endLine + 
                                ", isoStartTime=" + isoStartTime + ", isoEndTime=" + isoEndTime + ", severity=" + severity + 
                                ", otherFlightId=" + otherFlightId);
                        
                        event = new Event(eventId, fleetId, flightId, eventDefinitionId, startLine, endLine, 
                                        isoStartTime, isoEndTime, severity, otherFlightId);
                        
                        LOG.info("Event created successfully");
                    } else {
                        result.put("event_id", eventId);
                        result.put("status", "error");
                        result.put("message", "Event not found: " + eventId);
                        return result;
                    }
                }
            }
            
            // Process based on event type
            LOG.info("Event definition ID: " + eventDefinitionId);
            if (eventDefinitionId == -1) {
                // Proximity event - reconstruct proximity data from event metadata
                LOG.info("Processing as proximity event");
                result = backfillProximityEvent(connection, event, flight);
            } else {
                // Regular event - use the existing non-proximity logic
                LOG.info("Processing as regular event (eventDefinitionId=" + eventDefinitionId + ")");
                List<Event> events = Arrays.asList(event);
                
                // Load the required time series data
                DoubleTimeSeries latitudeSeries = flight.getDoubleTimeSeries(connection, Parameters.LATITUDE);
                DoubleTimeSeries longitudeSeries = flight.getDoubleTimeSeries(connection, Parameters.LONGITUDE);
                StringTimeSeries timestampSeries = flight.getStringTimeSeries(connection, Parameters.UTC_DATE_TIME);
                DoubleTimeSeries altitudeAGLSeries = flight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
                DoubleTimeSeries altitudeMSLSeries = flight.getDoubleTimeSeries(connection, Parameters.ALT_MSL);

                // Check if we have the required data
                if (latitudeSeries == null || longitudeSeries == null || timestampSeries == null) {
                    result.put("event_id", eventId);
                    result.put("status", "error");
                    result.put("message", "Missing required time series data for flight " + flight.getId());
                    return result;
                }
                

                insertCoordinatesForNonProximityEvents(connection, events, flight);
                
                // Count and print the inserted points
                String countSql = "SELECT COUNT(*) as count FROM heatmap_points WHERE event_id = ?";
                int insertedPoints = 0;
                try (PreparedStatement countStmt = connection.prepareStatement(countSql)) {
                    countStmt.setInt(1, eventId);
                    try (ResultSet rs = countStmt.executeQuery()) {
                        if (rs.next()) {
                            insertedPoints = rs.getInt("count");
                        }
                    }
                }
                
                System.out.println("Event " + eventId + " backfilled: " + insertedPoints + " points inserted");
                
                result.put("event_id", eventId);
                result.put("status", "success");
                result.put("message", "Successfully backfilled " + insertedPoints + " heatmap points for event " + eventId);
                result.put("inserted_points", insertedPoints);
                result.put("event_type", "regular");
            }
            
        } catch (Exception e) {
            result.put("event_id", eventId);
            result.put("status", "error");
            result.put("message", "Error backfilling event " + eventId + ": " + e.getMessage());
            result.put("exception", e);
        }
        
        return result;
    }

    /**
     * Converts MySQL datetime format to ISO 8601 format.
     * 
     * @param mysqlDateTime MySQL datetime string (e.g., "2025-06-13 17:59:04")
     * @return ISO 8601 formatted string (e.g., "2025-06-13T17:59:04Z")
     */
    private static String convertToISO8601(String mysqlDateTime) {
        LOG.info("Converting datetime: " + mysqlDateTime);
        
        if (mysqlDateTime == null || mysqlDateTime.trim().isEmpty()) {
            LOG.warning("Empty datetime string");
            return null;
        }
        
        try {
            // Parse MySQL datetime format and convert to ISO 8601
            LocalDateTime localDateTime = LocalDateTime.parse(mysqlDateTime, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String result = localDateTime.atOffset(ZoneOffset.UTC).format(TimeUtils.ISO_8601_FORMAT);
            return result;
        } catch (Exception e) {
            LOG.warning("Failed to convert datetime: " + mysqlDateTime + ", error: " + e.getMessage());
            // Return original if conversion fails
            return mysqlDateTime;
        }
    }

    /**
     * Backfills heatmap points for a proximity event by reconstructing the proximity data
     * from event metadata and flight data.
     *
     * @param connection Database connection
     * @param event The proximity event to backfill
     * @param mainFlight The main flight
     * @return Map containing the result of the backfill operation
     * @throws SQLException if database operation fails
     */
    private static Map<String, Object> backfillProximityEvent(Connection connection, Event event, Flight mainFlight) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        int eventId = event.getId();
        
        LOG.info("Starting backfillProximityEvent for event " + eventId);
        
        try {
            // Get the other flight ID
            Integer otherFlightId = event.getOtherFlightId();
            LOG.info("Other flight ID: " + otherFlightId);
            
            if (otherFlightId == null || otherFlightId == -1) {
                result.put("event_id", eventId);
                result.put("status", "error");
                result.put("message", "Proximity event " + eventId + " has no valid other_flight_id");
                return result;
            }
            
            // Load the other flight
            Flight otherFlight = Flight.getFlight(connection, otherFlightId);
            LOG.info("Other flight loaded: " + (otherFlight != null ? "OK" : "NULL"));
            
            if (otherFlight == null) {
                result.put("event_id", eventId);
                result.put("status", "error");
                result.put("message", "Other flight " + otherFlightId + " not found for proximity event " + eventId);
                return result;
            }
            
            // Get event metadata (lateral and vertical distances)
            List<EventMetaData> metaDataList = EventMetaData.getEventMetaData(connection, eventId);
            double lateralDistance = 0.0;
            double verticalDistance = 0.0;
            
            for (EventMetaData metaData : metaDataList) {
                if (metaData.getName() == EventMetaData.EventMetaDataKey.LATERAL_DISTANCE) {
                    lateralDistance = metaData.getValue();
                } else if (metaData.getName() == EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE) {
                    verticalDistance = metaData.getValue();
                }
            }
            
            // Extract flight data for the event time window
            DoubleTimeSeries mainLatitudeSeries = mainFlight.getDoubleTimeSeries(connection, Parameters.LATITUDE);
            DoubleTimeSeries mainLongitudeSeries = mainFlight.getDoubleTimeSeries(connection, Parameters.LONGITUDE);
            DoubleTimeSeries mainAltitudeAGLSeries = mainFlight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
            DoubleTimeSeries mainAltitudeMSLSeries = mainFlight.getDoubleTimeSeries(connection, Parameters.ALT_MSL);
            StringTimeSeries mainTimestampSeries = mainFlight.getStringTimeSeries(connection, Parameters.UTC_DATE_TIME);
            
            DoubleTimeSeries otherLatitudeSeries = otherFlight.getDoubleTimeSeries(connection, Parameters.LATITUDE);
            DoubleTimeSeries otherLongitudeSeries = otherFlight.getDoubleTimeSeries(connection, Parameters.LONGITUDE);
            DoubleTimeSeries otherAltitudeAGLSeries = otherFlight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
            DoubleTimeSeries otherAltitudeMSLSeries = otherFlight.getDoubleTimeSeries(connection, Parameters.ALT_MSL);
            StringTimeSeries otherTimestampSeries = otherFlight.getStringTimeSeries(connection, Parameters.UTC_DATE_TIME);

            
            if (mainLatitudeSeries == null || mainLongitudeSeries == null || mainTimestampSeries == null ||
                otherLatitudeSeries == null || otherLongitudeSeries == null || otherTimestampSeries == null) {
                result.put("event_id", eventId);
                result.put("status", "error");
                result.put("message", "Missing required flight data for proximity event " + eventId);
                return result;
            }
            
            // Create ProximityPointData lists
            List<ProximityPointData> mainFlightPoints = new ArrayList<>();
            List<ProximityPointData> otherFlightPoints = new ArrayList<>();
            
            // Extract main flight points
            for (int i = event.getStartLine(); i <= event.getEndLine() && i < mainLatitudeSeries.size() && i < mainLongitudeSeries.size() && i < mainTimestampSeries.size(); i++) {
                double latitude = mainLatitudeSeries.get(i);
                double longitude = mainLongitudeSeries.get(i);
                
                // Skip invalid coordinates
                if (latitude == 0.0 && longitude == 0.0 ||
                    Double.isNaN(latitude) || Double.isNaN(longitude) ||
                    Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                    continue;
                }
                
                String timestampStr = mainTimestampSeries.get(i);
                if (timestampStr == null) continue;
                
                try {
                    OffsetDateTime timestamp = OffsetDateTime.parse(timestampStr);
                    
                                         // Try to get AGL altitude first
                     double altitudeAGL = Double.NaN;
                     if (mainAltitudeAGLSeries != null && i < mainAltitudeAGLSeries.size()) {
                         altitudeAGL = mainAltitudeAGLSeries.get(i);
                         if (Double.isNaN(altitudeAGL) || Double.isInfinite(altitudeAGL) || altitudeAGL == 0.0) {
                             altitudeAGL = Double.NaN;
                             LOG.info("Point " + i + ": AGL is invalid (NaN, Infinite, or 0.0), setting to NaN");
                         }
                     } else {
                         LOG.info("Point " + i + ": AGL series not available");
                     }
                     
                     // If AGL is not available, try MSL conversion
                     if (Double.isNaN(altitudeAGL)) {
                         if (mainAltitudeMSLSeries != null && i < mainAltitudeMSLSeries.size()) {
                             double altitudeMSL = mainAltitudeMSLSeries.get(i);
                             if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL) && altitudeMSL > 0) {
                                 altitudeAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                                 LOG.info("Converted MSL " + altitudeMSL + " to AGL " + altitudeAGL + " for point " + i);
                             } else {
                                 LOG.info("Point " + i + ": MSL is invalid (NaN, Infinite, or <= 0)");
                             }
                         } else {
                             LOG.info("Point " + i + ": MSL series not available");
                         }
                     }


                     if (mainAltitudeAGLSeries != null && i < mainAltitudeAGLSeries.size()) {
                         double rawAGL = mainAltitudeAGLSeries.get(i);
                     }
                     if (mainAltitudeMSLSeries != null && i < mainAltitudeMSLSeries.size()) {
                         double rawMSL = mainAltitudeMSLSeries.get(i);
                     }
                     

                    
                    ProximityPointData point = new ProximityPointData(
                        latitude, longitude, timestamp, altitudeAGL);
                    mainFlightPoints.add(point);
                } catch (Exception e) {
                    // Skip invalid timestamps
                    continue;
                }
            }
            
            // Extract other flight points (use same time range)
            for (int i = event.getStartLine(); i <= event.getEndLine() && i < otherLatitudeSeries.size() && i < otherLongitudeSeries.size() && i < otherTimestampSeries.size(); i++) {
                double latitude = otherLatitudeSeries.get(i);
                double longitude = otherLongitudeSeries.get(i);
                
                // Skip invalid coordinates
                if (latitude == 0.0 && longitude == 0.0 ||
                    Double.isNaN(latitude) || Double.isNaN(longitude) ||
                    Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                    continue;
                }
                
                String timestampStr = otherTimestampSeries.get(i);
                if (timestampStr == null) continue;
                
                try {
                    OffsetDateTime timestamp = OffsetDateTime.parse(timestampStr);
                    
                    // Try to get AGL altitude first
                    double altitudeAGL = Double.NaN;
                    if (otherAltitudeAGLSeries != null && i < otherAltitudeAGLSeries.size()) {
                        altitudeAGL = otherAltitudeAGLSeries.get(i);
                        if (Double.isNaN(altitudeAGL) || Double.isInfinite(altitudeAGL) || altitudeAGL == 0.0) {
                            altitudeAGL = Double.NaN;
                        }
                    }
                    
                                         // If AGL is not available, try MSL conversion
                     if (Double.isNaN(altitudeAGL)) {
                         if (otherAltitudeMSLSeries != null && i < otherAltitudeMSLSeries.size()) {
                             double altitudeMSL = otherAltitudeMSLSeries.get(i);
                             if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL) && altitudeMSL > 0) {
                                 altitudeAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                             } else {
                                 LOG.info("Other flight point " + i + ": MSL is invalid (NaN, Infinite, or <= 0)");
                             }
                         } else {
                             LOG.info("Other flight point " + i + ": MSL series not available");
                         }
                     }
                    
                    ProximityPointData point = new ProximityPointData(
                        latitude, longitude, timestamp, altitudeAGL);
                    otherFlightPoints.add(point);
                } catch (Exception e) {
                    // Skip invalid timestamps
                    continue;
                }
            }
            
            // Create maps for the proximity event insertion
            Map<Event, List<ProximityPointData>> mainFlightPointsMap = new HashMap<>();
            Map<Event, List<ProximityPointData>> otherFlightPointsMap = new HashMap<>();
            
            mainFlightPointsMap.put(event, mainFlightPoints);
            otherFlightPointsMap.put(event, otherFlightPoints);
            
            // Insert the proximity points
            List<Event> events = Arrays.asList(event);
            insertCoordinatesForProximityEvents(connection, events, mainFlightPointsMap, otherFlightPointsMap);
            
            // Count and print the inserted points
            String countSql = "SELECT COUNT(*) as count FROM heatmap_points WHERE event_id = ?";
            int insertedPoints = 0;
            try (PreparedStatement countStmt = connection.prepareStatement(countSql)) {
                countStmt.setInt(1, eventId);
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        insertedPoints = rs.getInt("count");
                    }
                }
            }

            System.out.println("Event " + eventId + " backfilled: " + insertedPoints + " points inserted");
            
            result.put("event_id", eventId);
            result.put("status", "success");
            result.put("message", "Successfully backfilled " + insertedPoints + " proximity heatmap points for event " + eventId);
            result.put("inserted_points", insertedPoints);
            result.put("event_type", "proximity");
            result.put("main_flight_points", mainFlightPoints.size());
            result.put("other_flight_points", otherFlightPoints.size());
            result.put("lateral_distance_ft", lateralDistance);
            result.put("vertical_distance_ft", verticalDistance);
            
        } catch (Exception e) {
            result.put("event_id", eventId);
            result.put("status", "error");
            result.put("message", "Error backfilling proximity event " + eventId + ": " + e.getMessage());
            result.put("exception", e);
        }
        
        return result;
    }
}