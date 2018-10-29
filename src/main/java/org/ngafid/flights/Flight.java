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

import javax.xml.bind.DatatypeConverter;

import org.ngafid.common.MutableDouble;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.terrain.TerrainCache;


public class Flight {
    private final static double MAX_AIRPORT_DISTANCE_FT = 10000;
    private final static double MAX_RUNWAY_DISTANCE_FT = 100;

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

    private String status;
    private ArrayList<MalformedFlightFileException> exceptions = new ArrayList<MalformedFlightFileException>();

    private int numberRows;
    private String fileInformation;
    private ArrayList<String> dataTypes;
    private ArrayList<String> headers;

    private HashMap<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<String, DoubleTimeSeries>();
    private HashMap<String, StringTimeSeries> stringTimeSeries = new HashMap<String, StringTimeSeries>();

    private ArrayList<Itinerary> itinerary = new ArrayList<Itinerary>();

    public int getNumberRows() {
        return numberRows;
    }

    public String getStatus() {
        return status;
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
        System.err.println("detected airframe type: '" + airframeType);


        //the next line is the column data types
        String dataTypesLine = bufferedReader.readLine();
        if (dataTypesLine.charAt(0) != '#') throw new FatalFlightFileException("Second line of the flight file should begin with a '#' and contain column data types.");
        dataTypesLine = dataTypesLine.substring(1);

        dataTypes.addAll( Arrays.asList( dataTypesLine.split("\\,", -1) ) );
        dataTypes.replaceAll(String::trim);

        //the next line is the column headers
        String headersLine = bufferedReader.readLine();
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
                DoubleTimeSeries dts = new DoubleTimeSeries(headers.get(i), dataTypes.get(i), current);
                if (dts.validCount() > 0) {
                    doubleTimeSeries.put(headers.get(i), dts);
                } else {
                    System.err.println("WARNING: dropping double column '" + headers.get(i) + "' because all entries were empty.");
                }

            } else {
                //System.out.println(headers.get(i) + " is a STRING column, ArrayList size: " + current.size());
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

        calculateItinerary();
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

        String[] parts = zipEntryName.split("/");
        this.tailNumber = parts[0];
        //System.out.println("tail number is: " + tailNumber);

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
        doubleTimeSeries.put(altitudeAGLColumnName, altitudeAGLTS);

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

            int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);

            altitudeAGLTS.add(altitudeAGL);

            //System.out.println("msl: " + altitudeMSL + ", agl: " + altitudeAGL);
        }
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
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO flights (fleet_id, uploader_id, upload_id, airframe_type, tail_number, start_time, end_time, filename, md5_hash, number_rows, status, has_coords, has_agl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, uploaderId);
            preparedStatement.setInt(3, uploadId);
            preparedStatement.setString(4, airframeType);
            preparedStatement.setString(5, tailNumber);
            preparedStatement.setString(6, startDateTime);
            preparedStatement.setString(7, endDateTime);
            preparedStatement.setString(8, filename);
            preparedStatement.setString(9, md5Hash);
            preparedStatement.setInt(10, numberRows);
            preparedStatement.setString(11, status);
            preparedStatement.setBoolean(12, hasCoords);
            preparedStatement.setBoolean(13, hasAGL);
            preparedStatement.executeUpdate();

            System.out.println(preparedStatement);

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                int flightId = resultSet.getInt(1);

                for (DoubleTimeSeries series : doubleTimeSeries.values()) {
                    series.updateDatabase(connection, flightId);
                }

                for (StringTimeSeries series : stringTimeSeries.values()) {
                    series.updateDatabase(connection, flightId);
                }

                for (Exception exception : exceptions) {
                    PreparedStatement exceptionPreparedStatement = connection.prepareStatement("INSERT INTO flight_warnings (flight_id, message, stack_trace) VALUES (?, ?, ?)");
                    exceptionPreparedStatement.setInt(1, flightId);
                    exceptionPreparedStatement.setString(2, exception.getMessage());

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    String sStackTrace = sw.toString(); // stack trace as a string

                    exceptionPreparedStatement.setString(3, sStackTrace);
                    exceptionPreparedStatement.executeUpdate();

                }

                for (int i = 0; i < itinerary.size(); i++) {
                    itinerary.get(i).updateDatabase(connection, flightId, i);
                }
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

        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) printWriter.print(",");
            printWriter.print(columnNames[i]);
        }
        printWriter.println();

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
