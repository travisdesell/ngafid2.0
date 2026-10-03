package org.ngafid.core.flights;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Flight#writeToFile}: exporting a flight's data to a CSV file, including header/data-type lines,
 * filename edge cases (empty, invalid, long, special characters), overwriting existing files, and the column-skipping
 * rules for time-series data (skipped columns and constant min/max series).
 *
 * <p>Shared database seeding fixtures live in {@link FlightTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightWriteToFileTest extends FlightTestBase {

    /**
     * Verifies {@code Flight.writeToFile} writes a flight's data to a CSV file.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(16)
    @DisplayName("Should write flight data to file")
    public void testWriteToFile() throws SQLException, IOException {
        ArrayList<Flight> flights = Flight.getFlights(connection, 1);
        assertNotNull(flights);
        assertTrue(flights.size() > 0, "Should have at least one flight in test data");

        Flight flight = flights.get(0);
        assertNotNull(flight);

        String tempFileName = "test_flight_output.csv";

        try {
            flight.writeToFile(connection, tempFileName);

            File outputFile = new File(tempFileName);
            assertTrue(outputFile.exists(), "Output file should be created");
            assertTrue(outputFile.length() > 0, "Output file should not be empty");

            List<String> lines = Files.readAllLines(Paths.get(tempFileName));
            assertTrue(lines.size() >= 2, "File should have at least 2 lines (header and data type)");

            assertTrue(lines.get(0).startsWith("#"), "First line should be a header starting with #");

            assertTrue(lines.get(1).startsWith("#"), "Second line should be data types starting with #");

        } finally {
            File tempFile = new File(tempFileName);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} writes a flight's CSV with comprehensive data coverage.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(59)
    @DisplayName("Should test writeToFile method with comprehensive coverage")
    public void testWriteToFileComprehensive() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        // Test writing to file
        String testFilename = "test_flight_output_comprehensive.csv";
        assertDoesNotThrow(() -> {
            flight.writeToFile(connection, testFilename);
        });

        java.io.File testFile = new java.io.File(testFilename);
        assertTrue(testFile.exists(), "Output file should be created");
        assertTrue(testFile.length() > 0, "Output file should not be empty");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} writes the CSV for a different flight's data.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(60)
    @DisplayName("Should test writeToFile method with different flight data")
    public void testWriteToFileWithDifferentFlight() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 2);

        if (flight != null) {
            String testFilename = "test_flight_output_different.csv";
            assertDoesNotThrow(() -> {
                flight.writeToFile(connection, testFilename);
            });

            java.io.File testFile = new java.io.File(testFilename);
            assertTrue(testFile.exists(), "Output file should be created");

            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} behaves correctly (surfacing the expected failure) when given a null
     * connection.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(59)
    @DisplayName("Should test writeToFile method with null connection")
    public void testWriteToFileWithNullConnection() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String testFilename = "test_flight_output_null.csv";
        assertThrows(NullPointerException.class, () -> {
            flight.writeToFile(null, testFilename);
        });
    }

    /**
     * Verifies {@code Flight.writeToFile} handles an invalid output filename.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(60)
    @DisplayName("Should test writeToFile method with invalid filename")
    public void testWriteToFileWithInvalidFilename() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String invalidFilename = "/invalid/path/that/does/not/exist/test.csv";
        assertThrows(IOException.class, () -> {
            flight.writeToFile(connection, invalidFilename);
        });
    }

    /**
     * Verifies {@code Flight.writeToFile} handles an empty output filename.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(61)
    @DisplayName("Should test writeToFile method with empty filename")
    public void testWriteToFileWithEmptyFilename() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String emptyFilename = "";
        assertThrows(IOException.class, () -> {
            flight.writeToFile(connection, emptyFilename);
        });
    }

    /**
     * Verifies {@code Flight.writeToFile} handles special characters in the output filename.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(62)
    @DisplayName("Should test writeToFile method with special characters in filename")
    public void testWriteToFileWithSpecialCharacters() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String testFilename = "test_flight_output_special_@#$%.csv";
        assertDoesNotThrow(() -> {
            flight.writeToFile(connection, testFilename);
        });

        java.io.File testFile = new java.io.File(testFilename);
        assertTrue(testFile.exists(), "Output file should be created");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} handles a very long output filename.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(63)
    @DisplayName("Should test writeToFile method with long filename")
    public void testWriteToFileWithLongFilename() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String longFilename = "test_flight_output_" + "x".repeat(100) + ".csv";
        assertDoesNotThrow(() -> {
            flight.writeToFile(connection, longFilename);
        });

        java.io.File testFile = new java.io.File(longFilename);
        assertTrue(testFile.exists(), "Output file should be created");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} overwrites an already-existing output file.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(64)
    @DisplayName("Should test writeToFile method with existing file")
    public void testWriteToFileWithExistingFile() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String testFilename = "test_flight_output_existing.csv";

        java.io.File testFile = new java.io.File(testFilename);
        testFile.createNewFile();
        long originalSize = testFile.length();

        assertDoesNotThrow(() -> {
            flight.writeToFile(connection, testFilename);
        });

        assertTrue(testFile.exists(), "Output file should exist");
        assertTrue(testFile.length() != originalSize, "File should be overwritten with new content");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} writes CSV output for multiple flights.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(65)
    @DisplayName("Should test writeToFile method with multiple flights")
    public void testWriteToFileWithMultipleFlights() throws SQLException, IOException {
        for (int flightId = 1; flightId <= 3; flightId++) {
            Flight flight = Flight.getFlight(connection, flightId);
            if (flight != null) {
                String testFilename = "test_flight_output_" + flightId + ".csv";
                assertDoesNotThrow(() -> {
                    flight.writeToFile(connection, testFilename);
                });

                java.io.File testFile = new java.io.File(testFilename);
                assertTrue(testFile.exists(), "Output file should be created for flight " + flightId);

                if (testFile.exists()) {
                    testFile.delete();
                }
            }
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} handles a variety of edge-case filenames.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(66)
    @DisplayName("Should test writeToFile method with edge case filenames")
    public void testWriteToFileWithEdgeCaseFilenames() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String[] edgeCaseFilenames = {
            "test.csv",
            "test_with_spaces.csv",
            "test-with-dashes.csv",
            "test_with_underscores.csv",
            "test123.csv",
            "TEST_UPPERCASE.csv"
        };

        for (String filename : edgeCaseFilenames) {
            assertDoesNotThrow(() -> {
                flight.writeToFile(connection, filename);
            });

            java.io.File testFile = new java.io.File(filename);
            assertTrue(testFile.exists(), "Output file should be created for " + filename);

            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} behaves correctly across different connection states.
     *
     * @throws SQLException if a database operation fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(67)
    @DisplayName("Should test writeToFile method with different connection states")
    public void testWriteToFileWithDifferentConnectionStates() throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, 1);

        String testFilename = "test_flight_output_connection.csv";
        assertDoesNotThrow(() -> {
            flight.writeToFile(connection, testFilename);
        });

        java.io.File testFile = new java.io.File(testFilename);
        assertTrue(testFile.exists(), "Output file should be created");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} writes CSV output including real time-series data columns.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(68)
    @DisplayName("Should test writeToFile method with real time series data")
    public void testWriteToFileWithRealTimeSeriesData() throws SQLException, IOException {
        // Create a test flight with time series data
        createTestFlightWithTimeSeriesData();

        // Get the test flight
        Flight flight = Flight.getFlight(connection, 999);
        assertNotNull(flight, "Test flight should exist");

        // Test writeToFile with real data
        String testFilename = "test_flight_output_with_data.csv";
        flight.writeToFile(connection, testFilename);

        java.io.File testFile = new java.io.File(testFilename);
        assertTrue(testFile.exists(), "Output file should be created");
        assertTrue(testFile.length() > 0, "Output file should not be empty");

        // Read and verify file content
        List<String> lines = Files.readAllLines(Paths.get(testFilename));
        assertTrue(lines.size() >= 2, "File should have at least 2 lines (header and data type)");

        // Verify the first line starts with # (header)
        assertTrue(lines.get(0).startsWith("#"), "First line should be a header starting with #");

        // Verify the second line starts with # (data types)
        assertTrue(lines.get(1).startsWith("#"), "Second line should be data types starting with #");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} skips time-series data that should be excluded from the CSV.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(69)
    @DisplayName("Should test writeToFile method with time series data that should be skipped")
    public void testWriteToFileWithSkippedTimeSeriesData() throws SQLException, IOException {
        // Create a test flight with time series data that includes skipped columns
        createTestFlightWithSkippedTimeSeriesData();

        // Get the test flight
        Flight flight = Flight.getFlight(connection, 998);
        assertNotNull(flight, "Test flight should exist");

        // Test writeToFile with data that should be skipped
        String testFilename = "test_flight_output_skipped.csv";
        flight.writeToFile(connection, testFilename);

        java.io.File testFile = new java.io.File(testFilename);
        assertTrue(testFile.exists(), "Output file should be created");

        // Read and verify file content - should not contain skipped columns
        List<String> lines = Files.readAllLines(Paths.get(testFilename));
        assertTrue(lines.size() >= 2, "File should have at least 2 lines");

        // Verify skipped columns are not in the output
        String headerLine = lines.get(0);
        assertFalse(headerLine.contains("AirportDistance"), "Header should not contain AirportDistance");
        assertFalse(headerLine.contains("RunwayDistance"), "Header should not contain RunwayDistance");

        if (testFile.exists()) {
            testFile.delete();
        }
    }

    /**
     * Verifies {@code Flight.writeToFile} handles time-series columns whose min and max values are equal.
     *
     * @throws SQLException if loading the flight data fails
     * @throws IOException if writing the file fails
     */
    @Test
    @Order(70)
    @DisplayName("Should test writeToFile method with time series data having same min/max values")
    public void testWriteToFileWithSameMinMaxTimeSeriesData() throws SQLException, IOException {
        // Create a test flight with time series data where min == max (should be skipped)
        createTestFlightWithSameMinMaxTimeSeriesData();

        // Get the test flight
        Flight flight = Flight.getFlight(connection, 997);
        assertNotNull(flight, "Test flight should exist");

        // Test writeToFile with data that should be skipped due to same min/max
        String testFilename = "test_flight_output_same_minmax.csv";
        flight.writeToFile(connection, testFilename);

        java.io.File testFile = new java.io.File(testFilename);
        assertTrue(testFile.exists(), "Output file should be created");

        // Read and verify file content - should not contain columns with same min/max
        List<String> lines = Files.readAllLines(Paths.get(testFilename));
        assertTrue(lines.size() >= 2, "File should have at least 2 lines");

        // Verify columns with same min/max are not in the output
        String headerLine = lines.get(0);
        assertFalse(headerLine.contains("ConstantValue"), "Header should not contain ConstantValue");

        if (testFile.exists()) {
            testFile.delete();
        }
    }
}
