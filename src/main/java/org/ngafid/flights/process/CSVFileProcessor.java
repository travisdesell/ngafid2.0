package org.ngafid.flights.process;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.*;
import org.ngafid.flights.Airframes.AliasKey;
import us.dustinj.timezonemap.TimeZoneMap;
import java.sql.Connection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
     * Reads a csv file and parses data into a list of FlightBuilder objects
     * @return A list of FlightBuilders (correspond to flights) as a Stream
     * @throws FlightProcessingException
     */
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();
        List<FlightBuilder> flightBuilders = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8))) {

            // Extract and process headers
            List<String> headerLines = extractHeaderLines(bufferedReader);
            String fileInformation = getFlightInfo(headerLines.get(0)); // Will not read a line. Header lines are already read by buffer reader.

            if (meta.airframe != null && meta.airframe.getName().equals("ScanEagle")) {
                scanEagleParsing(fileInformation); // TODO: Handle ScanEagle data
            } else {
                processFileInformation(fileInformation);
                processHeaders(headerLines);

            }

            CSVReader csvReader = new CSVReader(bufferedReader);
            String[] firstRow = csvReader.peek();
            List<String[]> rows = csvReader.readAll();

            boolean isG5FlightRecorder = isG5FlightRecorder(headerLines, firstRow);

            // Library for mapping lat/long to timezones.
            TimeZoneMap timeZoneMap = null;
            if (isG5FlightRecorder) {
                // TODO: TimeZoneEngine initialization take significant amount of time 1-2 seconds. If we expect many G5 files,
                //  timeZoneMap needs to be created once per program lifetime.
                // Time zone for the US including Alaska and Hawaii
                timeZoneMap = TimeZoneMap.forRegion(18.91, -179.15, 71.538800, -66.93457);
            }

            // Split the input list of entries from csv file into separate files if time difference between entries greater than 5 minutes.
            List<List<String[]>> flights = splitCSVIntoFlights(rows, isG5FlightRecorder);

            // Add derived csv files (if any) to the uploads folder.
            if (flights.size() > 1) {
                for (int i = 0; i < flights.size(); i++) {
                    List<String[]> flight = flights.get(i);
                    String derivedFilename = filename.replace(".csv", "_flight_" + (i + 1) + ".csv");
                    byte[] csvData = writeFlightToCSV(flight, headerLines);
                    pipeline.addDerivedFile(derivedFilename, csvData);
                }
            }

            // Process each flight
            for (List<String[]> flight : flights) {
                ArrayList<ArrayList<String>> columns = new ArrayList<>();
                for (int i = 0; i < firstRow.length; i++) {
                    columns.add(new ArrayList<>());
                }
                for (String[] row : flight) {
                    if (row.length < firstRow.length) {
                        break;
                    }
                    for (int i = 0; i < row.length; i++) {
                        columns.get(i).add(row[i]);
                    }
                }
                // Populate doubleTimeSeries and stringTimeSeries maps

                TimeUtils.LocalDateTimeResult localDateTimeResult = null;

                // Calculate local date time and offset for G5 data
                if (isG5FlightRecorder) {
                    localDateTimeResult = TimeUtils.calculateLocalDateTime(timeZoneMap, columns.get(0),columns.get(1),columns.get(4), columns.get(5));
                }

                for (int i = 0; i < columns.size(); i++) {
                    var column = columns.get(i);
                    var name = headers.get(i);
                    var dataType = dataTypes.get(i);
                    // Add local Date Time for G5 flight recorder
                    if (isG5FlightRecorder && (i  < 2)) {
                        if (i == 0) {
                            stringTimeSeries.put("Lcl Date", new StringTimeSeries("Lcl Date", dataType, localDateTimeResult.getLocalDates()));
                        } else {
                            stringTimeSeries.put("Lcl Time", new StringTimeSeries("Lcl Time", dataType, localDateTimeResult.getLocalTimes()));
                            stringTimeSeries.put("UTCOfst", new StringTimeSeries("UTCOfst", "hh:mm",localDateTimeResult.getUtcOffsets()));
                        }
                    } else {
                        try {
                            Double.parseDouble(column.get(0));
                            doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, column));
                        } catch (NumberFormatException e) {
                            stringTimeSeries.put(name, new StringTimeSeries(name, dataType, column));
                        }
                    }
                }
                String md5Hash = calculateMd5Hash(flight);

                // Build and add a flight
                FlightMeta newMeta = new FlightMeta(meta); // Deep copy. Each FlightBuilder has its own FlightMeta object.
                newMeta.setMd5Hash(md5Hash);

                FlightBuilder builder;
                if (isG5FlightRecorder) {
                    builder  = new G5FlightBuilder(newMeta, doubleTimeSeries, stringTimeSeries);

                } else {
                    builder = new CSVFlightBuilder(newMeta, doubleTimeSeries, stringTimeSeries);
                }

                flightBuilders.add(builder);

                // Clear data for the next flight
                doubleTimeSeries.clear();
                stringTimeSeries.clear();
            }
        } catch (IOException | FatalFlightFileException | CsvException e) {
            throw new FlightProcessingException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        LOG.info( "Parse method end. Returning flights: " + flightBuilders.size());
        return flightBuilders.stream();
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
                    return true; // If the date is valid, we return true
                } catch (DateTimeParseException e) {
                    // If the date is not valid, we return false or handle the logic accordingly
                    return false;
                }
            }
            return false;
    }

    /**
         * Processes header lines from the csv flight, populates dataTypes and headers with
         * the flight information.
         * @param headerLines
         * @throws FatalFlightFileException
     */
    private void processHeaders(List<String> headerLines) throws FatalFlightFileException {
        if (headerLines.get(1) != null) {

            String[] secondLineArray = headerLines.get(1).split(",", -1);  // Use -1 to preserve empty values
            String[] thirdLineArray = headerLines.get(2).split(",", -1);    // Use -1 to preserve empty values

            if (secondLineArray.length != thirdLineArray.length) {
                throw new IllegalArgumentException("Lines have different lengths.");
            }

            for (int i = 0; i < secondLineArray.length; i++) {
                String processedDataType = CSVFileProcessor.extractContentInsideParentheses(secondLineArray[i].strip());
                dataTypes.add(processedDataType);  // Add the processed result to dataTypes
                String processedHeader = thirdLineArray[i].strip();
                headers.add(processedHeader.isEmpty() ? "<Empty>" : processedHeader);  // Add header or "<Empty>" if it's blank
            }
        }
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
     * Extracts three header lines from a csv file, removes # from the beginning of the second line (if # is present)
     * @param bufferedReader
     * @return List of header lines (3 lines expected)
     * @throws IOException
     */
    private List<String> extractHeaderLines(BufferedReader bufferedReader) throws IOException {
        String flightInformationLine = bufferedReader.readLine();
        String secondLine = bufferedReader.readLine();
        String thirdLine = bufferedReader.readLine();

        if (secondLine.startsWith("#")) {
            LOG.info( "extractHeaderLines " + " # found in the beginning of the second line, removing." );
            secondLine = secondLine.substring(1);  // Remove the first character (if it's '#')
        }

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
        // If no parentheses are found, return the entire string
        return input;
    }

    /**
     * Write a List<String[]> (flight data) into CSV format and return it as a byte array
     * @param flightData
     * @param headerLines
     * @return
     * @throws IOException
     */
    private byte[] writeFlightToCSV(List<String[]> flightData, List<String> headerLines) throws IOException {
        LOG.info("CSVFileProcessor - writeFlightToCSV - start");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        try {
            for (String line : headerLines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }

            for (String[] row : flightData) {
                String rowData = String.join(",", row);
                bufferedWriter.write(rowData);
                bufferedWriter.newLine();
            }
        } finally {
            bufferedWriter.flush();  // Ensure all data is written
        }
        return outputStream.toByteArray();
    }

    /**
     * Splits a list of CSV rows into multiple flights based on a 5-minute time gap.
     * If a row has an invalid date time, the line is skipped.
     * @param rows the list of CSV rows to process
     * @return a list of lists of type string, where each inner list represents a separate flight
     */
    public List<List<String[]>> splitCSVIntoFlights(List<String[]> rows, boolean isG5flightRecorder) {

        List<List<String[]>> flights = new ArrayList<>();
        List<String[]> currentFlight = new ArrayList<>();
        LocalDateTime lastTimestamp = null;

        for (String[] row : rows) {
            if (isG5flightRecorder && row[4].isEmpty()) {
                continue;
            }
            try {
                String dateTimeString = row[0] + " " + row[1]; // Assuming the first two columns are date and time
                String normalizedDateTime = dateTimeString.replaceAll("\\s+", " ");
                LocalDateTime currentTimestamp;

                try {
                    // Find the correct formatter for the current date-time string
                    DateTimeFormatter correctFormatter = TimeUtils.findCorrectFormatter(normalizedDateTime);

                    if (correctFormatter == null) {
                        throw new DateTimeParseException("Unable to find correct formatter for  date/time: " + dateTimeString, dateTimeString, 0);
                    }
                    // Try to find a formatter if previous fails.
                    currentTimestamp = LocalDateTime.parse(normalizedDateTime, correctFormatter);

                } catch (DateTimeParseException e) {
                    LOG.severe("Failed to parse date/time: " + normalizedDateTime + ". Error: " + e.getMessage());
                    throw e;  // Rethrow or handle the exception
                }

                if (lastTimestamp != null) {
                    // Check if the time difference between consecutive rows exceeds 5 minutes
                    Duration duration = Duration.between(currentTimestamp.toInstant(ZoneOffset.UTC), lastTimestamp.toInstant(ZoneOffset.UTC));
                    long timeDifferenceInMillis = Math.abs(duration.toMillis());
                    if (timeDifferenceInMillis > 5 * 60 * 1000) { // More than 5 minutes

                        // Add the current flight to the list of flights and start a new flight
                        flights.add(new ArrayList<>(currentFlight)); // Add the current flight
                        currentFlight.clear(); // Reset for a new flight
                    }
                }

                // Add the current row to the current flight
                currentFlight.add(row);

                // Update lastTimestamp to the current row's timestamp
                lastTimestamp = currentTimestamp;

            } catch (IllegalArgumentException e) {
                // Log detailed error
                LOG.warning("Skipping row due to unparseable date/time: " + Arrays.toString(row));
            }
        }

        // Add the last flight to the list if not empty
        if (!currentFlight.isEmpty()) {
            flights.add(currentFlight);
        }

        LOG.info("CSVFileProcessor - returning number of flights: " + flights.size());
        return flights;
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
     *
     * #system_id=xxx, key1=val1, key2=val2, ...
     *
     * We gather the key:value pairs and pull out any useful information we find, storing the results in a FlightMeta
     * object.
     */
    private void processFileInformation(String fileInformation) throws FatalFlightFileException {
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
        // This is where we can integrate airframe name input from user.
        // Check if flight information contains airframe_name and system_id, if not, put dummy values (for testing).
        if (!values.containsKey("airframe_name")) {
            values.put("airframe_name", "Cessna 172S");
            LOG.severe("!!! TESTING ONLY: Log: airframe_name is missing, setting to DummyAirframe - Cessna 172S.");
        }

        if (!values.containsKey("system_id")) {
            if (values.containsKey("serial_number")) {
                values.put("system_id", values.get("serial_number"));
                    LOG.severe("Log: serial_number is missing, replacing serial_number with system_id: " + values.get("system_id"));
            } else {
                values.put("system_id", "11111111111111");
                LOG.severe("!!! TESTING ONLY: Log: system_id is missing, setting to DummySystemId  - 111111111.");
            }
        }

        for (var entry : values.entrySet()) {
            switch (entry.getKey()) {
                case "airframe_name":
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
