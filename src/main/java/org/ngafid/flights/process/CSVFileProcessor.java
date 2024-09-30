package org.ngafid.flights.process;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
 * Handles parsing of CSV files
 *
 * @author Aaron Chan
 */
public class CSVFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(CSVFileProcessor.class.getName());
    private final List<String> headers;
    private final List<String> dataTypes;
    private final FlightMeta meta = new FlightMeta();
    String logTag = "CSVFileProcessor";

    public CSVFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException {
        this(connection, stream.readAllBytes(), filename, pipeline);
    }
    private CSVFileProcessor(Connection connection, byte[] bytes, String filename, Pipeline pipeline)
            throws IOException {
        super(connection, new ByteArrayInputStream(bytes), filename, pipeline);

        headers = new ArrayList<>();
        dataTypes = new ArrayList<>();

        meta.airframeType = "Fixed Wing"; // Fixed Wing By default
        meta.filename = filename;
    }



    public Stream<FlightBuilder> parse() throws FlightProcessingException{

        LOG.info(logTag+ "- parse - start");
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<FlightBuilder> flightBuilders = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8));
             ) {
            List<String> headerLines = extractHeaderLines(bufferedReader);
            String flightInformation = getFlightInfo(headerLines.get(0));
            processFileInformation(flightInformation);

            if (headerLines.get(1) != null) {
                if (meta.airframeName != null && meta.airframeName.equals("ScanEagle")) {
                    LOG.info(logTag+ " CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");
                    scanEagleParsing(flightInformation); // TODO: Handle ScanEagle data
                }
                // Split both secondLine and thirdLine into arrays
                String[] secondLineArray = headerLines.get(1).split(",", -1);  // Use -1 to preserve empty values
                String[] thirdLineArray = headerLines.get(2).split(",", -1);    // Use -1 to preserve empty values

                // Ensure both arrays have the same length
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
            updateAirframe();

            ArrayList<ArrayList<String>> columns = new ArrayList<>();
            CSVReader csvReader = new CSVReader(bufferedReader);
            String[] firstRow = csvReader.peek();

            for (int i = 0; i < firstRow.length; i++)
                columns.add(new ArrayList<>());

            // Documentation of CSVReader claims this is a linked list,
            // so it is important to iterate over it rather
            List<String[]> rows = csvReader.readAll();
            List<List<String[]>> flights = splitCSVIntoFlights(rows);

            // If we have more than 1 flight — we have splits, hence we need to create derived files.
            if (flights.size() > 1) {
                for (int i = 0; i < flights.size(); i++) {
                    List<String[]> flight = flights.get(i);

                    // Create a CSV filename for each flight
                    String derivedFilename = filename.replace(".csv", "_flight_" + (i + 1) + ".csv");

                    // Convert the flight data to CSV format and get it as a byte[]
                    byte[] csvData = writeFlightToCSV(flight,headerLines);

                    // Call addDerivedFile to store the CSV file
                    pipeline.addDerivedFile(derivedFilename, csvData);
                }
            }

            for(List<String[]> flight: flights){

                for (String[] row : flight) {
                    if (row.length < firstRow.length)
                        break;
                    for (int i = 0; i < row.length; i++)
                        columns.get(i).add(row[i]);
                }

                for (int i = 0; i < columns.size(); i++) {
                    var column = columns.get(i);
                    var name = headers.get(i);
                    var dataType = dataTypes.get(i);

                    try {
                        Double.parseDouble(column.get(0));
                        doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, column));
                    } catch (NumberFormatException e) {
                        stringTimeSeries.put(name, new StringTimeSeries(name, dataType, column));
                    }
                }

                String md5Hash = calculateMd5Hash(flight);

                // After a flight is processed, build a flight and add to array of flightbuilders.
                //Deep copy. Each FlightBuilder has its own FlightMeta object.
                FlightMeta newMeta = new FlightMeta(meta);
                newMeta.setMd5Hash(md5Hash);
                FlightBuilder builder = new CSVFlightBuilder(meta, doubleTimeSeries, stringTimeSeries);
                flightBuilders.add(builder);
                // Clear after variables after each iteration
                doubleTimeSeries.clear();
                stringTimeSeries.clear();
            }

        } catch (IOException | FatalFlightFileException | CsvException e) {
            throw new FlightProcessingException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return flightBuilders.stream();
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
            return DatatypeConverter.printHexBinary(flightHash).toLowerCase();
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

        if(secondLine.startsWith("#")){
            LOG.info(logTag + " extractHeaderLines " + " # found in the beginning of the second line, removing." );
            secondLine = secondLine.substring(1);  // Remove the first character (if it's '#')
        }

        List<String> headerLines = new ArrayList<>();
        headerLines.add(flightInformationLine);
        headerLines.add(secondLine);
        headerLines.add(thirdLine);

        return headerLines;
    }

    /**
     G5, G3x data has metadata formated like this: UTC Date (yyyy-mm-dd),UTC Time (hh:mm:ss),
     This method extracts the content inside parentheses
     If parentheses not found, returns the original input.
     */
    public static String extractContentInsideParentheses(String input) {

        Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(input);

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
     *
     * @param rows the list of CSV rows to process
     * @return a list of lists of type string, where each inner list represents a separate flight
     */
    public List<List<String[]>> splitCSVIntoFlights(List<String[]> rows) throws FlightProcessingException {

        List<List<String[]>> flights = new ArrayList<>();
        List<String[]> currentFlight = new ArrayList<>();
        LocalDateTime lastTimestamp = null;


        for (String[] row : rows) {
            // Parse timestamp from the row
            String dateTimeString = row[0] + " " + row[1]; // Assuming the first two columns are date and time



            LocalDateTime currentTimestamp = TimeUtils.parseDateTime(dateTimeString);

            if (lastTimestamp != null) {
                // Check if the time difference between consecutive rows exceeds 5 minutes
                Duration duration = Duration.between(currentTimestamp.toInstant(ZoneOffset.UTC), lastTimestamp.toInstant(ZoneOffset.UTC));
                System.out.println("Duration is: " + duration + "Between " + lastTimestamp + " and " + currentTimestamp);
                long timeDifferenceInMillis  = Math.abs(duration.toMillis());
                System.out.println("Time Difference is  " + timeDifferenceInMillis);
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
        }

        // Add the last flight to the list if not empty
        if (!currentFlight.isEmpty()) {
            flights.add(currentFlight);
        }
        LOG.info("CSVFileProcessor - extractContentInsideParentheses - returning number of flights: " + flights.size());
        return flights;
    }
    /**
     * Updates the airframe type if airframe name does not belong to fixed wing
     */
    private void updateAirframe() {
        if (meta.airframeName.equals("R44") || meta.airframeName.equals("Robinson R44")) {
            meta.airframeName = "R44";
            meta.airframeType = "Rotorcraft";
        }
    }


    private String getFlightInfo(String fileInformation) throws FatalFlightFileException{


        if (fileInformation == null || fileInformation.trim().length() == 0) {
            throw new FatalFlightFileException("The flight file was empty.");
        }

        if (fileInformation.charAt(0) != '#' && fileInformation.charAt(0) != '{') {
            if (fileInformation.startsWith("DID_")) {
                LOG.info("CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");

                meta.airframeName = "ScanEagle";
                meta.airframeType = "UAS Fixed Wing";
            } else {
                throw new FatalFlightFileException(
                        "First line of the flight file should begin with a '#' and contain flight recorder information.");
            }
        }

        return fileInformation;
    }

    private void processFileInformation(String fileInformation) throws FatalFlightFileException {
        // Some files have random double quotes in the header for some reason? We can
        // just remove these since we don't consider them anyways.
        fileInformation = fileInformation.replace("\"", "");
        String[] infoParts = fileInformation.split(",");

        try {
            for (int i = 1; i < infoParts.length; i++) {
                // process everything else (G1000 data)
                if (infoParts[i].trim().length() == 0)
                    continue;

                String subParts[] = infoParts[i].trim().split("=");
                String key = subParts[0];
                String value = subParts[1];

                // TODO: Create some sort of automatic mapping for synonomous airframe names.
                if (key.equals("airframe_name")) {
                    meta.airframeName = value;

                    // throw an error for 'Unknown Aircraft'
                    if (meta.airframeName.equals("Unknown Aircraft")) {
                        throw new FatalFlightFileException(
                                "Flight airframe name was 'Unknown Aircraft', please fix and re-upload so the flight can be properly identified and processed.");
                    }

                    if (meta.airframeName.equals("Diamond DA 40")) {
                        meta.airframeName = "Diamond DA40";
                    } else if ((meta.airframeName
                            .equals("Garmin Flight Display") || meta.airframeName.equals("Robinson R44 Raven I"))
                            && pipeline.getUpload().getFleetId() == 1 /*
                     * This is a hack for UND who has their airframe
                     * names
                     * set up
                     * incorrectly for their helicopters
                     */) {
                        meta.airframeName = "R44";
                    } else if (meta.airframeName.equals("Garmin Flight Display")) {
                        throw new FatalFlightFileException(
                                "Flight airframe name was 'Garmin Flight Display' which does not specify what airframe type the flight was, please fix and re-upload so the flight can be properly identified and processed.");
                    }

                    if (meta.airframeName.equals("Cirrus SR22 (3600 GW)")) {
                        meta.airframeName = "Cirrus SR22";
                    }

                    if (Airframes.FIXED_WING_AIRFRAMES.contains(meta.airframeName)
                            || meta.airframeName.contains("Garmin")) {
                        meta.airframeType = "Fixed Wing";
                    } else if (meta.airframeName.equals("R44") || meta.airframeName.equals("Robinson R44")) {
                        meta.airframeName = "R44";
                        meta.airframeType = "Rotorcraft";
                    } else {
                        System.err.println(
                                "Could not import flight because the aircraft type was unknown for the following airframe name: '"
                                        + meta.airframeName + "'");
                        System.err.println(
                                "Please add this to the the `airframe_type` table in the database and update this method.");
                        System.exit(1);
                    }

                } else if (key.equals("system_id")) {
                    meta.systemId = value;
                }
            }
        } catch (Exception e) {
            // LOG.info("parsting flight information threw exception: " + e);
            // e.printStackTrace();
            throw new FatalFlightFileException("Flight information line was not properly formed with key value pairs.",
                    e);
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
