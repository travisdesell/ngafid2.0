package org.ngafid.uploads.process.format;

import org.jetbrains.annotations.NotNull;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.*;

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
            LOG.info("Md5 hash calculated: " + result);
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

        try {
            calculateLocalDateTimeAndOffset(doubleTimeSeries, stringTimeSeries);
            List<Integer> splitIndices = splitCSVIntoFlightIndices(stringTimeSeries, SPLIT_TIME_IN_MINUTES);
            return createFlightBuildersFromSegments(splitIndices, rows, doubleTimeSeries, stringTimeSeries).stream();
        } catch (FlightFileFormatException | TimeUtils.UnrecognizedDateTimeFormatException | NullPointerException e) {
            throw new FlightProcessingException(e);
        }

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
    public List<Integer> splitCSVIntoFlightIndices(Map<String, StringTimeSeries> stringTimeSeries, int splitIntervalInMinutes) throws FlightFileFormatException, TimeUtils.UnrecognizedDateTimeFormatException {
        List<Integer> splitIndices = new ArrayList<>();
        LocalDateTime lastTimestamp = null;

        // Determine the correct formatter based on the first row
        StringTimeSeries dateSeries = stringTimeSeries.get("UTC Date");
        StringTimeSeries timeSeries = stringTimeSeries.get("UTC Time");
        DateTimeFormatter correctFormatter = getDateTimeFormatter(dateSeries.get(dateSeries.size() / 2), timeSeries.get(dateSeries.size() / 2));

        for (int i = 0; i < dateSeries.size(); i++) {

            String dateTimeString = dateSeries.get(i) + " " + timeSeries.get(i); // Assuming the first two columns are date and time
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
    private static DateTimeFormatter getDateTimeFormatter(String date, String time) throws TimeUtils.UnrecognizedDateTimeFormatException {
        String firstDateTimeString = date + " " + time;
        DateTimeFormatter correctFormatter = TimeUtils.findCorrectFormatter(firstDateTimeString.replaceAll("\\s+", " "));
        return correctFormatter;
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
            Map<String, StringTimeSeries> stringTimeSeries) throws TimeUtils.UnrecognizedDateTimeFormatException {

        StringTimeSeries utcDateSeries = stringTimeSeries.get("UTC Date");
        StringTimeSeries utcTimeSeries = stringTimeSeries.get("UTC Time");
        DoubleTimeSeries latitudeSeries = doubleTimeSeries.get("Latitude");
        DoubleTimeSeries longitudeSeries = doubleTimeSeries.get("Longitude");

        if (Stream.of(utcTimeSeries, utcTimeSeries, latitudeSeries, longitudeSeries).anyMatch(Objects::isNull)) {
            throw new NullPointerException("Need UTC Date, UTC Time, Latitude, and Longitude to import G5 / G3X data.");
        }

        TimeUtils.LocalDateTimeResult localDateTimeResult = TimeUtils.calculateLocalDateTimeFromTimeSeries(
                utcDateSeries, utcTimeSeries, latitudeSeries, longitudeSeries);

        stringTimeSeries.put("Lcl Date", new StringTimeSeries("Lcl Date", "yyyy-MM-dd", localDateTimeResult.getLocalDates()));
        stringTimeSeries.put("Lcl Time", new StringTimeSeries("Lcl Time", "HH:mm:ss", localDateTimeResult.getLocalTimes()));
        stringTimeSeries.put("UTCOfst", new StringTimeSeries("UTCOfst", "hh:mm", localDateTimeResult.getUtcOffsets()));
    }
}