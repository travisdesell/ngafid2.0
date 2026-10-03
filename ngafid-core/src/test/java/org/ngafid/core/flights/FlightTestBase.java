package org.ngafid.core.flights;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.ngafid.core.TestWithConnection;
import org.ngafid.core.util.filters.Filter;

/**
 * Shared test fixtures for the {@link Flight} test classes.
 *
 * <p>Holds the database seeding helpers (airframes, uploads, tails, flights, double/string time-series rows) and the
 * small {@link Filter} stubs that several {@code Flight*Test} classes rely on, so the per-feature test classes can stay
 * focused on assertions without duplicating setup. Extends {@link TestWithConnection}, so every subclass test still
 * receives a fresh {@code connection} against the Testcontainers MySQL instance before each test.
 */
public class FlightTestBase extends TestWithConnection {
    private static final Logger LOG = Logger.getLogger(FlightTestBase.class.getName());

    // Helper methods for test setup
    protected void createTestAirframeIfNotExists() throws SQLException {
        try (PreparedStatement stmt =
                connection.prepareStatement("SELECT COUNT(*) FROM airframes WHERE airframe = 'Test Cessna 172S'")) {
            try (var rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement typeStmt =
                            connection.prepareStatement("INSERT INTO airframe_types (name) VALUES ('Fixed Wing') "
                                    + "ON DUPLICATE KEY UPDATE name = name")) {
                        typeStmt.executeUpdate();
                    }

                    try (PreparedStatement airframeStmt = connection.prepareStatement(
                            "INSERT INTO airframes (airframe, type_id) VALUES ('Test Cessna 172S', "
                                    + "(SELECT id FROM airframe_types WHERE name = 'Fixed Wing' LIMIT 1)) "
                                    + "ON DUPLICATE KEY UPDATE airframe = airframe")) {
                        airframeStmt.executeUpdate();
                    }
                }
            }
        }
    }

    protected void createTestUploadIfNotExists() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM uploads WHERE id = 999")) {
            try (var rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement uploadStmt = connection.prepareStatement(
                            "INSERT INTO uploads (id, fleet_id, uploader_id, filename, identifier, status,"
                                    + " number_chunks, uploaded_chunks, chunk_status, md5_hash, size_bytes)"
                                    + " VALUES (999, 1, 1, 'test_upload.csv', 'test_identifier', 'PROCESSED_OK', 1, 1,"
                                    + " '1', 'test_md5_hash', 1024) ON DUPLICATE KEY UPDATE id = id")) {
                        uploadStmt.executeUpdate();
                    }
                }
            }
        }
    }

    protected void createTestFlightWithTimeSeriesData() throws SQLException {
        // First, create the test flight
        createTestFlight(999);

        setupTimeSeriesData(connection, 999);
    }

    /**
     * Creates a test flight with time series data that includes columns that should be skipped
     */
    protected void createTestFlightWithSkippedTimeSeriesData() throws SQLException {
        // First, create the test flight
        createTestFlight(998);

        setupTimeSeriesDataWithSkippedColumns(connection, 998);
    }

    /**
     * Creates a test flight with time series data where min == max (should be skipped)
     */
    protected void createTestFlightWithSameMinMaxTimeSeriesData() throws SQLException {
        // First, create the test flight
        createTestFlight(997);

        setupTimeSeriesDataWithSameMinMax(connection, 997);
    }

    /**
     * Creates a test flight in the database
     *
     * @param flightId flight ID to create
     */
    protected void createTestFlight(int flightId) throws SQLException {
        LOG.fine(() -> "Starting createTestFlight for flightId=" + flightId);

        // Ensure we have the necessary test data
        LOG.fine("Creating test airframe...");
        createTestAirframeIfNotExists();
        LOG.fine("Creating test upload...");
        createTestUploadIfNotExists();
        LOG.fine("Creating test tail...");
        createTestTailIfNotExists(flightId);

        // First, check if the flight already exists
        try (PreparedStatement checkStmt = connection.prepareStatement("SELECT COUNT(*) FROM flights WHERE id = ?")) {
            checkStmt.setInt(1, flightId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    LOG.fine(() -> "Flight " + flightId + " already exists, returning");
                    // Flight already exists, return
                    return;
                }
            }
        }

        // Debug: Check if dependencies exist
        try (PreparedStatement debugStmt = connection.prepareStatement("SELECT COUNT(*) FROM airframes")) {
            try (ResultSet rs = debugStmt.executeQuery()) {
                if (rs.next()) {
                    int airframeCount = rs.getInt(1);
                    if (airframeCount == 0) {
                        throw new SQLException("No airframes found - createTestAirframeIfNotExists failed");
                    }
                }
            }
        }
        try (PreparedStatement debugStmt = connection.prepareStatement("SELECT COUNT(*) FROM uploads WHERE id = 999")) {
            try (ResultSet rs = debugStmt.executeQuery()) {
                if (rs.next()) {
                    int uploadCount = rs.getInt(1);
                    if (uploadCount == 0) {
                        throw new SQLException("Upload 999 not found - createTestUploadIfNotExists failed");
                    }
                }
            }
        }

        // Get the airframe ID. Select "Cessna 172S" explicitly (rather than the first row by
        // LIMIT 1, whose ordering is not guaranteed) so the flight's airframe is deterministic
        // and matches what the airframe assertions below expect.
        int airframeId = 1; // Default to 1
        try (PreparedStatement airframeStmt =
                connection.prepareStatement("SELECT id FROM airframes WHERE airframe = 'Cessna 172S' LIMIT 1")) {
            try (ResultSet rs = airframeStmt.executeQuery()) {
                if (rs.next()) {
                    airframeId = rs.getInt(1);
                }
            }
        }

        final int resolvedAirframeId = airframeId;
        LOG.fine(() -> "Attempting to insert flight " + flightId + " with airframeId=" + resolvedAirframeId);
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO flights (id, fleet_id, uploader_id, upload_id, airframe_id, system_id, "
                        + "start_time, end_time, filename, md5_hash, number_rows, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, flightId);
            stmt.setInt(2, 1);
            stmt.setInt(3, 1);
            stmt.setInt(4, 999);
            stmt.setInt(5, airframeId);
            stmt.setString(6, "TEST_SYSTEM_" + flightId);
            stmt.setString(7, "2023-01-01 10:00:00");
            stmt.setString(8, "2023-01-01 11:00:00");
            stmt.setString(9, "test_flight_" + flightId + ".csv");
            stmt.setString(10, "test_md5_hash_" + flightId);
            stmt.setInt(11, 200); // number_rows - match our test data size
            stmt.setString(12, "SUCCESS");

            int rowsAffected = stmt.executeUpdate();
            LOG.fine(() -> "Flight " + flightId + " insertion result: rowsAffected=" + rowsAffected);
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert flight " + flightId + " - no rows affected");
            }
        } catch (SQLException e) {
            throw new SQLException("Failed to create flight " + flightId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Creates a test tail record if it doesn't exist
     *
     * @param flightId flight ID for the tail record
     */
    protected void createTestTailIfNotExists(int flightId) throws SQLException {
        String systemId = "TEST_SYSTEM_" + flightId;

        // Check if tail already exists
        try (PreparedStatement checkStmt =
                connection.prepareStatement("SELECT COUNT(*) FROM tails WHERE fleet_id = ? AND system_id = ?")) {
            checkStmt.setInt(1, 1);
            checkStmt.setString(2, systemId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Tail already exists, return
                    return;
                }
            }
        }

        // Insert the test tail
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO tails (fleet_id, system_id, tail, confirmed) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, 1);
            stmt.setString(2, systemId);
            stmt.setString(3, "N" + flightId + "TEST");
            stmt.setBoolean(4, true); // confirmed = true

            stmt.executeUpdate();
        }
    }

    /**
     * Sets up comprehensive time series data for testing
     *
     * @param connection database connection
     * @param flightId flight ID to populate
     */
    protected void setupTimeSeriesData(Connection connection, int flightId) throws SQLException {
        insertSeriesNames(connection);

        insertDataTypeNames(connection);

        insertTimeSeriesData(connection, flightId);
    }

    /**
     * Sets up time series data with columns that should be skipped
     * @param connection the database connection
     * @param flightId the flight ID
     */
    protected void setupTimeSeriesDataWithSkippedColumns(Connection connection, int flightId) throws SQLException {
        insertSeriesNames(connection);

        insertDataTypeNames(connection);

        insertTimeSeriesDataWithSkippedColumns(connection, flightId);
    }

    /**
     * Sets up time series data (should be skipped)
     * @param connection the database connection
     * @param flightId the flight ID
     */
    protected void setupTimeSeriesDataWithSameMinMax(Connection connection, int flightId) throws SQLException {
        insertSeriesNames(connection);

        insertDataTypeNames(connection);

        insertTimeSeriesDataWithSameMinMax(connection, flightId);
    }

    protected void insertSeriesNames(Connection connection) throws SQLException {
        String[] seriesNames = {
            "Altitude",
            "Airspeed",
            "VerticalSpeed",
            "Heading",
            "Bank",
            "Pitch",
            "AirportDistance",
            "RunwayDistance",
            "EngineRPM",
            "FuelFlow",
            "ConstantValue"
        };

        for (String name : seriesNames) {
            try (PreparedStatement stmt =
                    connection.prepareStatement("INSERT INTO double_series_names (name) VALUES (?)")) {
                stmt.setString(1, name);
                try {
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignore duplicate-key errors (row already inserted by a prior test); surface anything else
                    // so a real insert failure is not silently hidden.
                    if (!(e instanceof java.sql.SQLIntegrityConstraintViolationException)) {
                        throw e;
                    }
                }
            }
        }
    }

    protected void insertDataTypeNames(Connection connection) throws SQLException {
        String[] dataTypes = {"double", "float", "int"};

        for (String type : dataTypes) {
            try (PreparedStatement stmt =
                    connection.prepareStatement("INSERT INTO data_type_names (name) VALUES (?)")) {
                stmt.setString(1, type);
                try {
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignore duplicate-key errors (row already inserted by a prior test); surface anything else
                    // so a real insert failure is not silently hidden.
                    if (!(e instanceof java.sql.SQLIntegrityConstraintViolationException)) {
                        throw e;
                    }
                }
            }
        }
    }

    protected void insertTimeSeriesData(Connection connection, int flightId) throws SQLException {
        // Get name and type IDs
        Map<String, Integer> nameIds = getSeriesNameIds(connection);
        Map<String, Integer> typeIds = getDataTypeIds(connection);

        // The writeToFile method skips the first 2 minutes (119 seconds), so we need at least 200 data points
        int dataPoints = 200;

        String[][] testSeries = {
            {"Altitude", "double", generateDataPoints(dataPoints, 1000, 5000)},
            {"Airspeed", "double", generateDataPoints(dataPoints, 120, 160)},
            {"VerticalSpeed", "double", generateDataPoints(dataPoints, 500, 900)},
            {"Heading", "double", generateDataPoints(dataPoints, 90, 110)},
            {"Bank", "double", generateDataPoints(dataPoints, 5, 25)},
            {"Pitch", "double", generateDataPoints(dataPoints, 2, 10)},
            {"EngineRPM", "double", generateDataPoints(dataPoints, 2400, 2800)},
            {"FuelFlow", "double", generateDataPoints(dataPoints, 12, 16)}
        };

        for (String[] series : testSeries) {
            insertTimeSeriesRecord(connection, flightId, series[0], series[1], series[2], nameIds, typeIds);
        }
    }

    protected void insertTimeSeriesDataWithSkippedColumns(Connection connection, int flightId) throws SQLException {
        // Get name and type IDs
        Map<String, Integer> nameIds = getSeriesNameIds(connection);
        Map<String, Integer> typeIds = getDataTypeIds(connection);

        int dataPoints = 200;

        String[][] testSeries = {
            {"Altitude", "double", generateDataPoints(dataPoints, 1000, 5000)},
            {"Airspeed", "double", generateDataPoints(dataPoints, 120, 160)},
            {"AirportDistance", "double", generateDataPoints(dataPoints, 1000, 1000)}, // Should be skipped
            {"RunwayDistance", "double", generateDataPoints(dataPoints, 500, 500)}, // Should be skipped
            {"EngineRPM", "double", generateDataPoints(dataPoints, 2400, 2800)}
        };

        for (String[] series : testSeries) {
            insertTimeSeriesRecord(connection, flightId, series[0], series[1], series[2], nameIds, typeIds);
        }
    }

    protected void insertTimeSeriesDataWithSameMinMax(Connection connection, int flightId) throws SQLException {
        // Get name and type IDs
        Map<String, Integer> nameIds = getSeriesNameIds(connection);
        Map<String, Integer> typeIds = getDataTypeIds(connection);

        int dataPoints = 200;

        String[][] testSeries = {
            {"Altitude", "double", generateDataPoints(dataPoints, 1000, 5000)},
            {"Airspeed", "double", generateDataPoints(dataPoints, 120, 160)},
            {"ConstantValue", "double", generateDataPoints(dataPoints, 100, 100)}, // Same min/max - should be skipped
            {"EngineRPM", "double", generateDataPoints(dataPoints, 2400, 2800)}
        };

        for (String[] series : testSeries) {
            insertTimeSeriesRecord(connection, flightId, series[0], series[1], series[2], nameIds, typeIds);
        }
    }

    /**
     * Generates a comma-separated string of data points for testing
     *
     * @param count number of points to generate
     * @param min minimum value
     * @param max maximum value
     * @return comma-separated data values
     */
    protected String generateDataPoints(int count, double min, double max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            double value = min + (max - min) * i / (count - 1);
            sb.append(value);
        }
        return sb.toString();
    }

    protected void insertTimeSeriesRecord(
            Connection connection,
            int flightId,
            String name,
            String dataType,
            String dataValues,
            Map<String, Integer> nameIds,
            Map<String, Integer> typeIds)
            throws SQLException {
        Integer nameId = nameIds.get(name);
        Integer typeId = typeIds.get(dataType);

        if (nameId != null && typeId != null) {
            // Parse data values
            String[] values = dataValues.split(",");
            double[] data = new double[values.length];
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double sum = 0;

            for (int i = 0; i < values.length; i++) {
                data[i] = Double.parseDouble(values[i]);
                min = Math.min(min, data[i]);
                max = Math.max(max, data[i]);
                sum += data[i];
            }

            double avg = sum / data.length;

            // Insert into double_series table
            try (PreparedStatement stmt =
                    connection.prepareStatement("INSERT INTO double_series (flight_id, name_id, data_type_id, length, "
                            + "valid_length, min, avg, max, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, flightId);
                stmt.setInt(2, nameId);
                stmt.setInt(3, typeId);
                stmt.setInt(4, data.length);
                stmt.setInt(5, data.length);
                stmt.setDouble(6, min);
                stmt.setDouble(7, avg);
                stmt.setDouble(8, max);
                stmt.setBytes(9, serializeDoubleArray(data));

                try {
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    // Ignore duplicate-key errors (row already inserted by a prior test); surface anything else
                    // so a real insert failure is not silently hidden.
                    if (!(e instanceof java.sql.SQLIntegrityConstraintViolationException)) {
                        throw e;
                    }
                }
            }
        }
    }

    protected Map<String, Integer> getSeriesNameIds(Connection connection) throws SQLException {
        // Case-insensitive so lookups match the DB's case-insensitive collation (e.g. a "double" lookup
        // finds a "DOUBLE" row inserted by production code), avoiding missed inserts.
        Map<String, Integer> nameIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id, name FROM double_series_names");
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                nameIds.put(rs.getString("name"), rs.getInt("id"));
            }
        }
        return nameIds;
    }

    protected Map<String, Integer> getDataTypeIds(Connection connection) throws SQLException {
        // Case-insensitive so a "double" lookup finds a "DOUBLE" row (production code inserts the uppercase
        // form, and MySQL's unique key on name is case-insensitive).
        Map<String, Integer> typeIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id, name FROM data_type_names");
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                typeIds.put(rs.getString("name"), rs.getInt("id"));
            }
        }
        return typeIds;
    }

    protected byte[] serializeDoubleArray(double[] data) {
        try {
            return org.ngafid.core.util.Compression.compressDoubleArray(data);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to compress double array", e);
        }
    }

    protected int getTestUploadId() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT id FROM uploads WHERE id = 999")) {
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 999; // Fallback
    }

    protected void setupStringTimeSeriesData(Connection connection, int flightId) throws SQLException {
        // Insert series name and get the generated ID
        int nameId;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO string_series_names (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "TestStringSeries_" + flightId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    nameId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated key for string_series_names");
                }
            }
        } catch (SQLException e) {
            // If it already exists, get the existing ID
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException
                    || e.getMessage().contains("Unique index or primary key violation") // H2
                    || e.getMessage().contains("Duplicate entry")) { // MySQL
                try (PreparedStatement stmt =
                        connection.prepareStatement("SELECT id FROM string_series_names WHERE name = ?")) {
                    stmt.setString(1, "TestStringSeries_" + flightId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            nameId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to find existing string_series_names record");
                        }
                    }
                }
            } else {
                throw e;
            }
        }

        // Insert data type name and get the generated ID
        int typeId;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO data_type_names (name) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "String");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    typeId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated key for data_type_names");
                }
            }
        } catch (SQLException e) {
            // If it already exists, get the existing ID
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException
                    || e.getMessage().contains("Unique index or primary key violation") // H2
                    || e.getMessage().contains("Duplicate entry")) { // MySQL
                try (PreparedStatement stmt =
                        connection.prepareStatement("SELECT id FROM data_type_names WHERE name = ?")) {
                    stmt.setString(1, "String");
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            typeId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to find existing data_type_names record");
                        }
                    }
                }
            } else {
                throw e;
            }
        }

        // Insert string series data
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO string_series (flight_id, name_id, data_type_id, length, valid_length, "
                        + "data) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, flightId);
            stmt.setInt(2, nameId);
            stmt.setInt(3, typeId);
            stmt.setInt(4, 3);
            stmt.setInt(5, 3);

            // Serialize string array
            String[] testData = {"value1", "value2", "value3"};
            byte[] serializedData = serializeStringArray(testData);
            stmt.setBytes(6, serializedData);
            stmt.executeUpdate();
        }
    }

    // Helper method to get tag ID by name
    protected int getTagIdByName(Connection connection, String tagName) throws SQLException {
        try (PreparedStatement stmt =
                connection.prepareStatement("SELECT id FROM flight_tags WHERE name = ? AND fleet_id = ?")) {
            stmt.setString(1, tagName);
            stmt.setInt(2, 1);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Tag not found: " + tagName);
                }
            }
        }
    }

    // Helper method to serialize string array
    protected byte[] serializeStringArray(String[] data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(data);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize string array", e);
        }
    }

    protected void setupTimeSeriesDataWithSpecialNames(Connection connection, int flightId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO double_series_names (name) VALUES (?) ON DUPLICATE KEY UPDATE name = name")) {
            stmt.setString(1, "Test_Series-1");
            stmt.executeUpdate();
        }

        insertTimeSeriesDataWithSpecialNames(connection, flightId);
    }

    protected void insertTimeSeriesDataWithSpecialNames(Connection connection, int flightId) throws SQLException {
        Map<String, Integer> nameIds = getSeriesNameIds(connection);
        Map<String, Integer> typeIds = getDataTypeIds(connection);

        // Insert time series record with special name
        insertTimeSeriesRecord(
                connection, flightId, "Test_Series-1", "double", generateDataPoints(10, 0.0, 100.0), nameIds, typeIds);
    }

    protected void setupTimeSeriesDataWithLongName(Connection connection, int flightId, String longName)
            throws SQLException {
        // Insert series name with long name
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO double_series_names (name) VALUES (?) ON DUPLICATE KEY UPDATE name = name")) {
            stmt.setString(1, longName);
            stmt.executeUpdate();
        }

        insertTimeSeriesDataWithLongName(connection, flightId, longName);
    }

    protected void insertTimeSeriesDataWithLongName(Connection connection, int flightId, String longName)
            throws SQLException {
        Map<String, Integer> nameIds = getSeriesNameIds(connection);
        Map<String, Integer> typeIds = getDataTypeIds(connection);

        // Insert time series record with long name
        insertTimeSeriesRecord(
                connection, flightId, longName, "double", generateDataPoints(10, 0.0, 100.0), nameIds, typeIds);
    }

    protected Filter createFilterWithDoubleParameter() {
        return new DoubleParameterFilter();
    }

    protected Filter createFilterWithIntegerParameter() {
        return new IntegerParameterFilter();
    }

    protected Filter createFilterWithMixedParameters() {
        return new MixedParameterFilter();
    }

    protected void setupTestDataForSorting(Connection connection, int fleetId) throws SQLException {
        // Use unique flight IDs to avoid conflicts
        int flightId1 = 9001 + (int) (System.currentTimeMillis() % 1000);
        int flightId2 = flightId1 + 1;
        int flightId3 = flightId1 + 2;

        // Create test flights first
        try {
            createTestFlight(flightId1);
        } catch (Exception e) {
            throw new SQLException("Failed to create flight " + flightId1 + ": " + e.getMessage(), e);
        }
        try {
            createTestFlight(flightId2);
        } catch (Exception e) {
            throw new SQLException("Failed to create flight " + flightId2 + ": " + e.getMessage(), e);
        }
        try {
            createTestFlight(flightId3);
        } catch (Exception e) {
            throw new SQLException("Failed to create flight " + flightId3 + ": " + e.getMessage(), e);
        }

        // Verify flights were created
        try (PreparedStatement checkStmt =
                connection.prepareStatement("SELECT COUNT(*) FROM flights WHERE id IN (?, ?, ?)")) {
            checkStmt.setInt(1, flightId1);
            checkStmt.setInt(2, flightId2);
            checkStmt.setInt(3, flightId3);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count != 3) {
                        // Debug: Check what flights actually exist
                        try (PreparedStatement debugStmt = connection.prepareStatement(
                                "SELECT id FROM flights WHERE id IN (?, ?, ?) ORDER BY id")) {
                            debugStmt.setInt(1, flightId1);
                            debugStmt.setInt(2, flightId2);
                            debugStmt.setInt(3, flightId3);
                            try (ResultSet debugRs = debugStmt.executeQuery()) {
                                StringBuilder existing = new StringBuilder("Existing flights: ");
                                while (debugRs.next()) {
                                    existing.append(debugRs.getInt(1)).append(", ");
                                }
                                throw new SQLException("Failed to create test flights for sorting. Expected 3, got "
                                        + count + ". " + existing);
                            }
                        }
                    }
                }
            }
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO itinerary (flight_id, airport, runway, `order`, min_altitude_index, "
                        + "start_of_approach, end_of_approach, start_of_takeoff, end_of_takeoff) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE airport = airport")) {
            stmt.setInt(1, flightId1);
            stmt.setString(2, "KJFK");
            stmt.setString(3, "04L");
            stmt.setInt(4, 1);
            stmt.setInt(5, 100);
            stmt.setInt(6, 200);
            stmt.setInt(7, 300);
            stmt.setInt(8, 50);
            stmt.setInt(9, 80);
            stmt.executeUpdate();

            stmt.setInt(1, flightId1);
            stmt.setString(2, "KLAX");
            stmt.setString(3, "25R");
            stmt.setInt(4, 2);
            stmt.setInt(5, 150);
            stmt.setInt(6, 250);
            stmt.setInt(7, 350);
            stmt.setInt(8, 75);
            stmt.setInt(9, 100);
            stmt.executeUpdate();

            stmt.setInt(1, flightId2);
            stmt.setString(2, "KORD");
            stmt.setString(3, "10C");
            stmt.setInt(4, 1);
            stmt.setInt(5, 120);
            stmt.setInt(6, 220);
            stmt.setInt(7, 320);
            stmt.setInt(8, 60);
            stmt.setInt(9, 90);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO events (fleet_id, flight_id, event_definition_id, start_time, end_time, "
                        + "severity, other_flight_id) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY "
                        + "UPDATE flight_id = flight_id")) {
            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId1);
            stmt.setInt(3, 1);
            stmt.setString(4, "2023-01-01 10:00:00");
            stmt.setString(5, "2023-01-01 10:05:00");
            stmt.setDouble(6, 0.5);
            stmt.setNull(7, java.sql.Types.INTEGER);
            stmt.executeUpdate();

            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId1);
            stmt.setInt(3, 2);
            stmt.setString(4, "2023-01-01 10:10:00");
            stmt.setString(5, "2023-01-01 10:15:00");
            stmt.setDouble(6, 0.7);
            stmt.setNull(7, java.sql.Types.INTEGER);
            stmt.executeUpdate();

            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId2);
            stmt.setInt(3, 1);
            stmt.setString(4, "2023-01-01 11:00:00");
            stmt.setString(5, "2023-01-01 11:05:00");
            stmt.setDouble(6, 0.3);
            stmt.setNull(7, java.sql.Types.INTEGER);
            stmt.executeUpdate();
        }
        // Create flight tags first and get their IDs
        String uniqueTag1 = "TestTag1_" + System.currentTimeMillis();
        String uniqueTag2 = "TestTag2_" + System.currentTimeMillis();
        int tagId1;
        int tagId2;
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO flight_tags (fleet_id, name, description, color) "
                        + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = name")) {
            stmt.setInt(1, fleetId);
            stmt.setString(2, uniqueTag1);
            stmt.setString(3, "Test Description 1");
            stmt.setString(4, "#FF0000");
            stmt.executeUpdate();

            stmt.setInt(1, fleetId);
            stmt.setString(2, uniqueTag2);
            stmt.setString(3, "Test Description 2");
            stmt.setString(4, "#00FF00");
            stmt.executeUpdate();
        }

        // Get the tag IDs
        try (PreparedStatement stmt =
                connection.prepareStatement("SELECT id FROM flight_tags WHERE fleet_id = ? AND name = ?")) {
            stmt.setInt(1, fleetId);
            stmt.setString(2, uniqueTag1);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tagId1 = rs.getInt(1);
                } else {
                    tagId1 = 1; // fallback
                }
            }

            stmt.setString(2, uniqueTag2);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tagId2 = rs.getInt(1);
                } else {
                    tagId2 = 2; // fallback
                }
            }
        }

        // Now insert the flight_tag_map entries with the correct tag IDs
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO flight_tag_map (flight_id, tag_id) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE flight_id = flight_id")) {
            stmt.setInt(1, flightId1);
            stmt.setInt(2, tagId1);
            stmt.executeUpdate();

            stmt.setInt(1, flightId1);
            stmt.setInt(2, tagId2);
            stmt.executeUpdate();

            stmt.setInt(1, flightId2);
            stmt.setInt(2, tagId1);
            stmt.executeUpdate();
        }
    }

    private static class DoubleParameterFilter extends Filter {
        DoubleParameterFilter() {
            super(new ArrayList<>());
        }

        @Override
        public String toQueryString(int fleetId, ArrayList<Object> parameters) {
            parameters.add(100.5);
            return "flights.id > ?";
        }
    }

    private static class IntegerParameterFilter extends Filter {
        IntegerParameterFilter() {
            super(new ArrayList<>());
        }

        @Override
        public String toQueryString(int fleetId, ArrayList<Object> parameters) {
            parameters.add(100);
            return "flights.id > ?";
        }
    }

    private static class MixedParameterFilter extends Filter {
        MixedParameterFilter() {
            super("AND");
        }

        @Override
        public String toQueryString(int fleetId, ArrayList<Object> parameters) {
            parameters.add(50.0);
            parameters.add(200);
            return "flights.id > ? AND flights.id < ?";
        }
    }
}
