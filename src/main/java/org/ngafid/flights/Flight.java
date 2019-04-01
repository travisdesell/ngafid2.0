package org.ngafid.flights;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.ngafid.common.MutableDouble;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.terrain.TerrainCache;

import org.ngafid.filters.Filter;

public class Flight {
    private static final Logger LOG = Logger.getLogger(Flight.class.getName());

    private final static double MAX_AIRPORT_DISTANCE_FT = 10000;
    private final static double MAX_RUNWAY_DISTANCE_FT = 100;

    private int id = -1;
    private int fleetId = -1;
    private int uploaderId = -1;
    private int uploadId = -1;

    private String filename;
    private String airframeType;
    private String tailNumber;
    private String md5Hash;
    private String startDateTime;
    private String endDateTime;

    //these will be set to true if the flight has
    //latitude/longitude coordinates and can therefore
    //calculate AGL, airport and runway proximity
    //hasAGL also requires an altitudeMSL column
    private boolean hasCoords = false;
    private boolean hasAGL = false;
    private boolean insertCompleted = false;

    private String status;
    private ArrayList<MalformedFlightFileException> exceptions = new ArrayList<MalformedFlightFileException>();

    private int numberRows;
    private String fileInformation;
    private ArrayList<String> dataTypes;
    private ArrayList<String> headers;

    private HashMap<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<String, DoubleTimeSeries>();
    private HashMap<String, StringTimeSeries> stringTimeSeries = new HashMap<String, StringTimeSeries>();

    private ArrayList<Itinerary> itinerary = new ArrayList<Itinerary>();


    public static ArrayList<Flight> getFlightsFromUpload(Connection connection, int uploadId) throws SQLException {
        String queryString = "SELECT id, fleet_id, uploader_id, upload_id, tail_id, airframe_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed FROM flights WHERE upload_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, uploadId);

        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }

    public void remove(Connection connection) throws SQLException {
        String query = "DELETE FROM events WHERE flight_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flight_warnings WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flight_processed WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM itinerary WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM double_series WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM string_series WHERE flight_id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();

        query = "DELETE FROM flights WHERE id = ?";
        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, this.id);
        LOG.info(preparedStatement.toString());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }



    public static ArrayList<Flight> getFlights(Connection connection, int fleetId) throws SQLException {
        return getFlights(connection, fleetId, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, int limit) throws SQLException {
        String queryString = "SELECT id, fleet_id, uploader_id, upload_id, tail_id, airframe_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed FROM flights WHERE fleet_id = ?";
        if (limit > 0) queryString += " LIMIT 100";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();
        return flights;
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter) throws SQLException {
        return getFlights(connection, fleetId, filter, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, int fleetId, Filter filter, int limit) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT id, fleet_id, uploader_id, upload_id, tail_id, airframe_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed FROM flights WHERE fleet_id = ? AND (" + filter.toQueryString(fleetId, parameters) + ")";

        if (limit > 0) queryString += " LIMIT 100";

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, fleetId);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 2, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 2, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 2, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition) throws SQLException {
        return getFlights(connection, extraCondition, 0);
    }

    public static ArrayList<Flight> getFlights(Connection connection, String extraCondition, int limit) throws SQLException {
        ArrayList<Object> parameters = new ArrayList<Object>();

        String queryString = "SELECT id, fleet_id, uploader_id, upload_id, tail_id, airframe_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed FROM flights WHERE (" + extraCondition + ")";

        if (limit > 0) queryString += " LIMIT 100";

        LOG.info(queryString);

        PreparedStatement query = connection.prepareStatement(queryString);
        for (int i = 0; i < parameters.size(); i++) {
            LOG.info("setting query parameter " + i + ": " + parameters.get(i));

            if (parameters.get(i) instanceof String) {
                query.setString(i + 1, (String)parameters.get(i));
            } else if (parameters.get(i) instanceof Double) {
                query.setDouble(i + 1, (Double)parameters.get(i));
            } else if (parameters.get(i) instanceof Integer) {
                query.setInt(i + 1, (Integer)parameters.get(i));
            }
        }

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<Flight> flights = new ArrayList<Flight>();
        while (resultSet.next()) {
            flights.add(new Flight(connection, resultSet));
        }

        resultSet.close();
        query.close();

        return flights;
    }


    // Added to use in pitch_db
    public static Flight getFlight(Connection connection, int flightId) throws SQLException {
        String queryString = "SELECT id, fleet_id, uploader_id, upload_id, tail_id, airframe_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed FROM flights WHERE id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, flightId);

        ResultSet resultSet = query.executeQuery();

        resultSet.close();
        query.close();

        if (resultSet.next()) {
            return new Flight(connection, resultSet);
        } else {
            return null;
        } 
    }
    ////

    public Flight(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        fleetId = resultSet.getInt(2);
        uploaderId = resultSet.getInt(3);
        uploadId = resultSet.getInt(4);

        int tailId = resultSet.getInt(5);
        int airframeId = resultSet.getInt(6); 

        tailNumber = Tails.getTail(connection, fleetId, tailId);
        airframeType = Airframes.getAirframe(connection, airframeId);

        startDateTime = resultSet.getString(7);
        endDateTime = resultSet.getString(8);
        filename = resultSet.getString(9);
        md5Hash = resultSet.getString(10);
        numberRows = resultSet.getInt(11);
        status = resultSet.getString(12);
        hasCoords = resultSet.getBoolean(13);
        hasAGL = resultSet.getBoolean(14);
        insertCompleted = resultSet.getBoolean(15);

        itinerary = Itinerary.getItinerary(connection, id);
    }

    public int getId() {
        return id;
    }

    public int getFleetId() {
        return fleetId;
    }


    public String getFilename() {
        return filename;
    }

    public int getNumberRows() {
        return numberRows;
    }

    public String getStatus() {
        return status;
    }

    public boolean insertCompleted() {
        return insertCompleted;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public DoubleTimeSeries getDoubleTimeSeries(String name) {
        return doubleTimeSeries.get(name);
    }

    public StringTimeSeries getStringTimeSeries(String name) {
        return stringTimeSeries.get(name);
    }

    private void setMD5Hash(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(inputStream.readAllBytes());
            md5Hash = DatatypeConverter.printHexBinary(hash).toLowerCase();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        //System.err.println("MD5 HASH: '" + md5Hash + "'");
    }

    public void calculateStartEndTime(String dateColumnName, String timeColumnName) throws MalformedFlightFileException {
        StringTimeSeries dates = stringTimeSeries.get(dateColumnName);
        StringTimeSeries times = stringTimeSeries.get(timeColumnName);

        String startDate = dates.getFirstValid();
        String startTime = times.getFirstValid();

        if (startDate == null) {
            throw new MalformedFlightFileException("Date column '" + dateColumnName + "' was empty! Cannot set start/end times.");
        }

        if (startTime == null) {
            throw new MalformedFlightFileException("Time column '" + timeColumnName + "' was empty! Cannot set start/end times.");
        }
        startDateTime = startDate + " " + startTime;

        String endDate = dates.getLastValid();
        String endTime = times.getLastValid();

        if (endDate == null) {
            throw new MalformedFlightFileException("Date column '" + dateColumnName + "' was empty! Cannot set end/end times.");
        }

        if (endTime == null) {
            throw new MalformedFlightFileException("Time column '" + timeColumnName + "' was empty! Cannot set end/end times.");
        }

        endDateTime = endDate + " " + endTime;
    }

    private void initialize(InputStream inputStream) throws FatalFlightFileException, IOException {
        numberRows = 0;
        ArrayList<ArrayList<String>> csvValues;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        dataTypes = new ArrayList<String>();
        headers = new ArrayList<String>();

        //file information -- this is the first line
        fileInformation = bufferedReader.readLine();
        if (fileInformation == null || fileInformation.length() == 0) throw new FatalFlightFileException("The flight file was empty.");
        if (fileInformation.charAt(0) != '#') throw new FatalFlightFileException("First line of the flight file should begin with a '#' and contain flight recorder information.");

        String[] infoParts = fileInformation.split(",");
        airframeType = null;
        try {
            for (int i = 1; i < infoParts.length; i++) {
                //System.err.println("splitting key/value: '" + infoParts[i] + "'");
                String subParts[] = infoParts[i].trim().split("=");
                String key = subParts[0];
                String value = subParts[1];

                //System.err.println("key: '" + key + "'");
                //System.err.println("value: '" + value + "'");

                if (key.equals("airframe_name")) {
                    airframeType = value.substring(1, value.length() - 1);
                    break;
                }
            }
        } catch (Exception e) {
            throw new FatalFlightFileException("Flight information line was not properly formed with key value pairs.", e);
        }

        if (airframeType == null)  throw new FatalFlightFileException("Flight information (first line of flight file) does not contain an 'airframe_name' key/value pair.");
        System.err.println("detected airframe type: '" + airframeType + "'");


        //the next line is the column data types
        String dataTypesLine = bufferedReader.readLine();
        if (dataTypesLine.charAt(0) != '#') throw new FatalFlightFileException("Second line of the flight file should begin with a '#' and contain column data types.");
        dataTypesLine = dataTypesLine.substring(1);

        dataTypes.addAll( Arrays.asList( dataTypesLine.split("\\,", -1) ) );
        dataTypes.replaceAll(String::trim);

        //the next line is the column headers
        String headersLine = bufferedReader.readLine();
        System.out.println("Headers line is: " + headersLine);
        headers.addAll( Arrays.asList( headersLine.split("\\,", -1) ) );
        headers.replaceAll(String::trim);

        if (dataTypes.size() != headers.size()) {
            throw new FatalFlightFileException("Number of columns in the header line (" + headers.size() + ") != number of columns in the dataTypes line (" + dataTypes.size() + ")");
        }

        //initialize a sub-ArrayList for each column
        csvValues = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < headers.size(); i++) {
            csvValues.add(new ArrayList<String>());
        }

        int lineNumber = 3;
        boolean lastLineWarning = false;

        String line;
        while ((line = bufferedReader.readLine()) != null) {
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
                    System.err.println("ERROR: line " + lineNumber + " had a different number of values (" + values.length + ") than the number of columns in the file (" + headers.size() + ").");
                    System.err.println("ERROR: Two line errors in a row means the flight file is corrupt.");
                    lastLineWarning = true;
                } else {
                    throw new FatalFlightFileException("A line in the middle of the flight file was missing values, which means the flight file is corrupt.");
                }
            } else {
                if (values.length != headers.size()) {
                    System.err.println("WARNING: line " + lineNumber + " had a different number of values (" + values.length + ") than the number of columns in the file. Not an error if it was the last line in the file.");
                    lastLineWarning = true;
                    continue;
                }
            }

            //for each CSV value
            for (int i = 0; i < values.length; i++) {
                //add this to the respective column in the csvValues ArrayList, trimming the whitespace around it
                csvValues.get(i).add( values[i].trim() );
            }

            lineNumber++;
            numberRows++;
        }

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
                DoubleTimeSeries dts = new DoubleTimeSeries(headers.get(i), dataTypes.get(i), current);
                if (dts.validCount() > 0) {
                    doubleTimeSeries.put(headers.get(i), dts);
                } else {
                    System.err.println("WARNING: dropping double column '" + headers.get(i) + "' because all entries were empty.");
                }

            } else {
                //System.out.println(headers.get(i) + " is a STRING column, ArrayList size: " + current.size());
                //System.out.println(current);
                StringTimeSeries sts = new StringTimeSeries(headers.get(i), dataTypes.get(i), current);
                if (sts.validCount() > 0) {
                    stringTimeSeries.put(headers.get(i), sts);
                } else {
                    System.err.println("WARNING: dropping string column '" + headers.get(i) + "' because all entries were empty.");
                }
            }
        }
    }

    private InputStream getReusableInputStream(InputStream inputStream) throws IOException {
        if (inputStream.markSupported() == false) {
            InputStream reusableInputStream = new ByteArrayInputStream(inputStream.readAllBytes());
            inputStream.close();
            // now the stream should support 'mark' and 'reset'
            return reusableInputStream;
        } else {
            return inputStream;
        }
    }

    private void process(InputStream inputStream) throws IOException, FatalFlightFileException {
        initialize(inputStream);

        //TODO: these may be different for different airframes/flight
        //data recorders. depending on the airframe/flight data recorder 
        //we should specify these.

        try {
            calculateStartEndTime("Lcl Date", "Lcl Time");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateAGL("AltAGL", "AltMSL", "Latitude", "Longitude");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateAirportProximity("Latitude", "Longitude");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateTotalFuel(new String[]{"FQtyL", "FQtyR"}, "Total Fuel");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            calculateLaggedAltMSL("AltMSL", 10, "AltMSL Lag Diff");
        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        try {
            if (airframeType.equals("Cessna 172S") || airframeType.equals("PA-28-181")) {
                String chtNames[] = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"};
                calculateVariance(chtNames, "E1 CHT Variance", "deg F");

                String egtNames[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"};
                calculateVariance(egtNames, "E1 EGT Variance", "deg F");

            } else if (airframeType.equals("PA-44-180")) {
                String egt1Names[] = {"E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"};
                calculateVariance(egt1Names, "E1 EGT Variance", "deg F");

                String egt2Names[] = {"E2 EGT1", "E2 EGT2", "E2 EGT3", "E2 EGT4"};
                calculateVariance(egt2Names, "E2 EGT Variance", "deg F");


            } else if (airframeType.equals("Cirrus SR20")) {
                String chtNames[] = {"E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4", "E1 CHT5", "E1 CHT6"};
                calculateVariance(chtNames, "E1 CHT Variance", "deg F");

                String egtNames[] = {"E1 EGT1", "E1 CHT2", "E1 CHT3", "E1 CHT4", "E1 CHT5", "E1 CHT6"};
                calculateVariance(egtNames, "E1 EGT Variance", "deg F");

            } else {
                LOG.severe("Cannot calculate engine variances! Unknown airframe type: '" + airframeType + "'");
                System.exit(1);
            }

        } catch (MalformedFlightFileException e) {
            exceptions.add(e);
        }

        if (hasCoords && hasAGL) {
            calculateItinerary();
        }
    }

    private void checkExceptions() {
        if (exceptions.size() > 0) {
            status = "WARNING";
            System.err.println("Flight produced " + exceptions.size() + " exceptions.");

            /*
               for (MalformedFlightFileException e : exceptions) {
               e.printStackTrace();
               }
               */
        } else {
            status = "SUCCESS";
        }
    }

    private void checkIfExists(Connection connection) throws FlightAlreadyExistsException {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT uploads.id, uploads.filename, flights.filename FROM flights, uploads WHERE flights.upload_id = uploads.id AND flights.md5_hash = ?");
            preparedStatement.setString(1, md5Hash);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String uploadFilename = resultSet.getString(2);
                String flightsFilename = resultSet.getString(3);

                preparedStatement.close();
                resultSet.close();

                throw new FlightAlreadyExistsException("Flight already exists in database, uploaded in zip file '" + uploadFilename + "' as '" + flightsFilename + "'");
            }

            preparedStatement.close();
            resultSet.close();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Flight(String zipEntryName, InputStream inputStream, Connection connection) throws IOException, FatalFlightFileException, FlightAlreadyExistsException {
        this.filename = zipEntryName;

        if (!filename.contains("/")) {
            throw new FatalFlightFileException("The flight file was not in a directory in the zip file. Flight files should be in a directory with the name of their tail number (or other aircraft identifier).");
        }

        String[] parts = zipEntryName.split("/");
        this.tailNumber = parts[0];

        try {
            inputStream = getReusableInputStream(inputStream);

            int length = inputStream.available();
            inputStream.mark(length);
            setMD5Hash(inputStream);

            //check to see if a flight with this MD5 hash already exists in the database
            if (connection != null) checkIfExists(connection);

            inputStream.reset();
            process(inputStream);

        } catch (FatalFlightFileException e) {
            status = "WARNING";
            throw e;
        } catch (IOException e) {
            status = "WARNING";
            throw e;
        }

        checkExceptions();
    }

    public Flight(String filename, Connection connection) throws IOException, FatalFlightFileException, FlightAlreadyExistsException {
        this.filename = filename;
        String[] parts = filename.split("/");
        this.tailNumber = parts[0];

        try {
            File file = new File(this.filename);
            FileInputStream fileInputStream = new FileInputStream(file);

            InputStream inputStream = getReusableInputStream(fileInputStream);

            int length = inputStream.available();
            inputStream.mark(length);
            setMD5Hash(inputStream);

            //check to see if a flight with this MD5 hash already exists in the database
            if (connection != null) checkIfExists(connection);

            inputStream.reset();
            process(inputStream);
            //        } catch (FileNotFoundException e) {
            //            System.err.println("ERROR: could not find flight file '" + filename + "'");
            //            exceptions.add(e);
        } catch (FatalFlightFileException e) {
            status = "WARNING";
            throw e;
        } catch (IOException e) {
            status = "WARNING";
            throw e;
        }

        checkExceptions();
    }

    public void calculateLaggedAltMSL(String altMSLColumnName, int lag, String laggedColumnName) throws MalformedFlightFileException {
        headers.add(laggedColumnName);
        dataTypes.add("ft msl");

        DoubleTimeSeries altMSL = doubleTimeSeries.get(altMSLColumnName);
        if (altMSL == null) {
            throw new MalformedFlightFileException("Cannot calculate '" + laggedColumnName + "' as parameter '" + altMSLColumnName + "' was missing.");
        }

        DoubleTimeSeries laggedAltMSL = new DoubleTimeSeries(laggedColumnName, "ft msl");

        for (int i = 0; i < altMSL.size(); i++) {
            if (i < lag) laggedAltMSL.add(0.0);
            else {
                laggedAltMSL.add(altMSL.get(i - lag));
            }
        }

        doubleTimeSeries.put(laggedColumnName, laggedAltMSL);
    }


    public void calculateVariance(String[] columnNames, String varianceColumnName, String varianceDataType) throws MalformedFlightFileException {
        headers.add(varianceColumnName);
        dataTypes.add(varianceDataType);

        DoubleTimeSeries columns[] = new DoubleTimeSeries[columnNames.length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = doubleTimeSeries.get(columnNames[i]);

            if (columns[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate '" + varianceColumnName + "' as parameter '" + columnNames[i] + "' was missing.");
            }
        }

        DoubleTimeSeries variance = new DoubleTimeSeries(varianceColumnName, varianceDataType);

        for (int i = 0; i < columns[0].size(); i++) {
            double max = -Double.MAX_VALUE;
            double min = Double.MAX_VALUE;

            for (int j = 0; j < columns.length; j++) {
                double current = columns[j].get(i);
                if (!Double.isNaN(current) && current > max) max = columns[j].get(i);
                if (!Double.isNaN(current) && current < min) min = columns[j].get(i);
            }

            double v = 0;
            if (max != -Double.MAX_VALUE && min != Double.MAX_VALUE) {
                v = max - min;
            }

            variance.add(v);
        }

        doubleTimeSeries.put(varianceColumnName, variance);
    }


    public void calculateTotalFuel(String[] fuelColumnNames, String totalFuelColumnName) throws MalformedFlightFileException {
        headers.add(totalFuelColumnName);
        dataTypes.add("gals");


        DoubleTimeSeries fuelQuantities[] = new DoubleTimeSeries[fuelColumnNames.length];
        for (int i = 0; i < fuelQuantities.length; i++) {
            fuelQuantities[i] = doubleTimeSeries.get(fuelColumnNames[i]);

            if (fuelQuantities[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate 'Total Fuel' as fuel parameter '" + fuelColumnNames[i] + "' was missing.");
            }
        }

        DoubleTimeSeries totalFuel = new DoubleTimeSeries(totalFuelColumnName, "gals");

        for (int i = 0; i < fuelQuantities[0].size(); i++) {
            double totalFuelValue = 0.0;
            for (int j = 0; j < fuelQuantities.length; j++) {
                totalFuelValue += fuelQuantities[j].get(i);
            }
            totalFuel.add(totalFuelValue);

        }

        doubleTimeSeries.put(totalFuelColumnName, totalFuel);

    }

    public void calculateAGL(String altitudeAGLColumnName, String altitudeMSLColumnName, String latitudeColumnName, String longitudeColumnName) throws MalformedFlightFileException {
        //calculates altitudeAGL (above ground level) from altitudeMSL (mean sea levl)
        headers.add(altitudeAGLColumnName);
        dataTypes.add("ft agl");

        DoubleTimeSeries altitudeMSLTS = doubleTimeSeries.get(altitudeMSLColumnName);
        DoubleTimeSeries latitudeTS = doubleTimeSeries.get(latitudeColumnName);
        DoubleTimeSeries longitudeTS = doubleTimeSeries.get(longitudeColumnName);

        if (altitudeMSLTS == null || latitudeTS == null || longitudeTS == null) {
            String message = "Cannot calculate AGL, flight file had empty or missing ";

            int count = 0;
            if (altitudeMSLTS == null) {
                message += "'" + altitudeMSLColumnName + "'";
                count++;
            }

            if (latitudeTS == null) {
                if (count > 0) message += ", ";
                message += "'" + latitudeColumnName + "'";
                count++;
            }

            if (longitudeTS == null) {
                if (count > 0) message += " and ";
                message += "'" + longitudeColumnName + "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            //should be initialized to false, but lets make sure
            hasCoords = false;
            hasAGL = false;
            throw new MalformedFlightFileException(message);
        }
        hasCoords = true;
        hasAGL = true;

        DoubleTimeSeries altitudeAGLTS = new DoubleTimeSeries(altitudeAGLColumnName, "ft agl");

        for (int i = 0; i < altitudeMSLTS.size(); i++) {
            double altitudeMSL = altitudeMSLTS.get(i);
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);

            //System.err.println("getting AGL for latitude: " + latitude + ", " + longitude);

            if (Double.isNaN(altitudeMSL) || Double.isNaN(latitude) || Double.isNaN(longitude)) {
                altitudeAGLTS.add(Double.NaN);
                //System.err.println("result is: " + Double.NaN);
                continue;
            }

            try {
                int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);

                altitudeAGLTS.add(altitudeAGL);

                //the terrain cache will not be able to find the file if the lat/long is outside of the USA
            } catch (NoSuchFileException e) {
                System.err.println("ERROR: could not read terrain file: " + e);

                hasAGL = false;
                throw new MalformedFlightFileException("Could not calculate AGL for this flight as it had latitudes/longitudes outside of the United States.");
            }

            //System.out.println("msl: " + altitudeMSL + ", agl: " + altitudeAGL);
        }

        doubleTimeSeries.put(altitudeAGLColumnName, altitudeAGLTS);
    }

    public void calculateAirportProximity(String latitudeColumnName, String longitudeColumnName) throws MalformedFlightFileException {
        //calculates if the aircraft is within maxAirportDistance from an airport

        DoubleTimeSeries latitudeTS = doubleTimeSeries.get(latitudeColumnName);
        DoubleTimeSeries longitudeTS = doubleTimeSeries.get(longitudeColumnName);

        if (latitudeTS == null || longitudeTS == null) {
            String message = "Cannot calculate airport and runway distances, flight file had empty or missing ";

            int count = 0;
            if (latitudeTS == null) {
                message += "'" + latitudeColumnName + "'";
                count++;
            }

            if (longitudeTS == null) {
                if (count > 0) message += " and ";
                message += "'" + longitudeColumnName + "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            //should be initialized to false, but lets make sure
            hasCoords = false;
            throw new MalformedFlightFileException(message);
        }
        hasCoords = true;

        headers.add("NearestAirport");
        dataTypes.add("IATA Code");

        headers.add("AirportDistance");
        dataTypes.add("ft");

        headers.add("NearestRunway");
        dataTypes.add("IATA Code");

        headers.add("RunwayDistance");
        dataTypes.add("ft");

        StringTimeSeries nearestAirportTS = new StringTimeSeries("NearestAirport", "txt");
        stringTimeSeries.put("NearestAirport", nearestAirportTS);
        DoubleTimeSeries airportDistanceTS = new DoubleTimeSeries("AirportDistance", "ft");
        doubleTimeSeries.put("AirportDistance", airportDistanceTS);

        StringTimeSeries nearestRunwayTS = new StringTimeSeries("NearestRunway", "txt");
        stringTimeSeries.put("NearestRunway", nearestRunwayTS);
        DoubleTimeSeries runwayDistanceTS = new DoubleTimeSeries("RunwayDistance", "ft");
        doubleTimeSeries.put("RunwayDistance", runwayDistanceTS);


        for (int i = 0; i < latitudeTS.size(); i++) {
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);

            MutableDouble airportDistance = new MutableDouble();
            Airport airport = Airports.getNearestAirportWithin(latitude, longitude, MAX_AIRPORT_DISTANCE_FT, airportDistance);
            if (airport == null) {
                nearestAirportTS.add("");
                airportDistanceTS.add(Double.NaN);
                nearestRunwayTS.add("");
                runwayDistanceTS.add(Double.NaN);

                //System.out.println(latitude + ", " + longitude + ", null, null, null, null");
            } else {
                nearestAirportTS.add(airport.iataCode);
                airportDistanceTS.add(airportDistance.get());

                MutableDouble runwayDistance = new MutableDouble();
                Runway runway = airport.getNearestRunwayWithin(latitude, longitude, MAX_RUNWAY_DISTANCE_FT, runwayDistance);
                if (runway == null) {
                    nearestRunwayTS.add("");
                    runwayDistanceTS.add(Double.NaN);
                    //System.out.println(latitude + ", " + longitude + ", " + airport.iataCode + ", " + airportDistance.get() + ", " + null + ", " + null);
                } else {
                    nearestRunwayTS.add(runway.name);
                    runwayDistanceTS.add(runwayDistance.get());
                    //System.out.println(latitude + ", " + longitude + ", " + airport.iataCode + ", " + airportDistance.get() + ", " + runway.name + ", " + runwayDistance.get());
                }
            }

        }
    }

    public void calculateItinerary() {
        //cannot calculate the itinerary without airport/runway calculate, which requires
        //lat and longs
        if (!hasCoords) return;

        StringTimeSeries nearestAirportTS = stringTimeSeries.get("NearestAirport");
        DoubleTimeSeries airportDistanceTS = doubleTimeSeries.get("AirportDistance");
        DoubleTimeSeries altitudeAGL = doubleTimeSeries.get("AltAGL");

        StringTimeSeries nearestRunwayTS = stringTimeSeries.get("NearestRunway");
        DoubleTimeSeries runwayDistanceTS = doubleTimeSeries.get("RunwayDistance");

        itinerary.clear();

        Itinerary currentItinerary = null;
        for (int i = 1; i < nearestAirportTS.size(); i++) {
            String airport = nearestAirportTS.get(i);
            String runway = nearestRunwayTS.get(i);

            if (airport != null && !airport.equals("")) {
                //We've gotten close to an airport, so create a stop if there
                //isn't one.  If there is one, update the runway being visited.
                //If the airport is a new airport (this shouldn't happen really),
                //then create a new stop.
                if (currentItinerary == null) {
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i));
                } else if (airport.equals(currentItinerary.getAirport())) {
                    currentItinerary.update(runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i));
                } else {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
                    currentItinerary = new Itinerary(airport, runway, i, altitudeAGL.get(i), airportDistanceTS.get(i), runwayDistanceTS.get(i));
                }
            } else {
                //aiport is null, so if there was an airport being visited
                //then we can determine it's runway and add it to the itinerary
                if (currentItinerary != null) {
                    currentItinerary.selectBestRunway();
                    if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
                }

                //set the currentItinerary to null until we approach another
                //airport
                currentItinerary = null;
            }
        }

        //dont forget to add the last stop in the itinerary if it wasn't set to null
        if (currentItinerary != null) {
            currentItinerary.selectBestRunway();
            if (currentItinerary.wasApproach()) itinerary.add(currentItinerary);
        }

        System.err.println("Itinerary:");
        for (int i = 0; i < itinerary.size(); i++) {
            System.err.println(itinerary.get(i));
        }
    }

    public void printValues(String[] requestedHeaders) {
        System.out.println("Values:");

        for (int i = 0; i < requestedHeaders.length; i++) {
            if (i > 0) System.out.print(",");
            System.out.printf("%16s", requestedHeaders[i]);
        }
        System.out.println();

        for (int i = 0; i < requestedHeaders.length; i++) {
            String header = requestedHeaders[i];
            if (i > 0) System.out.print(",");

            if (doubleTimeSeries.containsKey(header)) {
                System.out.printf("%16s", doubleTimeSeries.get(header).size());
            } else if (stringTimeSeries.containsKey(header)) {
                System.out.printf("%16s", stringTimeSeries.get(header).size());
            } else {
                System.out.println("ERROR: header '" + header + "' not present in flight file: '" + filename + "'");
                System.exit(1);
            }
        }
        System.out.println();

        for (int row = 0; row < numberRows; row++) {
            boolean first = true;
            for (int i = 0; i < requestedHeaders.length; i++) {
                String header = requestedHeaders[i];

                if (!first) System.out.print(",");
                first = false;

                if (doubleTimeSeries.containsKey(header)) {
                    System.out.printf("%16.8f", doubleTimeSeries.get(header).get(row));
                } else if (stringTimeSeries.containsKey(header)) {
                    System.out.printf("%16s", stringTimeSeries.get(header).get(row));
                } else {
                    System.out.println("ERROR: header '" + header + "' not present in flight file: '" + filename + "'");
                    System.exit(1);
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public void updateDatabase(Connection connection, int uploadId, int uploaderId, int fleetId) {
        this.fleetId = fleetId;
        this.uploaderId = uploaderId;
        this.uploadId = uploadId;

        try {
            //first check and see if the airframe and tail number already exist in the database for this 
            //flight
            int airframeId = Airframes.getId(connection, airframeType);
            Airframes.setAirframeFleet(connection, airframeId, fleetId);

            int tailId = Tails.getId(connection, fleetId, tailNumber);

            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO flights (fleet_id, uploader_id, upload_id, airframe_id, tail_id, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl, insert_completed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, uploaderId);
            preparedStatement.setInt(3, uploadId);
            preparedStatement.setInt(4, airframeId);
            preparedStatement.setInt(5, tailId);
            preparedStatement.setString(6, startDateTime);
            preparedStatement.setString(7, endDateTime);
            preparedStatement.setString(8, filename);
            preparedStatement.setString(9, md5Hash);
            preparedStatement.setInt(10, numberRows);
            preparedStatement.setString(11, status);
            preparedStatement.setBoolean(12, hasCoords);
            preparedStatement.setBoolean(13, hasAGL);
            preparedStatement.setBoolean(14, false); //insert not yet completed

            System.out.println(preparedStatement);
            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                int flightId = resultSet.getInt(1);
                this.id = flightId;

                for (DoubleTimeSeries series : doubleTimeSeries.values()) {
                    series.updateDatabase(connection, flightId);
                }

                for (StringTimeSeries series : stringTimeSeries.values()) {
                    series.updateDatabase(connection, flightId);
                }

                for (Exception exception : exceptions) {
                    FlightWarning.insertWarning(connection, flightId, exception.getMessage());
                }

                for (int i = 0; i < itinerary.size(); i++) {
                    itinerary.get(i).updateDatabase(connection, flightId, i);
                }

                PreparedStatement ps = connection.prepareStatement("UPDATE flights SET insert_completed = 1 WHERE id = ?");
                ps.setInt(1, this.id);
                ps.executeUpdate();

            } else {
                System.err.println("ERROR: insertion of flight to the database did not result in an id.  This should never happen.");
                System.exit(1);
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void writeToFile(String filename, String[] columnNames) throws IOException {
        ArrayList<DoubleTimeSeries> series = new ArrayList<DoubleTimeSeries>();

        for (int i = 0; i < columnNames.length; i++) {
            series.add(getDoubleTimeSeries(columnNames[i]));
        }

        PrintWriter printWriter = new PrintWriter(new FileWriter(filename));

        printWriter.print("#");
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) printWriter.print(",");
            printWriter.print(columnNames[i]);
        }
        printWriter.println();

        printWriter.print("#");
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) printWriter.print(",");
            printWriter.print(series.get(i).getDataType());
        }
        printWriter.println();

        for (int i = 0; i < numberRows; i++) {
            for (int j = 0; j < series.size(); j++) {
                if (j > 0) printWriter.print(",");
                printWriter.print(series.get(j).get(i));
            }
            printWriter.println();
        }

        printWriter.close();
    }

}
