package org.ngafid.flights.process;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    public CSVFileProcessor(InputStream stream, String filename) {
        super(stream, filename);
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
            String fileInformation = getFileInformation(bufferedReader); // Will read a line
            updateAirframe();

            if (airframeName != null && airframeName.equals("ScanEagle")) {
                scanEagleParsing(fileInformation); // TODO: Handle ScanEagle data
            } else {
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

        return Stream.of(new FlightBuilder[]{builder});
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

    private String getFileInformation(BufferedReader reader) throws FatalFlightFileException, IOException {
        String fileInformation = reader.readLine();

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


    private void scanEagleParsing(String fileInformation) {

        //need a custom method to process ScanEagle data because the column
        //names are different and there is no header info
        scanEagleSetTailAndID();
        scanEagleHeaders(fileInformation);
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
