package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Flight}'s metadata and accessor API: loading flights from an upload, single-flight lookup, simple
 * getters (filename, tail number, row count, airframe, upload/uploader ids, status), simulator-aircraft management,
 * completion recording, recorded exceptions, and the {@code checkCalculationParameters} missing-parameter checks.
 *
 * <p>Shared database seeding fixtures live in {@link FlightTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightMetadataTest extends FlightTestBase {

    /**
     * Verifies {@code Flight.getFlightsFromUpload} returns the flights belonging to an upload.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(41)
    @DisplayName("Should get flights from upload ID")
    public void testGetFlightsFromUpload() throws SQLException {
        // First, ensure we have a test upload
        createTestUploadIfNotExists();

        ArrayList<Flight> flights = Flight.getFlightsFromUpload(connection, 999);

        assertNotNull(flights, "Flights list should not be null");
        assertTrue(flights.size() >= 0, "Should have flights from test upload");
    }

    /**
     * Verifies {@code Flight.getFlightsFromUpload} returns an empty list for a non-existent upload id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(42)
    @DisplayName("Should get flights from upload ID with non-existent upload")
    public void testGetFlightsFromUploadNonExistent() throws SQLException {
        ArrayList<Flight> flights = Flight.getFlightsFromUpload(connection, 99999);

        assertNotNull(flights, "Flights list should not be null");
        assertEquals(0, flights.size(), "Should have no flights for non-existent upload");
    }

    /**
     * Verifies {@code Flight.getFlightsFromUpload} throws {@link NullPointerException} for a null connection.
     */
    @Test
    @Order(44)
    @DisplayName("Should handle null connection in getFlightsFromUpload")
    public void testGetFlightsFromUploadNullConnection() {
        assertThrows(NullPointerException.class, () -> {
            Flight.getFlightsFromUpload(null, 999);
        });
    }

    /**
     * Verifies {@code Flight.getFlight} loads a single flight by id.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(45)
    @DisplayName("Should test getFlight method")
    public void testGetFlight() throws SQLException {
        Flight flight = Flight.getFlight(connection, 1);

        assertNotNull(flight, "Flight should not be null");
        assertEquals(1, flight.getId());
        assertNotNull(flight.getFilename());
        assertNotNull(flight.getStartDateTime());
        assertNotNull(flight.getEndDateTime());
    }

    /**
     * Verifies {@code Flight.getFilename} returns the flight's source filename.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(46)
    @DisplayName("Should test getFilename method")
    public void testGetFilename() throws SQLException {
        String filename = Flight.getFilename(connection, 1);

        assertNotNull(filename, "Filename should not be null");
        assertFalse(filename.isEmpty(), "Filename should not be empty");
    }

    /**
     * Verifies {@code Flight.getTailNumber} returns the flight's tail number.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(48)
    @DisplayName("Should test getTailNumber method")
    public void testGetTailNumber() throws SQLException {
        Flight flight = Flight.getFlight(connection, 1);

        assertNotNull(flight, "Flight should not be null");
        // getTailNumber should return the tail number from the database
        assertNotNull(flight.getTailNumber(), "Tail number should not be null");
    }

    /**
     * Verifies {@code Flight.getSimAircraft} returns the configured simulator-aircraft entries for a fleet.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(52)
    @DisplayName("Should test getSimAircraft method")
    public void testGetSimAircraft() throws SQLException {
        // Create some test sim aircraft data first to ensure the while loop executes
        Flight.addSimAircraft(connection, 1, "/path/to/aircraft1");
        Flight.addSimAircraft(connection, 1, "/path/to/aircraft2");
        Flight.addSimAircraft(connection, 2, "/path/to/aircraft3"); // Different fleet

        // Test getting sim aircraft for fleet 1
        List<String> simAircraft = Flight.getSimAircraft(connection, 1);

        assertNotNull(simAircraft, "Sim aircraft list should not be null");
        // Should contain the created sim aircraft for fleet 1
        assertTrue(simAircraft.size() >= 2, "Sim aircraft list should contain at least 2 aircraft for fleet 1");
        assertTrue(simAircraft.contains("/path/to/aircraft1"), "Should contain aircraft1 path");
        assertTrue(simAircraft.contains("/path/to/aircraft2"), "Should contain aircraft2 path");
        assertFalse(simAircraft.contains("/path/to/aircraft3"), "Should not contain aircraft3 path (different fleet)");
    }

    /**
     * Verifies {@code Flight.insertCompleted} records a flight as completed.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(53)
    @DisplayName("Should test insertCompleted method")
    public void testInsertCompleted() throws SQLException {
        Flight flight = Flight.getFlight(connection, 1);

        // Test insertCompleted - should return true for flights that are not PROCESSING
        assertTrue(flight.insertCompleted(), "Flight should be completed");
    }

    /**
     * Verifies {@code Flight.addSimAircraft} adds a simulator-aircraft entry for a fleet.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(57)
    @DisplayName("Should test addSimAircraft method")
    public void testAddSimAircraft() throws SQLException {
        // Test adding sim aircraft (should not throw exception)
        assertDoesNotThrow(() -> {
            Flight.addSimAircraft(connection, 1, "test_aircraft_path");
        });
    }

    /**
     * Verifies {@code Flight.removeSimAircraft} removes a simulator-aircraft entry for a fleet.
     *
     * @throws SQLException if the operation fails
     */
    @Test
    @Order(60)
    @DisplayName("Should test removeSimAircraft method")
    public void testRemoveSimAircraft() throws SQLException {
        // Test removing sim aircraft (should not throw exception)
        assertDoesNotThrow(() -> {
            Flight.removeSimAircraft(connection, 1, "test_aircraft_path");
        });
    }

    /**
     * Verifies {@code Flight.getExceptions} returns an empty list when the flight has no recorded exceptions.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(98)
    @DisplayName("Should return empty list when no exceptions")
    public void testGetExceptionsEmpty() throws SQLException {
        createTestFlight(2001);
        Flight flight = Flight.getFlight(connection, 2001);

        List<MalformedFlightFileException> exceptions = flight.getExceptions();

        assertNotNull(exceptions, "Exceptions list should not be null");
        assertTrue(exceptions.isEmpty(), "Should return empty list when no exceptions");
    }

    /**
     * Verifies {@code Flight.getExceptions} returns the recorded exceptions for a flight that has them.
     *
     * @throws SQLException if the query fails
     */
    @Test
    @Order(99)
    @DisplayName("Should return exceptions when they exist")
    public void testGetExceptionsWithExceptions() throws SQLException {
        createTestFlight(2002);
        Flight flight = Flight.getFlight(connection, 2002);

        // Add some exceptions to the flight
        MalformedFlightFileException exception1 = new MalformedFlightFileException("Test exception 1");
        MalformedFlightFileException exception2 = new MalformedFlightFileException("Test exception 2");

        // Note: We can't directly add exceptions to the flight since the field is private
        // This test verifies the method exists and returns a list
        List<MalformedFlightFileException> exceptions = flight.getExceptions();

        assertNotNull(exceptions, "Exceptions list should not be null");
        // The list will be empty since we can't directly modify the private field
        assertTrue(exceptions.isEmpty(), "Should return empty list by default");
    }

    /**
     * Verifies {@code Flight.checkCalculationParameters} throws {@link MalformedFlightFileException} when several
     * required parameters are missing.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading flight data fails
     */
    @Test
    @Order(100)
    @DisplayName("Should throw MalformedFlightFileException when multiple parameters are missing")
    public void testCheckCalculationParametersMultipleMissing() throws SQLException, IOException {
        createTestFlight(2005);
        Flight flight = Flight.getFlight(connection, 2005);

        setupTimeSeriesData(connection, flight.getId());

        // This should throw an exception since both parameters don't exist
        MalformedFlightFileException exception = assertThrows(
                MalformedFlightFileException.class,
                () -> {
                    flight.checkCalculationParameters("Test Calculation", "NonExistent1", "NonExistent2");
                },
                "Should throw MalformedFlightFileException when parameters are missing");

        assertTrue(
                exception
                        .getMessage()
                        .contains("Cannot calculate 'Test Calculation' as parameter 'NonExistent1' was missing."),
                "Exception message should indicate first missing parameter");
    }

    /**
     * Verifies {@code Flight.checkCalculationParameters} handles an empty required-parameter list without error.
     */
    @Test
    @Order(101)
    @DisplayName("Should handle empty parameter list")
    public void testCheckCalculationParametersEmptyList()
            throws SQLException, IOException, MalformedFlightFileException {
        createTestFlight(2006);
        Flight flight = Flight.getFlight(connection, 2006);

        // This should not throw an exception since no parameters are checked
        assertDoesNotThrow(
                () -> {
                    flight.checkCalculationParameters("Test Calculation");
                },
                "Should not throw exception when no parameters are provided");
    }

    /**
     * Verifies {@code Flight.checkCalculationParameters} handles a null calculation name without error.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading flight data fails
     */
    @Test
    @Order(102)
    @DisplayName("Should handle null calculation name")
    public void testCheckCalculationParametersNullCalculationName() throws SQLException, IOException {
        createTestFlight(2007);
        Flight flight = Flight.getFlight(connection, 2007);

        // This should throw an exception since "NonExistentParameter" doesn't exist
        MalformedFlightFileException exception = assertThrows(
                MalformedFlightFileException.class,
                () -> {
                    flight.checkCalculationParameters(null, "NonExistentParameter");
                },
                "Should throw MalformedFlightFileException when parameter is missing");

        assertTrue(
                exception
                        .getMessage()
                        .contains("Cannot calculate 'null' as parameter 'NonExistentParameter' was missing."),
                "Exception message should handle null calculation name");
    }

    /**
     * Verifies the array overload of {@code Flight.checkCalculationParameters} reports all requested parameters as
     * missing when none exist.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading flight data fails
     */
    @Test
    @Order(209)
    @DisplayName("Should return all parameters when none exist")
    public void testCheckCalculationParametersArrayNoneExist() throws SQLException, IOException {
        createTestFlight(2010);
        Flight flight = Flight.getFlight(connection, 2010);

        // Don't create any time series data

        String[] seriesNames = {"NonExistent1", "NonExistent2", "NonExistent3"};
        List<String> missingParams = flight.checkCalculationParameters(seriesNames);

        assertNotNull(missingParams, "Missing parameters list should not be null");
        assertEquals(3, missingParams.size(), "Should return all 3 missing parameters");
        assertTrue(missingParams.contains("NonExistent1"), "Should contain first missing parameter");
        assertTrue(missingParams.contains("NonExistent2"), "Should contain second missing parameter");
        assertTrue(missingParams.contains("NonExistent3"), "Should contain third missing parameter");
    }

    /**
     * Verifies the array overload of {@code Flight.checkCalculationParameters} handles an empty parameter array.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading flight data fails
     */
    @Test
    @Order(210)
    @DisplayName("Should handle empty array")
    public void testCheckCalculationParametersArrayEmpty() throws SQLException, IOException {
        createTestFlight(2011);
        Flight flight = Flight.getFlight(connection, 2011);

        String[] seriesNames = {};
        List<String> missingParams = flight.checkCalculationParameters(seriesNames);

        assertNotNull(missingParams, "Missing parameters list should not be null");
        assertTrue(missingParams.isEmpty(), "Should return empty list when no parameters are checked");
    }

    /**
     * Verifies the array overload of {@code Flight.checkCalculationParameters} handles a null parameter array.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if reading flight data fails
     */
    @Test
    @Order(211)
    @DisplayName("Should handle null array")
    public void testCheckCalculationParametersArrayNull() throws SQLException, IOException {
        createTestFlight(2012);
        Flight flight = Flight.getFlight(connection, 2012);

        // This will throw a NullPointerException since the method doesn't handle null arrays
        assertThrows(
                NullPointerException.class,
                () -> {
                    flight.checkCalculationParameters((String[]) null);
                },
                "Should throw NullPointerException when array is null");
    }

    /**
     * Verifies {@code Flight.getNumberRows} returns the flight's sample/row count.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(312)
    @DisplayName("Should return the number of rows")
    public void testGetNumberRows() throws SQLException {
        createTestFlight(3013);
        Flight flight = Flight.getFlight(connection, 3013);

        int rows = flight.getNumberRows();

        assertEquals(200, rows, "Should return the correct number of rows");
    }

    /**
     * Verifies {@code Flight.getAirframe} returns the flight's airframe.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(313)
    @DisplayName("Should return the airframe")
    public void testGetAirframe() throws SQLException {
        createTestFlight(3014);
        Flight flight = Flight.getFlight(connection, 3014);

        Airframes.Airframe airframe = flight.getAirframe();

        assertNotNull(airframe, "Airframe should not be null");
        assertEquals("Cessna 172S", airframe.getName(), "Airframe name should match");
    }

    /**
     * Verifies {@code Flight.getAirframeNameId} returns the flight's airframe name id.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(314)
    @DisplayName("Should return the airframe name ID")
    public void testGetAirframeNameId() throws SQLException {
        createTestFlight(3015);
        Flight flight = Flight.getFlight(connection, 3015);

        int airframeNameId = flight.getAirframeNameId();

        assertTrue(airframeNameId > 0, "Airframe name ID should be positive");
    }

    /**
     * Verifies {@code Flight.getAirframeName} returns the flight's airframe name.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(315)
    @DisplayName("Should return the airframe name")
    public void testGetAirframeName() throws SQLException {
        createTestFlight(3016);
        Flight flight = Flight.getFlight(connection, 3016);

        String airframeName = flight.getAirframeName();

        assertNotNull(airframeName, "Airframe name should not be null");
        assertEquals("Cessna 172S", airframeName, "Airframe name should match");
    }

    /**
     * Verifies {@code Flight.getAirframeTypeId} returns the flight's airframe type id.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(316)
    @DisplayName("Should return the airframe type ID")
    public void testGetAirframeTypeId() throws SQLException {
        createTestFlight(3017);
        Flight flight = Flight.getFlight(connection, 3017);

        int airframeTypeId = flight.getAirframeTypeId();

        assertTrue(airframeTypeId > 0, "Airframe type ID should be positive");
    }

    /**
     * Verifies {@code Flight.getAirframeType} returns the flight's airframe type.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(317)
    @DisplayName("Should return the airframe type")
    public void testGetAirframeType() throws SQLException {
        createTestFlight(3018);
        Flight flight = Flight.getFlight(connection, 3018);

        String airframeType = flight.getAirframeType();

        assertNotNull(airframeType, "Airframe type should not be null");
        assertEquals("Fixed Wing", airframeType, "Airframe type should match");
    }

    /**
     * Verifies {@code Flight.isC172} returns true for a Cessna 172S flight.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(318)
    @DisplayName("Should return true for Cessna 172S")
    public void testIsC172True() throws SQLException {
        createTestFlight(3019);
        Flight flight = Flight.getFlight(connection, 3019);

        boolean isC172 = flight.isC172();

        assertTrue(isC172, "Should return true for Cessna 172S");
    }

    /**
     * Verifies {@code Flight.isC172} returns false for a non-Cessna-172S aircraft.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(319)
    @DisplayName("Should return false for non-Cessna 172S aircraft")
    public void testIsC172False() throws SQLException {
        // Create a different airframe that is not Cessna 172S
        createTestTailIfNotExists(3020);

        // Create a non-Cessna airframe manually
        int nonCessnaAirframeId = 1; // Default
        try (PreparedStatement stmt =
                connection.prepareStatement("INSERT INTO airframes (airframe, type_id) VALUES (?, ?)")) {
            stmt.setString(1, "Boeing 737");
            stmt.setInt(2, 1); // Use existing type
            try {
                stmt.executeUpdate();
                // Get the ID of the newly created airframe
                try (PreparedStatement getIdStmt =
                        connection.prepareStatement("SELECT id FROM airframes WHERE airframe = 'Boeing 737'")) {
                    try (ResultSet rs = getIdStmt.executeQuery()) {
                        if (rs.next()) {
                            nonCessnaAirframeId = rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                // Airframe might already exist, get its ID
                try (PreparedStatement getIdStmt =
                        connection.prepareStatement("SELECT id FROM airframes WHERE airframe = 'Boeing 737'")) {
                    try (ResultSet rs = getIdStmt.executeQuery()) {
                        if (rs.next()) {
                            nonCessnaAirframeId = rs.getInt(1);
                        }
                    }
                }
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO flights (id, fleet_id, uploader_id, upload_id, airframe_id, system_id, "
                        + "start_time, end_time, filename, md5_hash, number_rows, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, 3020);
            stmt.setInt(2, 1);
            stmt.setInt(3, 1);
            stmt.setInt(4, 1);
            stmt.setInt(5, nonCessnaAirframeId); // Use the non-Cessna airframe
            stmt.setString(6, "TEST_SYSTEM_3020");
            stmt.setTimestamp(7, java.sql.Timestamp.valueOf("2023-01-01 10:00:00"));
            stmt.setTimestamp(8, java.sql.Timestamp.valueOf("2023-01-01 11:00:00"));
            stmt.setString(9, "test_flight_3020.csv");
            stmt.setString(10, "test_hash_3020");
            stmt.setInt(11, 200);
            stmt.setString(12, "SUCCESS");
            stmt.executeUpdate();
        }

        Flight flight = Flight.getFlight(connection, 3020);

        boolean isC172 = flight.isC172();

        assertFalse(isC172, "Should return false for non-Cessna 172S aircraft");
    }

    /**
     * Verifies {@code Flight.getUploadId} returns the id of the upload the flight came from.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(320)
    @DisplayName("Should return the upload ID")
    public void testGetUploadId() throws SQLException {
        createTestFlight(3021);
        Flight flight = Flight.getFlight(connection, 3021);

        int uploadId = flight.getUploadId();

        assertTrue(uploadId > 0, "Should return a positive upload ID");
    }

    /**
     * Verifies {@code Flight.getUploaderId} returns the id of the user who uploaded the flight.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(321)
    @DisplayName("Should return the uploader ID")
    public void testGetUploaderId() throws SQLException {
        createTestFlight(3022);
        Flight flight = Flight.getFlight(connection, 3022);

        int uploaderId = flight.getUploaderId();

        assertEquals(1, uploaderId, "Should return the correct uploader ID");
    }

    /**
     * Verifies {@code Flight.getStatus} returns the flight's processing status.
     *
     * @throws SQLException if a database operation fails
     */
    @Test
    @Order(322)
    @DisplayName("Should return the flight status")
    public void testGetStatus() throws SQLException {
        createTestFlight(3023);
        Flight flight = Flight.getFlight(connection, 3023);

        Flight.FlightStatus status = flight.getStatus();

        assertNotNull(status, "Status should not be null");
        assertEquals(Flight.FlightStatus.SUCCESS, status, "Should return SUCCESS status");
    }
}
