package org.ngafid.flights.process;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import com.opencsv.exceptions.CsvException;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();


        String airframeInfoLine = null;
        String csvHeaderLine = null;

        List<FlightBuilder> flightBuilders = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8)); CSVReader csvReader = new CSVReader(bufferedReader)) {

            String fileInformation = getFlightInfo(bufferedReader); // Will read a line

            if (meta.airframeName != null && meta.airframeName.equals("ScanEagle")) {
                scanEagleParsing(fileInformation); // TODO: Handle ScanEagle data
            } else {
                processFileInformation(fileInformation);
                bufferedReader.read(); // Skip first char (#)
                Arrays.stream(csvReader.readNext())
                        .map(String::strip)
                        .forEachOrdered(dataTypes::add);;
                Arrays.stream(csvReader.readNext())
                        .map(String::strip)
                        .forEachOrdered(headers::add);;
            }

            updateAirframe();

            ArrayList<ArrayList<String>> columns = new ArrayList<>();
            String[] firstRow = csvReader.peek();


            for (int i = 0; i < firstRow.length; i++)
                columns.add(new ArrayList<>());

            // Documentation of CSVReader claims this is a linked list,
            // so it is important to iterate over it rather than using indexing
            List<String[]> rows = csvReader.readAll();

            List<List<String[]>> flights = splitCSVIntoFlights(rows);

            // If we have more than 1 flight — we have splits, hence we need to create derived files.
            if (flights.size() > 1) {
                for (int i = 0; i < flights.size(); i++) {
                    List<String[]> flight = flights.get(i);

                    // Create a CSV filename for each flight
                    String derivedFilename = filename.replace(".csv", "_flight_" + (i + 1) + ".csv");

                    // Convert the flight data to CSV format and get it as a byte[]
                    byte[] csvData = writeFlightToCSV(flight);

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

                //Calculate MD5 hash for the current flight's rows
                String md5Hash = null;
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    StringBuilder flightDataBuilder = new StringBuilder();

                    for (String[] flightRow : flight) {
                        for (String cell : flightRow) {
                            flightDataBuilder.append(cell);
                        }
                    }

                    byte[] flightHash = md.digest(flightDataBuilder.toString().getBytes(StandardCharsets.UTF_8));
                    md5Hash = DatatypeConverter.printHexBinary(flightHash).toLowerCase();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

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

    // Method to write a List<String[]> (flight data) into CSV format and return it as a byte array
    private byte[] writeFlightToCSV(List<String[]> flightData) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            // Write each row of flight data to the CSV
            for (String[] row : flightData) {
                csvWriter.writeNext(row);
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Splits a list of CSV rows into multiple flights based on a 5-minute time gap.
     *
     * @param rows the list of CSV rows to process
     * @return a list of lists of type string, where each inner list represents a separate flight
     */
    public List<List<String[]>> splitCSVIntoFlights(List<String[]> rows) throws DateTimeParseException {
        List<List<String[]>> flights = new ArrayList<>();
        List<String[]> currentFlight = new ArrayList<>();
        Date lastTimestamp = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (String[] row : rows) {

            // Parse timestamp from the row
            String dateTimeString = row[0] + " " + row[1]; // Assuming the first two columns are date and time
            Date currentTimestamp = parseDateTime(dateTimeString, formatter);

            if (lastTimestamp != null) {
                // Check if the time difference between consecutive rows exceeds 5 minutes
                long timeDifferenceInMillis = currentTimestamp.getTime() - lastTimestamp.getTime();
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

        return flights;
    }

    /**
     * Parses a date-time string into a Date object.
     *
     * @param dateTimeString the date-time string to parse
     * @param formatter      the SimpleDateFormat object used to parse the string
     * @return the parsed Date object
     * @throws DateTimeParseException if the date-time string cannot be parsed
     */
    private Date parseDateTime(String dateTimeString, SimpleDateFormat formatter) throws DateTimeParseException {
        try {
            return formatter.parse(dateTimeString);
        } catch (ParseException e) {
            throw new DateTimeParseException(dateTimeString, e.getMessage(), e.getErrorOffset());
        }
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

    /**
     * Gets the flight information from the first line of the file
     *
     * @param reader BufferedReader for reading the first line
     * @return
     * @throws FatalFlightFileException
     * @throws IOException
     */
    private String getFlightInfo(BufferedReader reader) throws FatalFlightFileException, IOException {
        String fileInformation = reader.readLine();

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
