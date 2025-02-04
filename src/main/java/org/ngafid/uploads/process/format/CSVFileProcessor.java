package org.ngafid.uploads.process.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import org.ngafid.flights.Airframes;
import org.ngafid.flights.Airframes.AliasKey;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.FlightMeta;
import org.ngafid.uploads.process.FlightProcessingException;
import org.ngafid.uploads.process.Pipeline;
import org.ngafid.uploads.process.steps.ProcessStep;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;

/**
 * Parses CSV files into Double and String time series, and returns a stream of flight builders.
 *
 * @author Aaron Chan
 * @author Joshua Karns
 * @author Roman Kozulia
 */
public class CSVFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(CSVFileProcessor.class.getName());

    List<String> headers;
    List<String> dataTypes;
    final FlightMeta meta = new FlightMeta();

    static class CSVFlightBuilder extends FlightBuilder {

        public CSVFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                                Map<String, StringTimeSeries> stringTimeSeries) {
            super(meta, doubleTimeSeries, stringTimeSeries);
        }

        private static final List<ProcessStep.Factory> processSteps = List.of();

        // This can be overridden.
        protected List<ProcessStep> gatherSteps(Connection connection) {
            // Add all of our processing steps here...
            // The order doesn't matter; the DependencyGraph will resolve
            // the order in the event that there are dependencies.
            List<ProcessStep> steps = super.gatherSteps(connection);
            processSteps.stream().map(factory -> factory.create(connection, this)).forEach(steps::add);
            return steps;
        }
    }

    /**
     * Determines if the data comes from G5 date recorder.
     * Checks the conditions:
     * If Headers contains serial_number AND no system_id AND no airframe_name AND
     * If the date is in the following format: M/d/yyyy
     *
     * @param headerLines
     * @param firstRow
     * @return true or false
     */
    private static boolean airframeIsG5(List<String> headerLines, String[] firstRow) {
        // G5 recorder has date in the format below
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
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
     * Determines if the supplied metainfo string comes from a scan eagle data recorder.
     *
     * @param metainfo
     * @return
     * @throws FatalFlightFileException
     */
    private static boolean airframeIsScanEagle(String metainfo) throws FatalFlightFileException {

        if (metainfo == null || metainfo.trim().isEmpty())
            return false;

        if (metainfo.charAt(0) != '#' && metainfo.charAt(0) != '{') {
            if (metainfo.startsWith("DID_")) {
                LOG.info("CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");
                return true;
            } else {
                throw new FatalFlightFileException(
                        "First line of the flight file should begin with a '#' and contain flight recorder information.");
            }
        }

        return false;
    }

    /**
     * Extracts three header lines from a csv file.
     *
     * @param bufferedReader
     * @return List of header lines (3 lines expected)
     * @throws IOException
     */
    private static List<String> extractHeaderLines(BufferedReader bufferedReader) throws IOException {
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
     * Determines which file processor should be used for the supplied file and creates it.
     *
     * @param connection
     * @param stream
     * @param filename
     * @param pipeline
     * @return
     */
    public static FlightFileProcessor factory(Connection connection, InputStream stream, String filename, Pipeline pipeline) throws Exception {
        byte[] bytes = stream.readAllBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        Factory _factory = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bis, StandardCharsets.UTF_8))) {
            List<String> headerLines = extractHeaderLines(reader);
            String firstLine = reader.readLine();
            if (firstLine == null)
                throw new IOException("Encountered end of stream prematurely.");

            String[] values = firstLine.split(",");

            if (airframeIsG5(headerLines, values)) {
                _factory = G5CSVFileProcessor::new;
            } else if (airframeIsScanEagle(headerLines.get(0))) {
                _factory = ScanEagleCSVFileProcessor::new;
            } else {
                _factory = CSVFileProcessor::new;
            }
        }

        bis.reset();
        return _factory.create(connection, bis, filename, pipeline);
    }

    protected CSVFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException {

        super(connection, stream, filename, pipeline);

        LOG.info("Creating CSV File Processor for file: " + filename);

        meta.airframe = new Airframes.Airframe("Fixed Wing"); // Fixed Wing By default
        meta.filename = filename;
    }

    void computeMd5Hash() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(super.stream.readAllBytes());
            meta.setMd5Hash(DatatypeConverter.printHexBinary(hash).toLowerCase());
            super.stream.reset();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Parses a CSV file containing flight data, processes the headers, validates rows,
     * and splits the data into multiple flight segments if time gaps exist.
     * For G5 flight recorders, calculates local time and time zone information.
     * Generates flight builders with corresponding time series data and returns them as a stream.
     *
     * @return Stream of FlightBuilder objects representing parsed flights.
     * @throws FlightProcessingException
     */
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        
        LOG.info("Parsing " + this.meta.filename);

        // Ensure we read from the beginning of the stream when we compute the hash, and reset it afterward.
        try {
            stream.reset();
            computeMd5Hash();
            stream.reset();
        } catch (IOException e) {
            // This should never happen since we are using a byte array backed stream.
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<String[]> rows = extractFlightData();

        try {
            readTimeSeries(rows, doubleTimeSeries, stringTimeSeries);
        } catch (FatalFlightFileException ex) {
            throw new FlightProcessingException(ex);
        }

        return Stream.of(
                new CSVFlightBuilder(meta, doubleTimeSeries, stringTimeSeries)
        );
    }

    /**
     * Reads meta data, headers, and the flight data.
     *
     * @return list of rows
     * @throws FlightProcessingException
     */
    List<String[]> extractFlightData() throws FlightProcessingException {
        // Extract headers and rows
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8))) {
            processMetaData(bufferedReader);
            dataTypes = processDataTypes(bufferedReader);
            headers = processHeaders(bufferedReader);

            CSVReader csvReader = new CSVReader(bufferedReader);
            return new ArrayList<>(csvReader.readAll());
        } catch (IOException | CsvException | FatalFlightFileException e) {
            e.printStackTrace();
            throw new FlightProcessingException(e);
        }
    }

    void readTimeSeries(List<String[]> rows, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws FlightProcessingException, FatalFlightFileException {
        ArrayList<ArrayList<String>> columns = new ArrayList<>();
        for (int j = 0; j < headers.size(); j++)
            columns.add(new ArrayList<>());

        int validRows = 0;

        // Populate columns with data from rows
        for (String[] row : rows) {
            if (row.length != headers.size())
                break;
            for (int i = 0; i < row.length; i++)
                columns.get(i).add(row[i]);

                validRows += 1;

            }

            // If we detect an invalid row before the last row of the file, or there are no valid rows.

            //Detected invalid row before the last row of the file (or no valid rows), throw exception
            if (validRows < Math.max(columns.size() - 2, 0))
                throw new FatalFlightFileException("Flight file has 0 valid rows - something serious is wrong with the format.");

            //Ignore flights that are too short (they didn't actually take off)
            final int MINIMUM_FLIGHT_DURATION_SEC = 180;
            if (validRows < MINIMUM_FLIGHT_DURATION_SEC)
                throw new FatalFlightFileException("Flight file was less than 3 minutes long, ignoring.");

            for (int i = 0 ; i < columns.size() ; i++) {

                ArrayList<String> column = columns.get(i);
                String name = headers.get(i);
                String dataType = dataTypes.get(i);

                LOG.info("Name: " + name + "; Data Type: " + dataType);

                //Track whether any non-empty cell was found, and whether it was numeric
                boolean foundNonEmptyValue = false;
                boolean isNumeric = false;
                
                //Use the first usable string encountered for fallback
                String firstUsableValue = null;

                //Scan the column for the first non-blank value
                for (int j = 0 ; j < column.size() ; j++) {

                    String value = column.get(j);

                    //Trim whitespace / possible placeholders
                    if (value == null)
                        value = "";

                    value = value.strip();
                    if (value.isEmpty()
                        || value.equalsIgnoreCase("null")
                        || value.equalsIgnoreCase("NaN")
                        || value.equalsIgnoreCase("N/A")
                        || value.equals("-")) {

                        column.set(j, "");
                        continue;
                    }

                    //Found a non-empty cell
                    foundNonEmptyValue = true;
                    firstUsableValue = value;

                    //Attempt parsing as a double... - 
                    try {

                        String normalized = value.replaceAll("[^\\d.+-Ee]", "");
                        JavaDoubleParser.parseDouble(normalized);  
                        
                        //...Successfully parsed as double, the column is numeric
                        isNumeric = true;

                    } catch (NumberFormatException nfe) {
                        
                        //...Not numeric, treat entire column as string

                    }
                    
                    break;

                }

                //Apply typing to the entire column
                if (isNumeric) {

                    for (int j = 0; j < column.size(); j++) {

                        String v = column.get(j).strip();

                        //Empty cell, insert empty string placeholder
                        if (v.isEmpty()) {
                            column.set(j, "");
                            continue;
                        }

                        String normalized = v.replaceAll("[^\\d.+-Ee]", "");
                        column.set(j, normalized);

                    }

                    doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, column));
                    LOG.info("Interpreted column '" + name + "' as DOUBLE; first usable value: " + firstUsableValue);

                } else {

                    if (!foundNonEmptyValue)
                        LOG.warning("Column '" + name  + "' has no valid (non-blank) values; defaulting to STRING.");

                    stringTimeSeries.put(name, new StringTimeSeries(name, dataType, column));
                    LOG.info("Interpreted column '" + name + "' as STRING; first usable value: " + firstUsableValue);

                }

            }


            for (String name : doubleTimeSeries.keySet()) {
                LOG.info("CSVFilePrcoessor -- Name -> Double Time Series Name: " + name + " = " + doubleTimeSeries.get(name));
            }

            //Update the number of rows in the meta object
            meta.setNumberRows(validRows);
            LOG.info("Number of rows: " + validRows);

            FlightBuilder builder = new CSVFlightBuilder(meta, doubleTimeSeries, stringTimeSeries);

            LOG.info("Returning flight builder!");
            Stream.of(builder);
            
        }

    // /**
    //  * Gets the flight information from the first line of the file
    //  *
    //  * @param reader BufferedReader for reading the first line
    //  * @return The first line of the file
    //  * @throws FatalFlightFileException
    //  * @throws IOException
    //  */
    // private String getFlightInfo(BufferedReader reader) throws FatalFlightFileException, IOException {
    //     String fileInformation = reader.readLine();

    //     if (fileInformation == null || fileInformation.trim().isEmpty()) {
    //         throw new FatalFlightFileException("The flight file was empty.");
    //     }

    //     if (fileInformation.charAt(0) != '#' && fileInformation.charAt(0) != '{') {
    //         if (fileInformation.startsWith("DID_")) {
    //             LOG.info("CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");

    //             meta.airframe = new Airframes.Airframe("ScanEagle");
    //             meta.airframeType = new Airframes.AirframeType("UAS Fixed Wing");
    //         } else {
    //             throw new FatalFlightFileException("First line of the flight file should begin with a '#' and contain"
    //                     + " flight recorder information.");
    //         }
    //     }
    // }


    /**
     * Flight files usually have two lines of meta-data at the top, which are proceeded by pound signs #.
     * An example line looks something like:
     * #system_id=xxx, key1=val1, key2=val2, ...
     * We gather the key:value pairs and pull out any useful information we find, storing the results in a FlightMeta
     * object.
     */
    void processMetaData(BufferedReader reader) throws FatalFlightFileException {
        // Some files have random double quotes in the header for some reason? We can
        // just remove these since we don't consider them anyways.
        String fileInformation;
        try {
            fileInformation = reader.readLine().replace("\"", "");
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- cannot process file.", e);
        }

        LOG.info("Parsing " + fileInformation);
        String[] infoParts = fileInformation.split(",");

        HashMap<String, String> values = new HashMap<>(infoParts.length * 2);
        try {
            for (int i = 1; i < infoParts.length; i++) {
                // process everything else (G1000 data)
                if (infoParts[i].trim().isEmpty())
                    continue;

                String[] subParts = infoParts[i].trim().split("=");
                // May throw index out of bounds.
                values.put(subParts[0].trim(), subParts[1].trim());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
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
                throw new FatalFlightFileException("Flight file contained no system_id -- this file format is likely unsupported at this time.");
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
            }
        }
    }

    List<String> processDataTypes(BufferedReader reader) throws FatalFlightFileException {
        // Note that we skip the first character -- this should be a hash mark.
        try {
            String dataTypesLine = reader.readLine();
            String[] dataTypesArray = dataTypesLine.substring(1).split(",", -1);
            return Arrays.stream(dataTypesArray)
                    .map(String::strip)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- cannot process file.", e);
        }
    }

    List<String> processHeaders(BufferedReader reader) throws FatalFlightFileException {
        try {
            String headerLine = reader.readLine();
            String[] headersArray = headerLine.split(",", -1);
            return Arrays.stream(headersArray)
                    .map(String::strip)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- cannot process file.", e);
        }
    }

    private void setAirframeName(String name) throws FatalFlightFileException {
        var fleetKey = new AliasKey(name, pipeline.getUpload().getFleetId());
        var defaultKey = Airframes.defaultAlias(name);

        if (Airframes.AIRFRAME_ALIASES.containsKey(fleetKey)) {
            meta.airframe = new Airframes.Airframe(Airframes.AIRFRAME_ALIASES.get(fleetKey));
        } else {
            meta.airframe = new Airframes.Airframe(Airframes.AIRFRAME_ALIASES.getOrDefault(defaultKey, name));
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


}
