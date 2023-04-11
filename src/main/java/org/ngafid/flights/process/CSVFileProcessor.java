package org.ngafid.flights.process;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.maven.cli.logging.Slf4jLogger;
import org.ngafid.flights.*;

import java.io.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class CSVFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(CSVFileProcessor.class.getName());
    private String airframeName;
    private String startDateTime;
    private String endDateTime;
    private String filename;
    private String airframeType;
    private String suggestedTailNumber;
    private String systemId;


    public CSVFileProcessor(InputStream stream, String filename) {
        super(stream, filename);
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();


        FlightBuilder builder = new FlightBuilder(new FlightMeta(), null, null);
    }


    private void init() throws FatalFlightFileException {
        ArrayList<ArrayList<String>> csvValues = null;

        List<String> dataTypes = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8))) {
            String fileInformation = getFileInformation(bufferedReader);

            if (airframeName != null && airframeName.equals("ScanEagle")) {
                scanEagleParsing();

            } else {
                //grab the airframe info from the header for other file types
                String[] infoParts = null;
                infoParts = fileInformation.split(",");
                airframeName = null;

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

                        if (key.equals("airframe_name")) {
                            airframeName = value.substring(1, value.length() - 1);

                            //throw an error for 'Unknown Aircraft'
                            if (airframeName.equals("Unknown Aircraft")) {
                                throw new FatalFlightFileException("Flight airframe name was 'Unknown Aircraft', please fix and re-upload so the flight can be properly identified and processed.");
                            }


                            if (airframeName.equals("Diamond DA 40")) {
                                airframeName = "Diamond DA40";

                            } else if ((airframeName.equals("Garmin Flight Display") || airframeName.equals("Robinson R44 Raven I")) && fleetId == 1 /*This is a hack for UND who has their airframe names set up incorrectly for their helicopters*/) {
                                airframeName = "R44";
                            } else if (airframeName.equals("Garmin Flight Display")) {
                                throw new FatalFlightFileException("Flight airframe name was 'Garmin Flight Display' which does not specify what airframe type the flight was, please fix and re-upload so the flight can be properly identified and processed.");

                            }

                            if (airframeName.equals("Cirrus SR22 (3600 GW)")) {
                                airframeName = "Cirrus SR22";
                            }

                            if (airframeName.equals("Cessna 172R") ||
                                    airframeName.equals("Cessna 172S") ||
                                    airframeName.equals("Cessna 172T") ||
                                    airframeName.equals("Cessna 182T") ||
                                    airframeName.equals("Cessna T182T") ||
                                    airframeName.equals("Cessna Model 525") ||
                                    airframeName.equals("Cirrus SR20") ||
                                    airframeName.equals("Cirrus SR22") ||
                                    airframeName.equals("Diamond DA40") ||
                                    airframeName.equals("Diamond DA 40 F") ||
                                    airframeName.equals("Diamond DA40NG") ||
                                    airframeName.equals("Diamond DA42NG") ||
                                    airframeName.equals("PA-28-181") ||
                                    airframeName.equals("PA-44-180") ||
                                    airframeName.equals("Piper PA-46-500TP Meridian") ||
                                    airframeName.contains("Garmin") ||
                                    airframeName.equals("Quest Kodiak 100") ||
                                    airframeName.equals("Cessna 400") ||
                                    airframeName.equals("Beechcraft A36/G36") ||
                                    airframeName.equals("Beechcraft G58")) {
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

            if (airframeName == null)
                throw new FatalFlightFileException("Flight information (first line of flight file) does not contain an 'airframe_name' key/value pair.");
            System.err.println("detected airframe type: '" + airframeName + "'");

            if (systemId == null)
                throw new FatalFlightFileException("Flight information (first line of flight file) does not contain an 'system_id' key/value pair.");
            System.err.println("detected airframe type: '" + systemId + "'");

            if (airframeName.equals("ScanEagle")) {
                //for the ScanEagle, the first line is the headers of the columns
                String headersLine = fileInformation;
                //System.out.println("Headers line is: " + headersLine);
                headers.addAll(Arrays.asList(headersLine.split("\\,", -1)));
                headers.replaceAll(String::trim);
                System.out.println("headers are:\n" + headers.toString());

                //scan eagle files have no data types, set all to ""
                for (int i = 0; i < headers.size(); i++) {
                    dataTypes.add("none");
                }

            } else {
                //the next line is the column data types
                String dataTypesLine = bufferedReader.readLine();
                if (dataTypesLine.length() == 0)
                    dataTypesLine = bufferedReader.readLine(); //handle windows files with carriage returns

                if (dataTypesLine.charAt(0) != '#')
                    throw new FatalFlightFileException("Second line of the flight file should begin with a '#' and contain column data types.");
                dataTypesLine = dataTypesLine.substring(1);

                dataTypes.addAll(Arrays.asList(dataTypesLine.split("\\,", -1)));
                dataTypes.replaceAll(String::trim);

                //the next line is the column headers
                String headersLine = bufferedReader.readLine();
                if (headersLine.length() == 0)
                    headersLine = bufferedReader.readLine(); //handle windows files with carriage returns

                System.out.println("Headers line is: " + headersLine);
                headers.addAll(Arrays.asList(headersLine.split("\\,", -1)));
                headers.replaceAll(String::trim);

                //if (airframeName.equals("Cessna T182T")) System.exit(1);

                if (dataTypes.size() != headers.size()) {
                    throw new FatalFlightFileException("Number of columns in the header line (" + headers.size() + ") != number of columns in the dataTypes line (" + dataTypes.size() + ")");
                }
            }

            //initialize a sub-ArrayList for each column
            if (csvValues == null) {
                csvValues = new ArrayList<ArrayList<String>>();
            }

            for (int i = 0; i < headers.size(); i++) {
                csvValues.add(new ArrayList<String>());
            }

            int lineNumber = 1;
            boolean lastLineWarning = false;

            String line;
            String lastWarning = "";
            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() == 0) {
                    line = bufferedReader.readLine(); //handle windows files with carriage returns
                    if (line == null) break;
                }


                if (line.contains("Lcl Time")) {
                    System.out.println("SKIPPING line[" + lineNumber + "]: " + line + " (THIS SHOULD NOT HAPPEN)");
                    continue;
                }

                //if the line is empty, skip it
                if (line.trim().length() == 0) continue;
                //this line is a comment, skip it
                if (line.charAt(0) == '#') continue;

                //split up the values by commas into our array of strings
                String[] values = line.split("\\,", -1);

                if (lastLineWarning) {
                    if (values.length != headers.size()) {
                        String newWarning = "ERROR: line " + lineNumber + " had a different number of values (" + values.length + ") than the number of columns in the file (" + headers.size() + ").";
                        System.err.println(newWarning);
                        System.err.println("ERROR: Two line errors in a row means the flight file is corrupt.");
                        lastLineWarning = true;

                        String errorMessage = "Multiple lines the flight file had extra or missing values, which means the flight file is corrupt:\n";
                        errorMessage += lastWarning + "\n";
                        errorMessage += newWarning;

                        throw new FatalFlightFileException(errorMessage);
                    } else {
                        throw new FatalFlightFileException("A line in the middle of the flight file was missing values, which means the flight file is corrupt:\n" + lastWarning);
                    }
                } else {
                    if (values.length != headers.size()) {
                        lastWarning = "WARNING: line " + lineNumber + " had a different number of values (" + values.length + ") than the number of columns in the file. Not an error if it was the last line in the file.";
                        System.err.println(lastWarning);
                        lastLineWarning = true;
                        continue;
                    }
                }

                //for each CSV value
                for (int i = 0; i < values.length; i++) {
                    //add this to the respective column in the csvValues ArrayList, trimming the whitespace around it
                    csvValues.get(i).add(values[i].trim());
                }

                lineNumber++;
                numberRows++;
            }

            //ignore flights that are too short (they didn't actually take off)
            if (numberRows < 180)
                throw new FatalFlightFileException("Flight file was less than 3 minutes long, ignoring.");


            if (lastLineWarning) {
                System.err.println("WARNING: last line of the file was cut short and ignored.");
            }

            System.out.println("parsed " + lineNumber + " lines.");

            for (int i = 0; i < csvValues.size(); i++) {
                //check to see if each column is a column of doubles or a column of strings

                //for each column, find the first non empty value and check to see if it is a double
                boolean isDoubleList = false;
                ArrayList<String> current = csvValues.get(i);

                for (int j = 0; j < current.size(); j++) {
                    String currentValue = current.get(j);
                    if (currentValue.length() > 0) {
                        try {
                            Double.parseDouble(currentValue);
                            isDoubleList = true;
                        } catch (NumberFormatException e) {
                            isDoubleList = false;
                            break;
                        }
                    }
                }

                if (isDoubleList) {
                    //System.out.println(headers.get(i) + " is a DOUBLE column, ArrayList size: " + current.size());
                    //System.out.println(current);
                    DoubleTimeSeries dts = new DoubleTimeSeries(connection, headers.get(i), dataTypes.get(i), current);
                    if (dts.validCount() > 0) {
                        doubleTimeSeries.put(headers.get(i), dts);
                    } else {
                        System.err.println("WARNING: dropping double column '" + headers.get(i) + "' because all entries were empty.");
                    }

                } else {
                    //System.out.println(headers.get(i) + " is a STRING column, ArrayList size: " + current.size());
                    //System.out.println(current);
                    StringTimeSeries sts = new StringTimeSeries(connection, headers.get(i), dataTypes.get(i), current);
                    if (sts.validCount() > 0) {
                        stringTimeSeries.put(headers.get(i), sts);
                    } else {
                        System.err.println("WARNING: dropping string column '" + headers.get(i) + "' because all entries were empty.");
                    }
                }
            }
        } catch (IOException e) {
            throw new FatalFlightFileException("Error reading flight file: " + e.getMessage());
        }


    }

    private String getFileInformation(BufferedReader reader) throws FatalFlightFileException, IOException {
        String fileInformation = reader.readLine();

        if (fileInformation == null || fileInformation.trim().length() == 0)
            throw new FatalFlightFileException("The flight file was empty.");
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


    private void scanEagleParsing() {

        //need a custom method to process ScanEagle data because the column
        //names are different and there is no header info


    }

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

}
