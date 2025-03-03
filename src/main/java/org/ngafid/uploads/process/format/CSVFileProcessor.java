package org.ngafid.uploads.process.format;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.ngafid.common.MD5;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.Airframes.AliasKey;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.FlightDataRecorder;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.FlightMeta;
import org.ngafid.uploads.process.FlightProcessingException;
import org.ngafid.uploads.process.Pipeline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses CSV files into Double and String time series, and returns a stream of flight builders
 * <p>
 * There are a lot of flight data recorders that output CSV data, the we are primarily concerned with the ones produced
 * by Garmin. The G1000 is the "default" and the G3X and G5 are the exceptions, which are handled by the factory method.
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

    private static final Pattern G5_PART_NUMBER_REGEX = Pattern.compile("006-B2304-\\d\\d");

    /**
     * Scans for Garmin-G5 part number in header. We might in the future use part numbers to identify other file formats.
     * <p>
     * According to the following document: https://static.garmin.com/pumac/190-01112-10_28.pdf?download=true
     * The part number for the G5 is 006-B2304-XX where XX represent the specific version.
     * <p>
     * Determines if the data comes from G5 date recorder.
     * Checks the conditions:
     * If Headers contains serial_number AND no system_id AND no airframe_name AND
     * If the date is in the following format: M/d/yyyy
     *
     * @param headerLines
     * @return true or false
     */
    private static boolean airframeIsG5(List<String> headerLines) {
        // G5 recorder has date in the format below
        return G5_PART_NUMBER_REGEX.asPredicate().test(headerLines.get(0));
    }

    private static final Pattern G3X_PART_NUMBER_REGEX = Pattern.compile("006-B1727-[A-Za-z\\d]{2}");

    /**
     * Scans first line of file for G3X part number.
     *
     * @param headerLines
     * @return
     */
    private static boolean airframeIsG3X(List<String> headerLines) {
        return G3X_PART_NUMBER_REGEX.asPredicate().test(headerLines.get(0));
    }

    /**
     * Determines if the supplied metadata string comes from a scan eagle data recorder.
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
                throw new IOException("Encountered end of stream prematurely in file " + filename);

            String[] values = firstLine.split(",");

            if (airframeIsG5(headerLines) || airframeIsG3X(headerLines)) {
                LOG.info("Creating G5 CSV file processor");
                _factory = G5CSVFileProcessor::new;
            } else if (airframeIsScanEagle(headerLines.get(0))) {
                _factory = ScanEagleCSVFileProcessor::new;
            } else {
                LOG.info("Creating normal CSV file processor");
                _factory = CSVFileProcessor::new;
            }
        }

        bis.reset();
        return _factory.create(connection, bis, filename, pipeline);
    }

    protected CSVFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException {
        super(connection, stream, filename, pipeline);

        meta.filename = filename;
    }

    /**
     * Parses a CSV file containing flight data, processes the headers, validates rows,
     * and splits the data into multiple flight segments if time gaps exist.
     * For G5 flight recorders, calculates local time and time zone information.
     * Generates flight builders with corresponding time series data and returns them as a stream.
     *
     * @throws FlightProcessingException
     * @rreturn Stream of FlightBuilder objects representing parsed flights.
     */
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        LOG.info("Parsing " + this.meta.filename);

        // Ensure we read from the beginning of the stream when we compute the hash, and reset it afterward.
        try {
            stream.reset();
            meta.setMd5Hash(MD5.computeHexHash(super.stream));
            stream.reset();
        } catch (IOException e) {
            // This should never happen since we are using a byte array backed stream.
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<String[]> rows = extractFlightData();

        readTimeSeries(rows, doubleTimeSeries, stringTimeSeries);

        return Stream.of(
                makeFlightBuilder(meta, doubleTimeSeries, stringTimeSeries)
        );
    }

    FlightBuilder makeFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleSeries, Map<String, StringTimeSeries> stringSeries) {
        return new FlightBuilder(meta, doubleSeries, stringSeries);
    }

    /**
     * Reads metadata, headers, and the flight data.
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

    /**
     * Separates the rows from the CSV reader into separate columns and infers their data type (double or string).
     * These columns are placed into the supplied time series maps.
     *
     * @param rows             Rows of the CSV file
     * @param doubleTimeSeries map to place the double columns into
     * @param stringTimeSeries map to place the string columns into
     * @throws FlightProcessingException if there are not enough valid rows
     */
    void readTimeSeries(List<String[]> rows, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws FlightProcessingException {
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

        if (validRows <= Math.max(rows.size() - 2, 0)) {
            throw new FlightProcessingException(
                    new FatalFlightFileException(
                            "Flight has 0 rows, or consecutive malformed rows -- " +
                                    "there is a serious problem with the file format."
                    )
            );
        }

        // Populate Time Series
        for (int j = 0; j < columns.size(); j++) {
            var columnData = columns.get(j);
            var name = headers.get(j);
            var dataType = dataTypes.get(j);

            try {
                JavaDoubleParser.parseDouble(columnData.get(columnData.size() / 2).trim());  // Check if the column is numeric
                doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, columnData));
            } catch (NumberFormatException e) {
                stringTimeSeries.put(name, new StringTimeSeries(name, dataType, columnData));
            }
        }
    }

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
            values.put("airframe_name", "Unknown");
            LOG.severe("!!! TESTING ONLY: Log: airframe_name is missing, setting to DummyAirframe - Cessna 172S.");
        }

        meta.flightDataRecorder = new FlightDataRecorder("G1000");

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
        // Note that we skip the first character -- this should be a hash mark for garmin products.
        try {
            return splitCommaSeparated(reader.readLine().substring(1));
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- cannot process file.", e);
        }
    }

    List<String> processHeaders(BufferedReader reader) throws FatalFlightFileException {
        try {
            return splitCommaSeparated(reader.readLine());
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- cannot process file.", e);
        }
    }

    private static List<String> splitCommaSeparated(String line) {
        return Arrays.stream(line.split(",", -1))
                .map(String::strip)
                .collect(Collectors.toList());
    }

    private void setAirframeName(String name) throws FatalFlightFileException {
        var fleetKey = new AliasKey(name, pipeline.getUpload().getFleetId());
        var defaultKey = Airframes.defaultAlias(name);

        String airframeName = null;
        if (Airframes.AIRFRAME_ALIASES.containsKey(fleetKey)) {
            airframeName = Airframes.AIRFRAME_ALIASES.get(fleetKey);
        } else {
            airframeName = Airframes.AIRFRAME_ALIASES.getOrDefault(defaultKey, name);
        }

        String airframeType = null;
        if (Airframes.FIXED_WING_AIRFRAMES.contains(airframeName)
                || airframeName.contains("Garmin")) {
            airframeType = "Fixed Wing";
        } else if (Airframes.ROTORCRAFT.contains(airframeName)) {
            airframeType = "Rotorcraft";
        } else {
            LOG.severe("Could not import flight because the aircraft type was unknown for the following airframe name: '"
                    + airframeName + "'");
            LOG.severe(
                    "Please add this to the the `airframe_type` table in the database and update this method.");
            throw new FatalFlightFileException("Unsupported airframe type '" + name + "'");
        }

        meta.airframe = new Airframes.Airframe(airframeName, new Airframes.Type(airframeType));
    }


}
