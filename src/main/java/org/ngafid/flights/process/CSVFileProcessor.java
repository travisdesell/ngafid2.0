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
     */
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        LOG.info("Parsing " + this.meta.filename);
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<FlightBuilder> flightBuilders = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8))) {

            // Extract and process headers
            List<String> headerLines = extractHeaderLines(bufferedReader);
            String fileInformation = getFlightInfo(headerLines.get(0));


            if (meta.airframe != null && meta.airframe.getName().equals("ScanEagle")) {
                scanEagleParsing(fileInformation); // TODO: Handle ScanEagle data
            } else {
                processFileInformation(fileInformation);
                processHeaders(headerLines);
            }

            CSVReader csvReader = new CSVReader(bufferedReader);
            String[] firstRow = csvReader.peek();
            List<String[]> rows = csvReader.readAll();
            rows = filterValidRows(rows, headers);

            boolean isG5FlightRecorder = isG5FlightRecorder(headerLines, firstRow);

            // Library for mapping lat/long to timezones. Time zone for the US including Alaska and Hawaii.
            TimeZoneMap timeZoneMap = null;
            if (isG5FlightRecorder) {
                timeZoneMap = TimeZoneMap.forRegion(18.91, -179.15, 71.538800, -66.93457);
                rows = filterValidRowsG5(rows);
            }

            List<Integer> splitIndices = splitCSVIntoFlightIndices(rows, SPLIT_TIME_IN_MINUTES);
            addDerivedFileToUploadFolder(splitIndices, rows, headerLines, filename);

            // Process each flight by splitting the series based on the indices
            for (int i = 0; i < splitIndices.size(); i++) {
                int fromIndex = splitIndices.get(i);
                int toIndex = (i == splitIndices.size() - 1) ? rows.size() : splitIndices.get(i + 1);
                List<String[]> flightRows = rows.subList(fromIndex, toIndex);

                // Create columns for each flight segment
                ArrayList<ArrayList<String>> columns = new ArrayList<>();
                for (int j = 0; j < firstRow.length; j++) {
                    columns.add(new ArrayList<>());
                }
                // Populate columns with data from the flightRows
                for (String[] row : flightRows) {
                    if (row.length < firstRow.length) {
                        break;
                    }
                    for (int j = 0; j < row.length; j++) {
                        columns.get(j).add(row[j]);
                    }
                }
                // G5 doesn't have local date time and utcOffset column, we calculate them here
                TimeUtils.LocalDateTimeResult localDateTimeResult = null;
                if(isG5FlightRecorder) {
                    StringTimeSeries utcDateSeries = new StringTimeSeries(headers.get(0), dataTypes.get(0), columns.get(0));
                    StringTimeSeries utcTimeSeries = new StringTimeSeries(headers.get(1), dataTypes.get(1), columns.get(1));
                    DoubleTimeSeries latitudeTimeSeries = new DoubleTimeSeries(headers.get(4), dataTypes.get(4), columns.get(4));
                    DoubleTimeSeries longitudeTimeSeries = new DoubleTimeSeries(headers.get(5), dataTypes.get(5), columns.get(5));
                    localDateTimeResult = TimeUtils.calculateLocalDateTimeFromTimeSeries(timeZoneMap, utcDateSeries,utcTimeSeries,latitudeTimeSeries,longitudeTimeSeries);

                    stringTimeSeries.put("Lcl Date", new StringTimeSeries("Lcl Date", "yyyy-MM-dd", localDateTimeResult.getLocalDates()));
                    stringTimeSeries.put("Lcl Time", new StringTimeSeries("Lcl Time", "HH:mm:ss", localDateTimeResult.getLocalTimes()));
                    stringTimeSeries.put("UTCOfst", new StringTimeSeries("UTCOfst", "hh:mm", localDateTimeResult.getUtcOffsets()));
                }

                // Populate doubleTimeSeries and stringTimeSeries
                int startIndex = isG5FlightRecorder ? 2 : 0; // Skip first two columns (UTC date/time) if it is G5 flight recorder
                for (int j = startIndex; j < columns.size(); j++) {
                    var column = columns.get(j);
                    var name = headers.get(j);
                    var dataType = dataTypes.get(j);

                    try {
                        Double.parseDouble(column.get(0));
                        doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, column));
                    } catch (NumberFormatException e) {
                        stringTimeSeries.put(name, new StringTimeSeries(name, dataType, column));
                    }
                }

                String md5Hash = calculateMd5Hash(flightRows);

                // Build and add a flight
                FlightMeta newMeta = new FlightMeta(meta);
                newMeta.setMd5Hash(md5Hash);

                FlightBuilder builder;
                if (isG5FlightRecorder) {
                    builder = new G5FlightBuilder(newMeta, doubleTimeSeries, stringTimeSeries);
                } else {
                    builder = new CSVFlightBuilder(newMeta, doubleTimeSeries, stringTimeSeries);
                }

                flightBuilders.add(builder);
                doubleTimeSeries.clear();
                stringTimeSeries.clear();
            }
        } catch (IOException | FatalFlightFileException | CsvException e) {
            throw new FlightProcessingException(e);
        } catch (SQLException e) {
            String errorMessage = "SQL error occurred while processing the flight data for file: " + filename;
            throw new FlightProcessingException(errorMessage, e);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        LOG.info("Parse method end. Returning " + flightBuilders.size() + " flight builders.");
        return flightBuilders.stream();
    }

    /**
     *  Ands derived CSV files to the uploads folder
     * @param splitIndices
     * @param rows
     * @param headerLines
     * @param filename
     * @throws IOException
     * @throws SQLException
     */
    private void addDerivedFileToUploadFolder(List<Integer> splitIndices, List<String[]> rows, List<String> headerLines, String filename) throws IOException, SQLException {
        if (splitIndices.size() > 1) {
            for (int i = 0; i < splitIndices.size(); i++) {
                int fromIndex = splitIndices.get(i);
                int toIndex = (i == splitIndices.size() - 1) ? rows.size() : splitIndices.get(i + 1);
                List<String[]> flightRows = rows.subList(fromIndex, toIndex);

                String derivedFilename = filename.replace(".csv", "_flight_" + (i + 1) + ".csv");
                byte[] csvData = writeFlightToCSV(flightRows, headerLines);
                pipeline.addDerivedFile(derivedFilename, csvData);
            }
        }
    }

    /**
     * Filter out invalid rows that are not the same length as header rows.
     * @param rows
     * @param headers
     * @return list of valid rows
     * @throws FatalFlightFileException
     */
    private List<String[]> filterValidRows(List<String[]> rows, List<String> headers) throws FatalFlightFileException {
        List<String[]> validRows = new ArrayList<>();

        for (String[] row : rows) {
            // Only add the row if its length matches the number of headers
            if (row.length == headers.size()) {
                validRows.add(row);
            }
        }
        // If we detect an invalid row before the last row of the file, or there are no val
        if (validRows.size() < Math.max(rows.size() - 2, 0)) {
            throw new FatalFlightFileException("Flight file has 0 valid rows - something serious is wrong with the format.");
        }
        return validRows;
    }

    /**
     * G5 data recorder have rows that have critical data missing:
     * Latitude,Longitude,AltGPS,GPS HDOP,GPS VDOP,GPS Velocity E (m/s),GPS Velocity N (m/s),GPS Velocity U (m/s)
     * Filters out rows where either Latitude or Longitude is missing or empty.
     *
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
     * Processes header lines from the csv flight, populates dataTypes and headers with
     * the flight information.
     * @param headerLines
     * @throws FatalFlightFileException
     */
    private void processHeaders(List<String> headerLines) throws FatalFlightFileException {
        if (headerLines.get(1) != null) {

            String[] secondLineArray = headerLines.get(1).split(",", -1);  // Use -1 to preserve empty values
            String[] thirdLineArray = headerLines.get(2).split(",", -1);    // Use -1 to preserve empty values

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
     * Writes a List<String[]> (flight data) into CSV format and return it as a byte array
     * @param flightData
     * @param headerLines
     * @return
     * @throws IOException
     */
    private byte[] writeFlightToCSV(List<String[]> flightData, List<String> headerLines) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

            for (String line : headerLines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }

            for (String[] row : flightData) {
                String rowData = String.join(",", row);
                bufferedWriter.write(rowData);
                bufferedWriter.newLine();
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Splits flights based on time intervals between rows and returns flight indices.
     * @param rows
     * @param splitIntervalInMinutes - max time difference between rows.
     * @return
     */
    public List<Integer> splitCSVIntoFlightIndices(List<String[]> rows, int splitIntervalInMinutes) {
        List<Integer> splitIndices = new ArrayList<>();
        LocalDateTime lastTimestamp = null;


        // Determine the correct formatter based on the first row
        if (rows.isEmpty() || rows.get(0).length < 2) {
            throw new IllegalArgumentException("Rows are empty or do not contain sufficient columns for date and time.");
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

            } catch (IllegalArgumentException e) {
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
