package org.ngafid.flights.process;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.Airframes.AliasKey;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.StringTimeSeries;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parses CSV files into Double and String time series, and returns a stream of flight builders.
 *
 * @author Aaron Chan
 * @author Joshua Karns
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

    private CSVFileProcessor(Connection connection, byte[] bytes, String filename, Pipeline pipeline) {
        super(connection, new ByteArrayInputStream(bytes), filename, pipeline);

        headers = new ArrayList<>();
        dataTypes = new ArrayList<>();

        meta.airframe = new Airframes.Airframe("Fixed Wing"); // Fixed Wing By default
        meta.filename = filename;
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        LOG.info("Parsing " + this.meta.filename);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(super.stream.readAllBytes());
            meta.setMd5Hash(DatatypeConverter.printHexBinary(hash).toLowerCase());

            super.stream.reset();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream,
                StandardCharsets.UTF_8)); CSVReader csvReader = new CSVReader(bufferedReader)) {
            String fileInformation = getFlightInfo(bufferedReader); // Will read a line

            if (meta.airframe != null && meta.airframe.getName().equals("ScanEagle")) {
                scanEagleParsing(fileInformation); // TODO: Handle ScanEagle data
            } else {
                processFileInormation(fileInformation);
                bufferedReader.read(); // Skip first char (#)
                Arrays.stream(csvReader.readNext()).map(String::strip).forEachOrdered(dataTypes::add);
                Arrays.stream(csvReader.readNext()).map(String::strip).forEachOrdered(headers::add);
            }

            ArrayList<ArrayList<String>> columns = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++)
                columns.add(new ArrayList<>());

            // Documentation of CSVReader claims this is a linked list,
            // so it is important to iterate over it rather than using indexing
            List<String[]> rows = csvReader.readAll();
            int validRows = 0;
            for (String[] row : rows) {
                // Encountered a row that is broken for some reason?
                if (row.length != headers.size()) break;
                for (int i = 0; i < row.length; i++)
                    columns.get(i).add(row[i]);
                validRows += 1;
            }

            // If we detect an invalid row before the last row of the file, or there are no valid rows.
            if (validRows < Math.max(columns.size() - 2, 0)) {
                throw new FatalFlightFileException("Flight file has 0 valid rows - something serious is wrong with " +
                        "the format.");
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

            for (String name : doubleTimeSeries.keySet()) {
                LOG.info("name = " + name + "; = " + doubleTimeSeries.get(name));
            }

        } catch (IOException | FatalFlightFileException | CsvException e) {
            e.printStackTrace();
            throw new FlightProcessingException(e);
        }

        FlightBuilder builder = new CSVFlightBuilder(meta, doubleTimeSeries, stringTimeSeries);

        LOG.info("Returning flight builder!");
        return Stream.of(builder);
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

        if (fileInformation == null || fileInformation.trim().isEmpty()) {
            throw new FatalFlightFileException("The flight file was empty.");
        }

        if (fileInformation.charAt(0) != '#' && fileInformation.charAt(0) != '{') {
            if (fileInformation.startsWith("DID_")) {
                LOG.info("CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");

                meta.airframe = new Airframes.Airframe("ScanEagle");
                meta.airframeType = new Airframes.AirframeType("UAS Fixed Wing");
            } else {
                throw new FatalFlightFileException("First line of the flight file should begin with a '#' and contain"
                        + " flight recorder information.");
            }
        }

        return fileInformation;
    }

    /**
     * Flight files usually have two lines of meta data at the top, which are preceeded by pound signs #.
     * An example line looks something like:
     * <p>
     * #system_id=xxx, key1=val1, key2=val2, ...
     * <p>
     * We gather the key:value pairs and pull out any useful information we find, storing the results in a FlightMeta
     * object.
     *
     * @param fileInformation The first line of the file
     */
    private void processFileInormation(String fileInformation) throws FatalFlightFileException {
        // Some files have random double quotes in the header for some reason? We can
        // just remove these since we don't consider them anyways.
        fileInformation = fileInformation.replace("\"", "");
        String[] infoParts = fileInformation.split(",");

        HashMap<String, String> values = new HashMap<>(infoParts.length * 2);
        try {
            for (int i = 1; i < infoParts.length; i++) {
                // process everything else (G1000 data)
                if (infoParts[i].trim().isEmpty()) continue;

                String[] subParts = infoParts[i].trim().split("=");

                // May throw index out of bounds.
                values.put(subParts[0].trim(), subParts[1].trim());
            }
        } catch (IndexOutOfBoundsException e) {
            // LOG.info("parsting flight information threw exception: " + e);
            // e.printStackTrace();
            throw new FatalFlightFileException("Flight information line was not properly formed with key value pairs" +
                    ".", e);
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
        } else meta.airframe = new Airframes.Airframe(Airframes.AIRFRAME_ALIASES.getOrDefault(defaultKey, name));

        if (Airframes.FIXED_WING_AIRFRAMES.contains(meta.airframe.getName()) || meta.airframe.getName().contains(
                "Garmin")) {
            meta.airframeType = new Airframes.AirframeType("Fixed Wing");
        } else if (Airframes.ROTORCRAFT.contains(meta.airframe.getName())) {
            meta.airframeType = new Airframes.AirframeType("Rotorcraft");
        } else {
            LOG.severe("Could not import flight because the aircraft type was unknown for the following airframe " +
                    "name: '" + meta.airframe.getName() + "'");
            LOG.severe("Please add this to the the `airframe_type` table in the database and update this method.");
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
