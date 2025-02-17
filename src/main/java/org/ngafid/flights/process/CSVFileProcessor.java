package org.ngafid.flights.process;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import org.ngafid.Database;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.*;
import org.ngafid.flights.Airframes.AliasKey;
import us.dustinj.timezonemap.TimeZoneMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

/**
 * Parses CSV files into Double and String time series, and returns a stream of flight builders.
 *
 * @author Aaron Chan
 * @author Joshua Karns
 * @author Roman Kozulia
 */
public class CSVFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(CSVFileProcessor.class.getName());
    private final List<String> headers;
    private final List<String> dataTypes;
    private final FlightMeta meta = new FlightMeta();
    private static final Pattern PARENTHESIS_PATTERN = Pattern.compile("\\(([^)]+)\\)");
    private static final int SPLIT_TIME_IN_MINUTES = 5; // Time difference indicates a separate flight.

    public CSVFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException {
        this(connection, stream.readAllBytes(), filename, pipeline);
    }

    private CSVFileProcessor(Connection connection, byte[] bytes, String filename, Pipeline pipeline) {
        super(connection, new ByteArrayInputStream(bytes), filename, pipeline);

        headers = new ArrayList<>();
        dataTypes = new ArrayList<>();

        meta.airframe = new Airframes.Airframe("Fixed Wing"); // Fixed Wing By default
        meta.filename = filename;
    }

    /**
     * Parses a CSV file containing flight data, processes the headers, validates rows,
     * and splits the data into multiple flight segments if time gaps exist.
     * For G5 flight recorders, calculates local time and time zone information.
     * Generates flight builders with corresponding time series data and returns them as a stream.
     *
     *  @return Stream of FlightBuilder objects representing parsed flights.
     * @throws FlightProcessingException
          * @throws SQLException 
          */
         public Stream<FlightBuilder> parse() throws FlightProcessingException, SQLException {
        LOG.info("Parsing " + this.meta.filename);

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();
        List<String[]> rows;
        List<String> headerLines;
        List<FlightBuilder> flightBuilders = new ArrayList<>();
        Map<String, Integer> g5headerIndices = null; // If we have G5 flight, we need header indices to calculate Local Date/Time

        // Extract headers and rows
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8))) {
            headerLines = extractHeaderLines(bufferedReader);
            String fileInformation = getFlightInfo(headerLines.get(0));

            if (meta.airframe != null && meta.airframe.getName().equals("ScanEagle")) {
                scanEagleParsing(fileInformation);
            } else {
                processFileInformation(fileInformation);
                if (!headerLines.isEmpty()) {
                    String[] dataTypesArray = headerLines.get(1).split(",", -1);
                    String[] headersArray = headerLines.get(2).split(",", -1);

                    Arrays.stream(dataTypesArray)
                            .map(String::strip)
                            .map(CSVFileProcessor::extractContentInsideParentheses)
                            .forEachOrdered(dataTypes::add);

                    Arrays.stream(headersArray)
                            .map(String::strip)
                            .forEachOrdered(headers::add);
                }
            }
            CSVReader csvReader = new CSVReader(bufferedReader);
            rows = new ArrayList<>(csvReader.readAll());
        } catch (IOException | CsvException | FatalFlightFileException e) {
            throw new FlightProcessingException(e);
        }

        boolean isG5FlightRecorder = isG5FlightRecorder(headerLines, rows.get(0));
        TimeZoneMap timeZoneMap = null;

        if (isG5FlightRecorder) {
            try {
                g5headerIndices = initializeAndValidateG5HeaderIndices(headerLines.get(2));
            } catch (FlightFileFormatException e) {
                throw new RuntimeException(e);
            }
            timeZoneMap = TimeZoneMap.forRegion(18.91, -179.15, 71.538800, -66.93457);
            int latitudeIndex = g5headerIndices.get("Latitude");
            int longitudeIndex = g5headerIndices.get("Longitude");
            rows = filterValidRowsG5(rows, latitudeIndex, longitudeIndex);
        }

        ArrayList<ArrayList<String>> columns = new ArrayList<>();
        for (int j = 0; j < headers.size(); j++) {
            columns.add(new ArrayList<>());
        }

        // Populate columns with data from rows
        for (String[] row : rows) {
            for (int j = 0; j < row.length; j++) {
                columns.get(j).add(row[j] != null ? row[j] : "");
            }
        }

        // Populate Time Series
        for (int j = 0; j < columns.size(); j++) {
            ArrayList<String> columnData = columns.get(j);
            String name = headers.get(j);
            String dataType = dataTypes.get(j);

            try {
                Double.parseDouble(columnData.get(0));  // Check if the column is numeric
                doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, columnData));
            } catch (NumberFormatException e) {
                stringTimeSeries.put(name, new StringTimeSeries(name, dataType, columnData));
            }
        }

        // Calculate G5-specific local date/time and timezone offset if applicable
        if (isG5FlightRecorder) {
            Map<String, StringTimeSeries> g5StringSeries = calculateG5LocalDateTimeAndOffset(
                    g5headerIndices, columns, headers, dataTypes, timeZoneMap);
            stringTimeSeries.putAll(g5StringSeries);
        }

        // Split the rows into flight segments bases on split time.
        List<Integer> splitIndices;
        try {
            splitIndices = splitCSVIntoFlightIndices(rows, SPLIT_TIME_IN_MINUTES);
        } catch (FlightFileFormatException e) {
            throw new FlightProcessingException("Error validating G5 headers", e);
        }
        // Process splits
        if (splitIndices.size() > 1) {
            flightBuilders.addAll(createFlightBuildersFromSegments(splitIndices, rows, doubleTimeSeries, stringTimeSeries, isG5FlightRecorder));
        } else {
            // if we don't have a split, we have a single splitIndex - 0 - no splits
            String md5Hash = calculateMd5Hash(rows);
            FlightMeta newMeta = new FlightMeta(meta);
            newMeta.setMd5Hash(md5Hash);

            FlightBuilder builder = isG5FlightRecorder
                    ? new G5FlightBuilder(newMeta, doubleTimeSeries, stringTimeSeries)
                    : new CSVFlightBuilder(newMeta, doubleTimeSeries, stringTimeSeries);

            flightBuilders.add(builder);
        }
        LOG.info("Parse method end. Returning " + flightBuilders.size() + " flight builders.");
        return flightBuilders.stream();
    }

    /**
     * G5 data recorder do not have local date time fields.
     * This method uses Latitude and Longitude and UTC Date Time (present in G5)
     * to calculate Local Date/Time and Offset.
     * @param g5headerIndices - map that tells us the index of a column we need.
     * @param columns
     * @param headers
     * @param dataTypes
     * @param timeZoneMap library we use to determine local date
     * @return
     */
    private Map<String, StringTimeSeries> calculateG5LocalDateTimeAndOffset(
            Map<String, Integer> g5headerIndices,
            List<ArrayList<String>> columns,
            List<String> headers,
            List<String> dataTypes,
            TimeZoneMap timeZoneMap) {

        Integer utcDateIndex = g5headerIndices.get("UTC Date");
        Integer utcTimeIndex = g5headerIndices.get("UTC Time");
        Integer latitudeIndex = g5headerIndices.get("Latitude");
        Integer longitudeIndex = g5headerIndices.get("Longitude");

        StringTimeSeries utcDateSeries = new StringTimeSeries(
                headers.get(utcDateIndex), dataTypes.get(utcDateIndex), columns.get(utcDateIndex));
        StringTimeSeries utcTimeSeries = new StringTimeSeries(
                headers.get(utcTimeIndex), dataTypes.get(utcTimeIndex), columns.get(utcTimeIndex));
        DoubleTimeSeries latitudeSeries = new DoubleTimeSeries(
                headers.get(latitudeIndex), dataTypes.get(latitudeIndex), columns.get(latitudeIndex));
        DoubleTimeSeries longitudeSeries = new DoubleTimeSeries(
                headers.get(longitudeIndex), dataTypes.get(longitudeIndex), columns.get(longitudeIndex));

        TimeUtils.LocalDateTimeResult localDateTimeResult = TimeUtils.calculateLocalDateTimeFromTimeSeries(
                timeZoneMap, utcDateSeries, utcTimeSeries, latitudeSeries, longitudeSeries);

        Map<String, StringTimeSeries> g5StringSeries = new HashMap<>();
        g5StringSeries.put("Lcl Date", new StringTimeSeries("Lcl Date", "yyyy-MM-dd", localDateTimeResult.getLocalDates()));
        g5StringSeries.put("Lcl Time", new StringTimeSeries("Lcl Time", "HH:mm:ss", localDateTimeResult.getLocalTimes()));
        g5StringSeries.put("UTCOfst", new StringTimeSeries("UTCOfst", "hh:mm", localDateTimeResult.getUtcOffsets()));

        return g5StringSeries;
    }

    /**
     * Created separate Flight Builders based of split indices.
     * @param splitIndices Indicate where the split begins. First index is always 0.
     * @param rows
     * @param doubleTimeSeries
     * @param stringTimeSeries
     * @param isG5FlightRecorder
     * @return List of FlightBuilder
     * @throws FlightProcessingException
     */
    private List<FlightBuilder> createFlightBuildersFromSegments(List<Integer> splitIndices,
                                                                 List<String[]> rows,
                                                                 Map<String, DoubleTimeSeries> doubleTimeSeries,
                                                                 Map<String, StringTimeSeries> stringTimeSeries,
                                                                 boolean isG5FlightRecorder) throws FlightProcessingException {

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

            FlightBuilder builder = isG5FlightRecorder
                    ? new G5FlightBuilder(newMeta, segmentDoubleSeries, segmentStringSeries)
                    : new CSVFlightBuilder(newMeta, segmentDoubleSeries, segmentStringSeries);

            segmentFlightBuilders.add(builder);
        }

        return segmentFlightBuilders;
    }

    /**
     * Creates a map of header indices by header name and retrieves required G5 column indices.
     * @param headerLine a coma separated string with header values.
     * @return Map of required G5 column names to their indices
     * @throws Exception if any required G5 headers are missing
     */
    private Map<String, Integer> initializeAndValidateG5HeaderIndices(String headerLine) throws FlightFileFormatException {
        String[] headers = headerLine.split(",");
        Map<String, Integer> headerIndices = new HashMap<>();

        // Populate header indices
        for (int i = 0; i < headers.length; i++) {
            headerIndices.put(headers[i].trim(), i);
        }

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
     * @param rows List of rows from the CSV file.
     * @param latitudeIndex Index of the Latitude column.
     * @param longitudeIndex Index of the Longitude column.
     * @return Filtered list of valid rows.
     */
    private List<String[]> filterValidRowsG5(List<String[]> rows, int latitudeIndex, int longitudeIndex) {
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
     * G5 data recorder have rows that have critical data missing:
     * Latitude,Longitude,AltGPS,GPS HDOP,GPS VDOP,GPS Velocity E (m/s),GPS Velocity N (m/s),GPS Velocity U (m/s)
     * Filters out rows where either Latitude or Longitude is missing or empty.
     * @param rows List of rows from the CSV file.
     * @return Filtered list of valid rows.
     */
    private List<String[]> filterValidRowsG5(List<String[]> rows) {
        List<String[]> validRows = new ArrayList<>();
        int rowsSkipped = 0;
        for (String[] row : rows) {
            // Assuming Latitude is at index 4 and Longitude is at index 5
            if (!row[4].isEmpty() && !row[5].isEmpty()) {
                validRows.add(row);
            } else {
                rowsSkipped++;
            }
        }
        LOG.info("Found and skipped in G5 file: " + rowsSkipped + "rows. Input rows size: " + rows.size() + ". valid rows size: " + validRows.size());
        return validRows;
    }

    /**
     * Determines if the data comes from G5 date recorder.
     * Checks the conditions:
     * If G5 is in the file name - we assume the data recorder is G5
     * If Headers contains serial_number AND no system_id AND no airframe_name AND
     * If the date is in the following format: M/d/yyyy
     * @param headerLines
     * @param firstRow
     * @return true or false
     */
    private boolean isG5FlightRecorder(List<String> headerLines, String[] firstRow) {
        String fileName = pipeline.getUpload().filename.toLowerCase();
        //G5 recorder has date in the format below
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        if (fileName.contains("g5")) {
            return true;
        }
        if (headerLines.get(0).contains("serial_number") &&
                !headerLines.get(0).contains("system_id") &&
                !headerLines.get(0).contains("airframe_name")) {
                // Check if the date in the expected format.
            try {
                LocalDate.parse(firstRow[0], formatter);
                return true;
            } catch (DateTimeParseException e) {
                 return false;
            }
        }
        return false;
    }

    /**
     * Calculates md5Hash from a given flight (flight entries)
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

    /**
     * Extracts three header lines from a csv file.
     * @param bufferedReader
     * @return List of header lines (3 lines expected)
     * @throws IOException
     */
    private List<String> extractHeaderLines(BufferedReader bufferedReader) throws IOException {
        String flightInformationLine = bufferedReader.readLine();
        String secondLine = bufferedReader.readLine();
        String thirdLine = bufferedReader.readLine();

        List<String> headerLines = new ArrayList<>();
        headerLines.add(flightInformationLine);
        headerLines.add(secondLine);
        headerLines.add(thirdLine);

        return headerLines;
    }


    /**
     *  G5, G3x data has metadata formated like this: UTC Date (yyyy-mm-dd),UTC Time (hh:mm:ss),
     *  This method extracts the content inside parentheses
     *  If parentheses not found, returns the original input.
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

    /**
     * Splits flights based on time intervals between rows and returns flight indices.
     * @param rows
     * @param splitIntervalInMinutes - max time difference between rows.
     * @return
     */
    public List<Integer> splitCSVIntoFlightIndices(List<String[]> rows, int splitIntervalInMinutes) throws FlightFileFormatException {
        List<Integer> splitIndices = new ArrayList<>();
        LocalDateTime lastTimestamp = null;


        // Determine the correct formatter based on the first row
        if (rows.isEmpty() || rows.get(0).length < 2) {
            throw new FlightFileFormatException("Rows are empty or do not contain sufficient columns for date and time.");
        }

        String firstDateTimeString = rows.get(0)[0] + " " + rows.get(0)[1];
        DateTimeFormatter correctFormatter = TimeUtils.findCorrectFormatter(firstDateTimeString.replaceAll("\\s+", " "));

        if (correctFormatter == null) {
            throw new DateTimeParseException("Unable to determine a valid date/time format for: " + firstDateTimeString, firstDateTimeString, 0);
        }

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);

            try {
                String dateTimeString = row[0] + " " + row[1]; // Assuming the first two columns are date and time
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
            } catch (Exception e) {
                LOG.warning("Skipping row due to unparseable date/time: " + Arrays.toString(row));
            }
        }
        return splitIndices;
    }

    /**
     * Gets the flight information from the first line of the file
     * @param fileInformation
     * @throws FatalFlightFileException
     * @throws IOException
     */
    private String getFlightInfo(String fileInformation) throws FatalFlightFileException {

        if (fileInformation == null || fileInformation.trim().length() == 0) {
            throw new FatalFlightFileException("The flight file was empty.");
        }

        if (fileInformation.charAt(0) != '#' && fileInformation.charAt(0) != '{') {
            if (fileInformation.startsWith("DID_")) {
                LOG.info("CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");

                meta.airframe = new Airframes.Airframe("ScanEagle");
                meta.airframeType = new Airframes.AirframeType("UAS Fixed Wing");
            } else {
                throw new FatalFlightFileException(
                        "First line of the flight file should begin with a '#' and contain flight recorder information.");
            }
        }
        return fileInformation;
    }

    /**
     * Flight files usually have two lines of meta-data at the top, which are proceeded by pound signs #.
     * An example line looks something like:
     * #system_id=xxx, key1=val1, key2=val2, ...
     * We gather the key:value pairs and pull out any useful information we find, storing the results in a FlightMeta
     * object.
          * @throws SQLException 
          */
         private void processFileInformation(String fileInformation) throws FatalFlightFileException, SQLException {
        // Some files have random double quotes in the header for some reason? We can
        // just remove these since we don't consider them anyways.
        fileInformation = fileInformation.replace("\"", "");
        String[] infoParts = fileInformation.split(",");

        HashMap<String, String> values = new HashMap<>(infoParts.length * 2);
        try {
            for (int i = 1; i < infoParts.length; i++) {
                // process everything else (G1000 data)
                if (infoParts[i].trim().length() == 0)
                    continue;

                String subParts[] = infoParts[i].trim().split("=");
                // May throw index out of bounds.
                values.put(subParts[0].trim(), subParts[1].trim());
            }
        } catch (IndexOutOfBoundsException e) {
            throw new FatalFlightFileException("Flight information line was not properly formed with key value pairs.", e);
        }

        if (!values.containsKey("system_id")) {
            if (values.containsKey("serial_number")) {
                values.put("system_id", values.get("serial_number"));
                LOG.severe("Log: serial_number is missing, replacing serial_number with system_id: " + values.get("system_id"));
            } 
            // else {
            //     values.put("system_id", "11111111111111");
            //     LOG.severe("!!! TESTING ONLY: Log: system_id is missing, setting to DummySystemId  - 111111111.");
            // }
        }

        // This is where we can integrate airframe name input from user.
        // Check if flight information contains airframe_name and system_id, if not, put dummy values (for testing).
        if (!values.containsKey("airframe_name")) {
            try(Connection connection = Database.getConnection()){
                
                LOG.info("Establishing Connection");

                LOG.info("system_id "+values.get("system_id"));

                String query = "Select system_id,fleet_id,airframe_id,airframes.airframe from system_id_to_airframe left join airframes ON system_id_to_airframe.airframe_id = airframes.id where system_id = ?";

                LOG.info("prepared statement start");
                
                PreparedStatement pmt = connection.prepareStatement(query);

                LOG.info("prepared statement done");

                pmt.setString(1, values.get("system_id"));

                LOG.info("system_id22 "+values.get("system_id"));

                ResultSet rs = pmt.executeQuery();

                while(rs.next()){
                    LOG.info("Enteredd ");
                    System.out.println("System ID " +rs.getString(1));
                    System.out.println("airframe_name "+rs.getString(4));
                    System.out.println("fleet_id "+rs.getInt(2));
                    System.out.println("airframe_id "+rs.getInt(3));
                    values.put("airframe_name", rs.getString(4));
                }

                //throw new Error("stopping the process");
            }
            catch(SQLException e){
                e.printStackTrace();
            }
            // values.put("airframe_name", "Cessna 172S");
            // LOG.severe("!!! TESTING ONLY: Log: airframe_name is missing, setting to DummyAirframe - Cessna 172S.");
        }

        for (var entry : values.entrySet()) {
            switch (entry.getKey()) {
                case "airframe_name":
                    LOG.info("case airframe_name "+entry.getValue());
                    setAirframeName(entry.getValue());
                    break;
                case "system_id":
                    meta.systemId = entry.getValue();
                    break;
                default:
                    continue;
            }
        }
    }

    private void setAirframeName(String name) throws FatalFlightFileException {
        var fleetKey = new AliasKey(name, pipeline.getUpload().getFleetId());
        var defaultKey = Airframes.defaultAlias(name);

        if (Airframes.AIRFRAME_ALIASES.containsKey(fleetKey)) {
            meta.airframe = new Airframes.Airframe(Airframes.AIRFRAME_ALIASES.get(fleetKey));
        } else if (Airframes.AIRFRAME_ALIASES.containsKey(defaultKey)) {
            meta.airframe = new Airframes.Airframe(Airframes.AIRFRAME_ALIASES.get(defaultKey));
        } else {
            meta.airframe = new Airframes.Airframe(name);
        }

        if (Airframes.FIXED_WING_AIRFRAMES.contains(meta.airframe.getName())
                || meta.airframe.getName().contains("Garmin")) {
            meta.airframeType = new Airframes.AirframeType("Fixed Wing");
        } else if (Airframes.ROTORCRAFT.contains(meta.airframe.getName())) {
            meta.airframeType = new Airframes.AirframeType("Rotorcraft");
        } else {
            LOG.severe("Could not import flight because the aircraft type was unknown for the following airframe name: '"
                            + meta.airframe.getName() + "'");
            LOG.severe(
                    "Please add this to the the `airframe_type` table in the database and update this method.");
            throw new FatalFlightFileException("Unsupported airframe type '" + name + "'");
        }
    }

    /**
     * Parses for ScanEagle flight data
     * 
     * @param fileInformation First line of the file
     */
    private void scanEagleParsing(String fileInformation) {
        // need a custom method to process ScanEagle data because the column
        // names are different and there is no header info
        scanEagleSetTailAndID();
        scanEagleHeaders(fileInformation);
    }

    /**
     * Handles setting the tail number and system id for ScanEagle data
     */
    private void scanEagleSetTailAndID() {
        String[] filenameParts = filename.split("_");
        meta.startDateTime = filenameParts[0];
        meta.endDateTime = meta.startDateTime;
        LOG.log(Level.INFO, "start date: '{0}'", meta.startDateTime);
        LOG.log(Level.INFO, "end date: '{0}'", meta.startDateTime);

        // UND doesn't have the systemId for UAS anywhere in the filename or file (sigh)
        meta.suggestedTailNumber = "N" + filenameParts[1] + "ND";
        meta.systemId = meta.suggestedTailNumber;

        LOG.log(Level.INFO, "suggested tail number: '{0}'", meta.suggestedTailNumber);
        LOG.log(Level.INFO, "system id: '{0}'", meta.systemId);
    }

    // TODO: Figure out ScanEagle data
    private void scanEagleHeaders(String fileInformation) {
        String headersLine = fileInformation;
        headers.addAll(Arrays.asList(headersLine.split("\\,", -1)));
        headers.replaceAll(String::trim);
        // scan eagle files have no data types, set all to ""
        for (int i = 0; i < headers.size(); i++) {
            dataTypes.add("none");
        }
    }
}
