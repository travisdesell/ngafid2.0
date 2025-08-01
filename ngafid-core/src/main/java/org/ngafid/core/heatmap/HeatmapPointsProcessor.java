package org.ngafid.core.heatmap;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.terrain.TerrainCache;
import org.ngafid.core.terrain.TerrainUnavailableException;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;

public class HeatmapPointsProcessor {
    private static final Logger LOG = Logger.getLogger(HeatmapPointsProcessor.class.getName());

    /**
     * Converts MSL altitude to AGL altitude using terrain data.
     * This uses the proper TerrainCache for accurate terrain elevation lookup.
     *
     * @param altitudeMSL MSL altitude in feet
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return AGL altitude in feet, or NaN if conversion not possible
     */
    private static double convertMSLToAGL(double altitudeMSL, double latitude, double longitude) {
        if (Double.isNaN(altitudeMSL) || Double.isInfinite(altitudeMSL) ||
            Double.isNaN(latitude) || Double.isInfinite(latitude) ||
            Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            return Double.NaN;
        }

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            LOG.warning("Invalid coordinates for MSL to AGL conversion: lat=" + latitude + ", lon=" + longitude);
            return Double.NaN;
        }

        //TODO: remove hardcoded return before production, uncomment the code below
        // Run out of memory.

        return 0;
       /*
        try {
            return TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);
        } catch (TerrainUnavailableException e) {
            LOG.warning("Terrain data unavailable for coordinates (" + latitude + ", " + longitude + "): " + e.getMessage());
            return Double.NaN;
        } catch (Exception e) {
            LOG.warning("Unexpected error converting MSL to AGL for coordinates (" + latitude + ", " + longitude + "): " + e.getMessage());
            return Double.NaN;
        }

        */
    }



    public static void insertCoordinatesForProximityEvents(Connection connection, List<Event> events, Map<Event, List<ProximityPointData>> mainFlightPointsMap, Map<Event, List<ProximityPointData>> otherFlightPointsMap) throws SQLException {


        String sql = "INSERT INTO heatmap_points (event_id, flight_id, latitude, longitude, timestamp, altitude_agl, lateral_distance, vertical_distance) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int inserted = 0;
            for (Event event : events) {
                LOG.info("Processing event ID: " + event.getId() + ", Flight ID: " + event.getFlightId());

                List<ProximityPointData> mainPoints = mainFlightPointsMap.get(event);
                LOG.info("Main points for event " + event.getId() + ": " + (mainPoints != null ? mainPoints.size() : "null"));

                if (mainPoints != null) {
                    for (ProximityPointData point : mainPoints) {
                        stmt.setInt(1, event.getId());
                        stmt.setInt(2, event.getFlightId());
                        stmt.setDouble(3, point.getLatitude());
                        stmt.setDouble(4, point.getLongitude());
                        stmt.setTimestamp(5, Timestamp.from(point.getTimestamp().toInstant()));

                        // Handle altitude - try AGL first, fallback to MSL if needed
                        double altitudeAGL = point.getAltitudeAGL();
                        if (Double.isNaN(altitudeAGL) || Double.isInfinite(altitudeAGL)) {
                            // Try to get MSL altitude from the point data if available
                            // Note: This assumes ProximityPointData might have MSL data
                            // If not, we'll set it to null
                            stmt.setNull(6, java.sql.Types.DOUBLE);
                            LOG.info("Invalid AGL altitude for proximity point, setting to null");
                        } else {
                            stmt.setDouble(6, altitudeAGL);
                        }

                        stmt.setDouble(7, point.getLateralDistance());
                        stmt.setDouble(8, point.getVerticalDistance());
                        stmt.addBatch();
                        inserted++;
                    }
                }

                List<ProximityPointData> otherPoints = otherFlightPointsMap.get(event);
                LOG.info("Other points for event " + event.getId() + ": " + (otherPoints != null ? otherPoints.size() : "null"));

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

                        // Handle altitude - try AGL first, set to null if not evailable
                        double altitudeAGL = point.getAltitudeAGL();
                        if (Double.isNaN(altitudeAGL) || Double.isInfinite(altitudeAGL)) {
                            stmt.setNull(6, java.sql.Types.DOUBLE);
                            LOG.info("Invalid AGL altitude for proximity point, setting to null");
                        } else {
                            stmt.setDouble(6, altitudeAGL);
                        }

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
                LOG.warning("Required coordinate columns not available for flight " + flight.getId());
                LOG.warning("latitudeSeries: " + (latitudeSeries == null ? "null" : "found"));
                LOG.warning("longitudeSeries: " + (longitudeSeries == null ? "null" : "found"));
                LOG.warning("timestampSeries: " + (timestampSeries == null ? "null" : "found"));
                return;
            }

            for (Event event : events) {
                LOG.info("Processing event ID: " + event.getId() + ", flightId: " + event.getFlightId() +
                        ", startLine: " + event.getStartLine() + ", endLine: " + event.getEndLine() +
                        ", eventDefinitionId: " + event.getEventDefinitionId());

                int validPointsForEvent = 0;
                int skippedPointsForEvent = 0;

                // Extract points from startLine to endLine
                for (int i = event.getStartLine(); i <= event.getEndLine() && i < latitudeSeries.size() && i < longitudeSeries.size() && i < timestampSeries.size(); i++) {
                    double latitude = latitudeSeries.get(i);
                    double longitude = longitudeSeries.get(i);


                    // Skip invalid coordinates (0,0, NaN, or infinite values)
                    if (latitude == 0.0 && longitude == 0.0 ||
                        Double.isNaN(latitude) || Double.isNaN(longitude) ||
                        Double.isInfinite(latitude) || Double.isInfinite(longitude)) {
                        LOG.warning("Skipping invalid coordinates at index " + i + ": lat=" + latitude + ", lon=" + longitude);
                        skippedPointsForEvent++;
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
                            LOG.warning("Failed to parse timestamp: " + timestampStr + " for event " + event.getId() + " at line " + i);
                            skippedPointsForEvent++;
                            continue;
                        }
                    } else {
                        stmt.setNull(5, java.sql.Types.TIMESTAMP);
                    }

                    // Set altitude - try AGL first, fallback to MSL conversion if AGL is not available
                    double altitude = Double.NaN;
                    if (altitudeAGLSeries != null && i < altitudeAGLSeries.size()) {
                        altitude = altitudeAGLSeries.get(i);
                        if (!Double.isNaN(altitude) && !Double.isInfinite(altitude)) {
                            // AGL is available and valid
                            stmt.setDouble(6, altitude);
                        } else if (altitudeMSLSeries != null && i < altitudeMSLSeries.size()) {
                            // AGL is invalid, try MSL conversion
                            double altitudeMSL = altitudeMSLSeries.get(i);
                            if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL)) {
                                                            // Convert MSL to AGL using approximation
                            double convertedAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                            if (!Double.isNaN(convertedAGL)) {
                                LOG.info("Converted MSL to AGL (approximation) for event " + event.getId() + " at index " + i + ": MSL=" + altitudeMSL + " -> AGL=" + convertedAGL);
                                stmt.setDouble(6, convertedAGL);
                            } else {
                                stmt.setNull(6, java.sql.Types.DOUBLE);
                            }
                            } else {
                                stmt.setNull(6, java.sql.Types.DOUBLE);
                            }
                        } else {
                            stmt.setNull(6, java.sql.Types.DOUBLE);
                        }
                    } else if (altitudeMSLSeries != null && i < altitudeMSLSeries.size()) {
                        // AGL not available, try MSL conversion
                        double altitudeMSL = altitudeMSLSeries.get(i);
                        if (!Double.isNaN(altitudeMSL) && !Double.isInfinite(altitudeMSL)) {

                            double convertedAGL = convertMSLToAGL(altitudeMSL, latitude, longitude);
                            if (!Double.isNaN(convertedAGL)) {
                                LOG.info("Converted MSL to AGL (approximation) for event " + event.getId() + " at index " + i + ": MSL=" + altitudeMSL + " -> AGL=" + convertedAGL);
                                stmt.setDouble(6, convertedAGL);
                            } else {
                                stmt.setNull(6, java.sql.Types.DOUBLE);
                            }
                        } else {
                            stmt.setNull(6, java.sql.Types.DOUBLE);
                        }
                    } else {
                        LOG.warning("No altitude data available for event " + event.getId() + " at index " + i);
                        stmt.setNull(6, java.sql.Types.DOUBLE);
                    }

                    stmt.addBatch();
                    inserted++;
                    validPointsForEvent++;
                }

                LOG.info("Event " + event.getId() + ": " + validPointsForEvent + " valid points, " + skippedPointsForEvent + " skipped points");
            }

            LOG.info("Executing batch insert for " + inserted + " total points...");
            stmt.executeBatch();
            LOG.info("Successfully inserted " + inserted + " points for regular events.");
        }
    }


    // Fetches proximity points for a given event_id and flight_id
    public static Map<String, Object> getCoordinates(int eventId, int flightId) {
        LOG.info("getProximityPointsForEventAndFlight called with event_id=" + eventId + ", flight_id=" + flightId);
        Map<String, Object> eventMap = new HashMap<>();
        List<Map<String, Object>> points = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            String query = "SELECT pp.event_id, pp.latitude, pp.longitude, pp.timestamp, pp.flight_id, pp.altitude_agl, a.airframe as flight_airframe, pp.lateral_distance, pp.vertical_distance " +
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
            LOG.info("Found " + points.size() + " points for event_id=" + eventId + ", flight_id=" + flightId);
        } catch (SQLException e) {
            LOG.severe("SQL error in getProximityPointsForEventAndFlight for event_id=" + eventId + ", flight_id=" + flightId + ": " + e.getMessage());
            e.printStackTrace();
        }
        eventMap.put("points", points);
        return eventMap;
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
            System.out.println("[DEBUG] getFilteredEvents SQL: " + sql.toString());
            System.out.println("[DEBUG] Parameters: startDate=" + startDate + ", endDate=" + endDate + ", areaMinLat=" + areaMinLat + ", areaMaxLat=" + areaMaxLat + ", areaMinLon=" + areaMinLon + ", areaMaxLon=" + areaMaxLon + ", airframe=" + airframe + ", eventDefinitionIds=" + eventDefinitionIds);
            System.out.println("[DEBUG] Spatial intersection logic: e.min_latitude <= " + areaMaxLat + " AND e.max_latitude >= " + areaMinLat + " AND e.min_longitude <= " + areaMaxLon + " AND e.max_longitude >= " + areaMinLon);
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
                        
                        // Debug: Print event coordinates
                        System.out.println("[DEBUG] Found event ID " + rs.getInt("id") + ": min_lat=" + rs.getDouble("min_latitude") + 
                            ", max_lat=" + rs.getDouble("max_latitude") + ", min_lon=" + rs.getDouble("min_longitude") + 
                            ", max_lon=" + rs.getDouble("max_longitude"));
                    }
                }
            }
            System.out.println("[DEBUG] getFilteredEvents found " + events.size() + " events.");
            System.out.println("[DEBUG] Event IDs found: " + events.stream().map(e -> e.get("id")).collect(java.util.stream.Collectors.toList()));
        }
        return events;
    }
}