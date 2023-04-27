package org.ngafid.flights.process;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Handles parsing of CSV files
 *
 * @author Aaron Chan
 */

public class CSVFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(CSVFileProcessor.class.getName());
    private String airframeName;
    private String startDateTime;
    private String endDateTime;
    private String airframeType;
    private String suggestedTailNumber;
    private String systemId;
    private final List<String> headers;
    private final List<String> dataTypes;

    private final Upload upload;

    public CSVFileProcessor(Connection connection, InputStream stream, String filename, Upload upload) {
        super(connection, stream, filename);
        this.upload = upload;

        headers = new ArrayList<>();
        dataTypes = new ArrayList<>();

        this.airframeType = "Fixed Wing"; // Fixed Wing By default
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<String[]> csvValues = null;
        List<String> dataTypes = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8)); CSVReader csvReader = new CSVReader(bufferedReader)) {
            String fileInformation = getFlightInfo(bufferedReader); // Will read a line
            updateAirframe();

            if (airframeName != null && airframeName.equals("ScanEagle")) {
                scanEagleParsing(fileInformation); // TODO: Handle ScanEagle data
            } else {
                processFileInormation(fileInformation);
                bufferedReader.read(); // Skip first char (#)
                dataTypes = List.of(csvReader.readNext());
                headers = List.of(csvReader.readNext());
                csvValues = csvReader.readAll();
            }

            int colIndex = 0;
            String[] firstRow = csvValues.get(0);
            for (String data : firstRow) {
                try {
                    Double.parseDouble(data);
                    doubleTimeSeries.put(headers.get(colIndex), new DoubleTimeSeries(headers.get(colIndex), dataTypes.get(colIndex)));
                } catch (NumberFormatException e) {
                    stringTimeSeries.put(headers.get(colIndex), new StringTimeSeries(headers.get(colIndex), dataTypes.get(colIndex)));
                }

                colIndex++;
            }

            List<String> finalHeaders = headers;
            csvValues.forEach(row -> {
                for (int i = 0; i < row.length; i++) {
                    String header = finalHeaders.get(i);
                    String value = row[i];

                    try {
                        doubleTimeSeries.get(header).add(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        stringTimeSeries.get(header).add(value);
                    }
                }
            });


        } catch (IOException | FatalFlightFileException | CsvException e) {
            throw new FlightProcessingException(e);
        }

        FlightBuilder builder = new FlightBuilder(new FlightMeta(), doubleTimeSeries, stringTimeSeries);

        return Stream.of(builder);
    }


    /**
     * Updates the airframe type if airframe name does not belong to fixed wing
     */
    private void updateAirframe() {
        if (airframeName.equals("R44") || airframeName.equals("Robinson R44")) {
            airframeName = "R44";
            airframeType = "Rotorcraft";
        }
    }

    /**
     * Gets the flight information from the first line of the file
     * @param reader BufferedReader for reading the first line
     * @return
     * @throws FatalFlightFileException
     * @throws IOException
     */
    private String getFlightInfo(BufferedReader reader) throws FatalFlightFileException, IOException {
        String fileInformation = reader.readLine();
        System.out.println("File information = " + fileInformation);

        if (fileInformation == null || fileInformation.trim().length() == 0) {
            throw new FatalFlightFileException("The flight file was empty.");
        }

        if (fileInformation.charAt(0) != '#' && fileInformation.charAt(0) != '{') {
            if (fileInformation.startsWith("DID_")) {
                LOG.info("CAME FROM A SCANEAGLE! CAN CALCULATE SUGGESTED TAIL/SYSTEM ID FROM FILENAME");

                this.airframeName = "ScanEagle";
                this.airframeType = "UAS Fixed Wing";
            } else {
                throw new FatalFlightFileException("First line of the flight file should begin with a '#' and contain flight recorder information.");
            }
        }

        return fileInformation;
    }

    private void processFileInormation(String fileInformation) throws FatalFlightFileException {
        String[] infoParts = fileInformation.split(",");
        try {
            for (int i = 1; i < infoParts.length; i++) {
                //process everything else (G1000 data)
                if (infoParts[i].trim().length() == 0) continue;

                //System.err.println("splitting key/value: '" + infoParts[i] + "'");
                String subParts[] = infoParts[i].trim().split("=");
                String key = subParts[0];
                String value = subParts[1];

                //System.err.println("key: '" + key + "'");
                //System.err.println("value: '" + value + "'");

                // TODO: Create some sort of automatic mapping for synonomous airframe names.
                if (key.equals("airframe_name")) {
                    airframeName = value.substring(1, value.length() - 1);

                    //throw an error for 'Unknown Aircraft'
                    if (airframeName.equals("Unknown Aircraft")) {
                        throw new FatalFlightFileException("Flight airframe name was 'Unknown Aircraft', please fix and re-upload so the flight can be properly identified and processed.");
                    }


                    if (airframeName.equals("Diamond DA 40")) {
                        airframeName = "Diamond DA40";
                    } else if ((airframeName.equals("Garmin Flight Display") || airframeName.equals("Robinson R44 Raven I")) && upload.getFleetId() == 1 /*This is a hack for UND who has their airframe names set up incorrectly for their helicopters*/) {
                        airframeName = "R44";
                    } else if (airframeName.equals("Garmin Flight Display")) {
                        throw new FatalFlightFileException("Flight airframe name was 'Garmin Flight Display' which does not specify what airframe type the flight was, please fix and re-upload so the flight can be properly identified and processed.");

                    }

                    if (airframeName.equals("Cirrus SR22 (3600 GW)")) {
                        airframeName = "Cirrus SR22";
                    }

                    if (Airframes.FIXED_WING_AIRFRAMES.contains(airframeName) || airframeName.contains("Garmin")) {
                        airframeType = "Fixed Wing";
                    } else if (airframeName.equals("R44") || airframeName.equals("Robinson R44")) {
                        airframeName = "R44";
                        airframeType = "Rotorcraft";
                    } else {
                        System.err.println("Could not import flight because the aircraft type was unknown for the following airframe name: '" + airframeName + "'");
                        System.err.println("Please add this to the the `airframe_type` table in the database and update this method.");
                        System.exit(1);
                    }

                } else if (key.equals("system_id")) {
                    systemId = value.substring(1, value.length() - 1);
                }
            }
        } catch (Exception e) {
            //LOG.info("parsting flight information threw exception: " + e);
            //e.printStackTrace();
            throw new FatalFlightFileException("Flight information line was not properly formed with key value pairs.", e);
        } 
    }


    /**
     * Parses for ScanEagle flight data
     * @param fileInformation First line of the file
     */
    private void scanEagleParsing(String fileInformation) {

        //need a custom method to process ScanEagle data because the column
        //names are different and there is no header info
        scanEagleSetTailAndID();
        scanEagleHeaders(fileInformation);
    }

    /**
     * Handles setting the tail number and system id for ScanEagle data
     */
    private void scanEagleSetTailAndID() {
        String[] filenameParts = filename.split("_");
        startDateTime = filenameParts[0];
        endDateTime = startDateTime;
        LOG.log(Level.INFO, "start date: '{0}'", startDateTime);
        LOG.log(Level.INFO, "end date: '{0}'", startDateTime);

        //UND doesn't have the systemId for UAS anywhere in the filename or file (sigh)
        suggestedTailNumber = "N" + filenameParts[1] + "ND";
        systemId = suggestedTailNumber;

        LOG.log(Level.INFO, "suggested tail number: '{0}'", suggestedTailNumber);
        LOG.log(Level.INFO, "system id: '{0}'", systemId);
    }


    // TODO: Figure out ScanEagle data
    private void scanEagleHeaders(String fileInformation) {
        String headersLine = fileInformation;
        headers.addAll(Arrays.asList(headersLine.split("\\,", -1)));
        headers.replaceAll(String::trim);
        System.out.println("headers are:\n" + headers.toString());
        //scan eagle files have no data types, set all to ""
        for (int i = 0; i < headers.size(); i++) {
            dataTypes.add("none");
        }
    }
}
