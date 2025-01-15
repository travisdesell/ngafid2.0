package org.ngafid.flights.process.formats;

import org.jetbrains.annotations.NotNull;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.flights.process.*;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File processor for Garmin G5 (and G3x, I think).
 *
 * @author Joshua Karns
 * @author Roman Kozulia
 */
public final class G5CSVFileProcessor extends CSVFileProcessor {
    private static final Logger LOG = Logger.getLogger(G5CSVFileProcessor.class.getName());

    static final int SPLIT_TIME_IN_MINUTES = 5; // Time difference indicates a separate flight.
    static final Pattern PARENTHESIS_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    /**
     * Flight builder for G5FlightBuilder
     */
    public static class G5FlightBuilder extends FlightBuilder {

        public G5FlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                               Map<String, StringTimeSeries> stringTimeSeries) {
            super(meta, doubleTimeSeries, stringTimeSeries);
        }

        private final Map<String, Set<String>> ALIASES = Map.of(
                "AltAGL", Set.of("Altitude Above Ground Level"));

        @Override
        protected final Map<String, Set<String>> getAliases() {
            return ALIASES;
        }
    }

    public G5CSVFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline) throws IOException {
        super(connection, stream, filename, pipeline);
    }

    /**
     * Calculates md5Hash from a given flight (flight entries)
     *
     * @param flight
     * @return md5Hash
     * @throws FlightProcessingException
     */
    private String calculateMd5Hash(List<String[]> flight) throws FlightProcessingException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder flightDataBuilder = new StringBuilder();
            // Append each cell from the flight data to the builder
            for (String[] flightRow : flight) {
                for (String cell : flightRow) {
                    flightDataBuilder.append(cell);
                }
            }
            // Calculate MD5 hash and convert to hex
            byte[] flightHash = md.digest(flightDataBuilder.toString().getBytes(StandardCharsets.UTF_8));
            String result = DatatypeConverter.printHexBinary(flightHash).toLowerCase();
            System.out.println("Md5 hash calculated: " + result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new FlightProcessingException(e);
        }
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        LOG.info("Parsing " + this.meta.filename);

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<String[]> rows = extractFlightData();

        readTimeSeries(rows, doubleTimeSeries, stringTimeSeries);

        // We cannot filter out "invalid" rows or else our aircraft will travel through time. Many of our analyses assume a 1-second gap.
        // rows = filterInvalidRows(rows, latitudeIndex, longitudeIndex);

        // Calculate G5-specific local date/time and timezone offset if applicable. Adds them to stringTimeSeries directly.
        calculateLocalDateTimeAndOffset(doubleTimeSeries, stringTimeSeries);

        // Split the rows into flight segments bases on split time.
        List<Integer> splitIndices;
        try {
            splitIndices = splitCSVIntoFlightIndices(stringTimeSeries, SPLIT_TIME_IN_MINUTES);
        } catch (FlightFileFormatException e) {
            throw new FlightProcessingException("Error validating G5 headers", e);
        }

        return createFlightBuildersFromSegments(splitIndices, rows, doubleTimeSeries, stringTimeSeries).stream();
    }

    /**
     * G5, G3x data has metadata formated like this: UTC Date (yyyy-mm-dd),UTC Time (hh:mm:ss),
     * This method extracts the content inside parentheses
     * If parentheses not found, returns the original input.
     *
     * @param input
     * @return
     */
    public static String extractContentInsideParentheses(String input) {
        Matcher matcher = PARENTHESIS_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);  // Content inside parentheses
        }
        return input;
    }

    @Override
    List<String> processDataTypes(BufferedReader reader) throws FatalFlightFileException {
        // Note that we skip the first character -- this should be a hash mark.
        try {
            String dataTypesLine = reader.readLine();
            String[] dataTypesArray = dataTypesLine.substring(1).split(",", -1);
            return Arrays.stream(dataTypesArray)
                    .map(String::strip)
                    .map(G5CSVFileProcessor::extractContentInsideParentheses)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- cannot process file.", e);
        }
    }

    /**
     * Created separate Flight Builders based of split indices.
     *
     * @param splitIndices     Indicate where the split begins. First index is always 0.
     * @param rows
     * @param doubleTimeSeries
     * @param stringTimeSeries
     * @return List of FlightBuilder
     * @throws FlightProcessingException
     */
    private List<FlightBuilder> createFlightBuildersFromSegments(List<Integer> splitIndices,
                                                                 List<String[]> rows,
                                                                 Map<String, DoubleTimeSeries> doubleTimeSeries,
                                                                 Map<String, StringTimeSeries> stringTimeSeries) throws FlightProcessingException {

        List<FlightBuilder> segmentFlightBuilders = new ArrayList<>();

        for (int i = 0; i < splitIndices.size(); i++) {
            int fromIndex = (i == 0) ? 0 : splitIndices.get(i);
            int toIndex = (i == splitIndices.size() - 1) ? rows.size() : splitIndices.get(i + 1);

            Map<String, DoubleTimeSeries> segmentDoubleSeries = new HashMap<>();
            for (Map.Entry<String, DoubleTimeSeries> entry : doubleTimeSeries.entrySet()) {
                try {
                    segmentDoubleSeries.put(entry.getKey(), entry.getValue().subSeries(fromIndex, toIndex));
                } catch (SQLException e) {
                    LOG.warning("Error slicing DoubleTimeSeries for segment from " + fromIndex + " to " + toIndex);
                }
            }

            Map<String, StringTimeSeries> segmentStringSeries = new HashMap<>();
            for (Map.Entry<String, StringTimeSeries> entry : stringTimeSeries.entrySet()) {
                try {
                    segmentStringSeries.put(entry.getKey(), entry.getValue().subSeries(fromIndex, toIndex));
                } catch (SQLException e) {
                    LOG.warning("Error slicing StringTimeSeries for segment from " + fromIndex + " to " + toIndex);
                }
            }

            List<String[]> flightRows = rows.subList(fromIndex, toIndex);
            String md5Hash = calculateMd5Hash(flightRows);
            FlightMeta newMeta = new FlightMeta(meta);
            newMeta.setMd5Hash(md5Hash);

            FlightBuilder builder = new G5FlightBuilder(newMeta, segmentDoubleSeries, segmentStringSeries);

            segmentFlightBuilders.add(builder);
        }

        return segmentFlightBuilders;
    }


    /**
     * Splits flights based on time intervals between rows and returns flight indices.
     *
     * @param splitIntervalInMinutes - max time difference between rows.
     * @return
     */
    public List<Integer> splitCSVIntoFlightIndices(Map<String, StringTimeSeries> stringTimeSeries, int splitIntervalInMinutes) throws FlightFileFormatException {
        List<Integer> splitIndices = new ArrayList<>();
        LocalDateTime lastTimestamp = null;

        // Determine the correct formatter based on the first row
        StringTimeSeries dateSeries = stringTimeSeries.get("UTC Date");
        StringTimeSeries timeSeries = stringTimeSeries.get("UTC Time");
        DateTimeFormatter correctFormatter = getDateTimeFormatter(dateSeries.get(0), dateSeries.get(0));

        for (int i = 0; i < dateSeries.size(); i++) {

            String dateTimeString = dateSeries + " " + timeSeries; // Assuming the first two columns are date and time
            String normalizedDateTime = dateTimeString.replaceAll("\\s+", " ");

            LocalDateTime currentTimestamp;
            try {
                currentTimestamp = LocalDateTime.parse(normalizedDateTime, correctFormatter);
            } catch (DateTimeParseException e) {
                LOG.severe("Failed to parse date/time: " + normalizedDateTime + ". Error: " + e.getMessage());
                continue; // Skip this row due to parsing error
            }

            // Check time difference from the last timestamp and determine if a new flight starts
            if (lastTimestamp != null) {
                Duration duration = Duration.between(lastTimestamp, currentTimestamp);
                long timeDifferenceInMillis = Math.abs(duration.toMillis());

                if (timeDifferenceInMillis > (long) splitIntervalInMinutes * 60 * 1000) {
                    splitIndices.add(i); // Mark the start of a new flight
                }
            } else {
                splitIndices.add(i); // First flight always starts at index 0
            }
            lastTimestamp = currentTimestamp;
        }

        return splitIndices;
    }

    @NotNull
    private static DateTimeFormatter getDateTimeFormatter(String date, String time) throws FlightFileFormatException {
        String firstDateTimeString = date + " " + time;
        DateTimeFormatter correctFormatter = TimeUtils.findCorrectFormatter(firstDateTimeString.replaceAll("\\s+", " "));

        if (correctFormatter == null) {
            throw new DateTimeParseException("Unable to determine a valid date/time format for: " + firstDateTimeString, firstDateTimeString, 0);
        }
        return correctFormatter;
    }

    /**
     * Creates a map of header indices by header name and retrieves required G5 column indices.
     *
     * @param headerLine a coma separated string with header values.
     * @return Map of required G5 column names to their indices
     * @throws Exception if any required G5 headers are missing
     */
    private Map<String, Integer> initializeAndValidateHeaderIndices(String headerLine) throws FlightFileFormatException {
        Map<String, Integer> headerIndices = new HashMap<>();

        // Populate header indices
        for (int i = 0; i < headers.size(); i++)
            headerIndices.put(this.headers.get(i).trim(), i);

        // Define and validate required G5 headers
        List<String> requiredG5Headers = Arrays.asList("UTC Date", "UTC Time", "Latitude", "Longitude");
        Map<String, Integer> g5Indices = new HashMap<>();

        for (String g5Header : requiredG5Headers) {
            Integer index = headerIndices.get(g5Header);
            if (index == null) {
                throw new FlightFileFormatException("Required G5 header '" + g5Header + "' not found in CSV file.");
            }
            g5Indices.put(g5Header, index);
        }

        return g5Indices;
    }

    /**
     * Filters out rows where either the "Latitude" or "Longitude" column is missing or empty.
     *
     * @param rows           List of rows from the CSV file.
     * @param latitudeIndex  Index of the Latitude column.
     * @param longitudeIndex Index of the Longitude column.
     * @return Filtered list of valid rows.
     */
    private List<String[]> filterInvalidRows(List<String[]> rows, int latitudeIndex, int longitudeIndex) {
        List<String[]> validRows = new ArrayList<>();
        int rowsSkipped = 0;

        for (String[] row : rows) {
            if (!row[latitudeIndex].isEmpty() && !row[longitudeIndex].isEmpty()) {
                validRows.add(row);
            } else {
                rowsSkipped++;
            }
        }

        LOG.info("Found and skipped in G5 file: " + rowsSkipped + " rows. Input rows size: " + rows.size() + ". Valid rows size: " + validRows.size());
        return validRows;
    }

    /**
     * G5 data recorder do not have local date time fields.
     * This method uses Latitude and Longitude and UTC Date Time (present in G5)
     * to calculate Local Date/Time and Offset.
     *
     * @return
     */
    private void calculateLocalDateTimeAndOffset(
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {

        StringTimeSeries utcDateSeries = stringTimeSeries.get("UTC Date");
        StringTimeSeries utcTimeSeries = stringTimeSeries.get("UTC Time");
        DoubleTimeSeries latitudeSeries = doubleTimeSeries.get("Latitude");
        DoubleTimeSeries longitudeSeries = doubleTimeSeries.get("Longitude");

        TimeUtils.LocalDateTimeResult localDateTimeResult = TimeUtils.calculateLocalDateTimeFromTimeSeries(
                utcDateSeries, utcTimeSeries, latitudeSeries, longitudeSeries);

        stringTimeSeries.put("Lcl Date", new StringTimeSeries("Lcl Date", "yyyy-MM-dd", localDateTimeResult.getLocalDates()));
        stringTimeSeries.put("Lcl Time", new StringTimeSeries("Lcl Time", "HH:mm:ss", localDateTimeResult.getLocalTimes()));
        stringTimeSeries.put("UTCOfst", new StringTimeSeries("UTCOfst", "hh:mm", localDateTimeResult.getUtcOffsets()));

    }
}
