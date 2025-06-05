package org.ngafid.service.proximity;

import org.ngafid.core.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.io.IOException;

public class ProximityEventService {
    private static final Logger LOG = Logger.getLogger(ProximityEventService.class.getName());
    private static final int PROXIMITY_EVENT_DEFINITION_ID = -1;

    /**
     * Get all proximity events across all fleets
     * @return a list of all proximity events
     * @throws SQLException if there is an error accessing the database
     */
    public static List<Map<String, Object>> getAllProximityEvents() throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
            String query = """
                SELECT e.id, e.flight_id, e.other_flight_id, e.start_time, e.end_time, 
                       e.severity,
                       f1.system_id as flight_system_id,
                       f2.system_id as other_flight_system_id,
                       f1.fleet_id,
                       em1.value as lateral_distance,
                       em2.value as vertical_distance
                FROM events e
                JOIN flights f1 ON e.flight_id = f1.id
                JOIN flights f2 ON e.other_flight_id = f2.id
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
                        event.put("lateralDistance", resultSet.getDouble("lateral_distance"));
                        event.put("verticalDistance", resultSet.getDouble("vertical_distance"));
                        events.add(event);
                    }
                }
            }
        }

        return events;
    }

    /**
     * Get coordinates for a flight within a specific time range
     * @param flightId the ID of the flight
     * @param startTime the start time in Unix timestamp (seconds)
     * @param endTime the end time in Unix timestamp (seconds)
     * @return a map containing coordinates and timestamps
     * @throws SQLException if there is an error accessing the database
     * @throws IOException if there is an error decompressing the data
     */
    public static Map<String, Object> getFlightCoordinatesForTimeRange(int flightId, long startTime, long endTime) throws SQLException, IOException {
        Map<String, Object> result = new HashMap<>();
        List<double[]> coordinates = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
            String query = """
                SELECT ds1.data as latitude_data, ds2.data as longitude_data, ds3.data as timestamp_data,
                       ds1.length as length
                FROM double_series ds1
                JOIN double_series ds2 ON ds1.flight_id = ds2.flight_id
                JOIN double_series ds3 ON ds1.flight_id = ds3.flight_id
                JOIN double_series_names dsn1 ON ds1.name_id = dsn1.id
                JOIN double_series_names dsn2 ON ds2.name_id = dsn2.id
                JOIN double_series_names dsn3 ON ds3.name_id = dsn3.id
                WHERE ds1.flight_id = ?
                AND dsn1.name = 'Latitude'
                AND dsn2.name = 'Longitude'
                AND dsn3.name = 'Unix Time Seconds'
            """;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, flightId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        byte[] latitudeData = resultSet.getBytes("latitude_data");
                        byte[] longitudeData = resultSet.getBytes("longitude_data");
                        byte[] timestampData = resultSet.getBytes("timestamp_data");
                        int size = resultSet.getInt("length");

                        // Convert byte arrays to arrays
                        double[] latitudes = org.ngafid.core.util.Compression.inflateDoubleArray(latitudeData, size);
                        double[] longitudes = org.ngafid.core.util.Compression.inflateDoubleArray(longitudeData, size);
                        
                        // Convert timestamp data to long array
                        double[] timestampsArray = org.ngafid.core.util.Compression.inflateDoubleArray(timestampData, size);
                        long[] timestampsLongArray = new long[size];
                        for (int i = 0; i < size; i++) {
                            timestampsLongArray[i] = (long)timestampsArray[i];
                        }

                        LOG.info("Start time: " + startTime + ", End time: " + endTime);
                        LOG.info("First timestamp in array: " + timestampsLongArray[0] + ", Last timestamp in array: " + timestampsLongArray[timestampsLongArray.length - 1]);
                        LOG.info("Total points before filtering: " + timestampsLongArray.length);

                        System.out.println("First timestamp in array: " + timestampsLongArray[0] + ", Last timestamp in array: " + timestampsLongArray[timestampsLongArray.length - 1]);

                        // Filter coordinates within time range
                        for (int i = 0; i < timestampsLongArray.length; i++) {
                            long timestamp = timestampsLongArray[i];
                            
                            if (timestamp >= startTime && timestamp <= endTime) {
                                double latitude = latitudes[i];
                                double longitude = longitudes[i];
                                
                                if (!Double.isNaN(latitude) && !Double.isNaN(longitude) 
                                    && latitude != 0.0 && longitude != 0.0) {
                                    coordinates.add(new double[]{longitude, latitude});
                                    timestamps.add(timestamp);
                                }
                            }
                        }

                        LOG.info("Total points after filtering: " + coordinates.size());
                        if (coordinates.size() == 0) {
                            LOG.warning("No coordinates found within time range. Check if timestamps are in the correct format (seconds vs milliseconds).");
                            LOG.warning("Start time: " + startTime + ", End time: " + endTime);
                            LOG.warning("First timestamp: " + timestampsLongArray[0] + ", Last timestamp: " + timestampsLongArray[timestampsLongArray.length - 1]);
                        }
                    }
                }
            }
        }

        result.put("coordinates", coordinates);
        result.put("timestamps", timestamps);
        return result;
    }
} 