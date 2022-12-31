package org.ngafid.flights;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import static org.ngafid.common.TimeUtils.addMilliseconds;

public class DJIFlightProcessor {
    private static final Logger LOG = Logger.getLogger(DJIFlightProcessor.class.getName());

    private static final Set<String> STRING_COLS = new HashSet<>(List.of(new String[]{"flyCState", "flycCommand", "flightAction",
            "nonGPSCause", "connectedToRC", "Battery:lowVoltage", "RC:ModeSwitch", "gpsUsed", "visionUsed", "IMUEX(0):err"}));

    public static Flight processDATFile(int fleetId, String entry, InputStream stream, Connection connection)
            throws SQLException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
        List<InputStream> inputStreams = duplicateInputStream(stream, 2);
        Map<String, DoubleTimeSeries> doubleTimeSeriesMap = getDoubleTimeSeriesMap(connection);
        Map<String, StringTimeSeries> stringTimeSeriesMap = getStringTimeSeriesMap(connection);
        Map<Integer, String> indexedCols = new HashMap<>();
        Map<String, String> attributeMap = getAttributeMap(inputStreams.remove(inputStreams.size() - 1));
        String flightStatus = "SUCCESS";

        if (!attributeMap.containsKey("mcID(SN)")) {
            throw new FatalFlightFileException("No DJI serial number provided in binary.");
        }

        try (CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(inputStreams.remove(inputStreams.size() - 1))))) {
            readData(reader, doubleTimeSeriesMap, stringTimeSeriesMap, indexedCols);
            calculateLatLonGPS(connection, doubleTimeSeriesMap);

            if (attributeMap.containsKey("dateTime")) {
                calculateDateTime(connection, doubleTimeSeriesMap, stringTimeSeriesMap, attributeMap.get("dateTime"));
            } else {
                // TODO: Data might have another way of determining the date/time
                flightStatus = "WARNING";
            }
        } catch (CsvValidationException e) {
            throw new FatalFlightFileException("Error parsing CSV file: " + e.getMessage());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        dropBlankCols(doubleTimeSeriesMap, stringTimeSeriesMap);

        Flight flight = new Flight(fleetId, entry, attributeMap.get("mcID(SN)"), "DJI " + attributeMap.get("ACType"),
                doubleTimeSeriesMap, stringTimeSeriesMap, connection);
        flight.setStatus(flightStatus);
        flight.setAirframeType("UAS Rotorcraft");
        flight.setAirframeTypeID(4);

        return flight;
    }

    private static void readData(CSVReader reader, Map<String, DoubleTimeSeries> doubleTimeSeriesMap,
                                 Map<String, StringTimeSeries> stringTimeSeriesMap, Map<Integer, String> indexedCols) throws IOException, CsvValidationException {
        String[] line;
        String[] headers = reader.readNext();

        for (int i = 0; i < headers.length; i++) {
            indexedCols.put(i, headers[i]);
        }

        while ((line = reader.readNext()) != null) {
            for (int i = 0; i < line.length; i++) {

                String column = indexedCols.get(i);

                try {
                    if (doubleTimeSeriesMap.containsKey(column)) {
                        DoubleTimeSeries colTimeSeries = doubleTimeSeriesMap.get(column);
                        double value = !line[i].equals("") ? Double.parseDouble(line[i]) : Double.NaN;
                        colTimeSeries.add(value);
                    } else {
                        StringTimeSeries colTimeSeries = stringTimeSeriesMap.get(column);
                        colTimeSeries.add(line[i]);
                    }
                } catch (NullPointerException e) {
                    LOG.log(Level.WARNING, "Column {0} not found in time series map", column);
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Could not parse value {0} as double", line[i]);
                }
            }
        }
    }

    private static void calculateLatLonGPS(Connection connection, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException {
        DoubleTimeSeries lonRad = doubleTimeSeriesMap.get("GPS(0):Long");
        DoubleTimeSeries latRad = doubleTimeSeriesMap.get("GPS(0):Lat");

        DoubleTimeSeries longDeg = new DoubleTimeSeries(connection, "Longitude", "degrees");
        DoubleTimeSeries latDeg = new DoubleTimeSeries(connection, "Latitude", "degrees");

        for (int i = 0; i < lonRad.size(); i++) {
            longDeg.add(Math.toDegrees(lonRad.get(i)) / 100);
        }

        for (int i = 0; i < lonRad.size(); i++) {
            latDeg.add(Math.toDegrees(latRad.get(i)) / 100);
        }

        doubleTimeSeriesMap.put("Longitude", longDeg);
        doubleTimeSeriesMap.put("Latitude", latDeg);
    }

    private static void calculateDateTime(Connection connection, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap, String dateTimeStr) throws SQLException, ParseException {
        StringTimeSeries localDateSeries = new StringTimeSeries(connection, "Lcl Date", "yyyy-mm-dd");
        StringTimeSeries localTimeSeries = new StringTimeSeries(connection, "Lcl Time", "hh:mm:ss");
        StringTimeSeries utcOfstSeries = new StringTimeSeries(connection, "UTCOfst", "hh:mm"); // Always 0
        DoubleTimeSeries seconds = doubleTimeSeriesMap.get("offsetTime");

        SimpleDateFormat lclDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat lclTimeFormat = new SimpleDateFormat("HH:mm:ss");

        String[] dateTime = dateTimeStr.split(" ");
        String date = dateTime[0];

        if (date.split("-")[1].length() == 1) {
            date = date.substring(0, 5) + "0" + date.substring(5);
        }

        if (date.split("-")[2].length() == 1) {
            date = date.substring(0, 8) + "0" + date.substring(8);
        }

        String time = dateTime[1];

        Date parsedDate = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(date + " " + time);
        for (int i = 0; i < seconds.size(); i++) {
            int millseconds = (int) (seconds.get(i) * 1000);
            Date newDate = addMilliseconds(parsedDate, millseconds);

            localDateSeries.add(lclDateFormat.format(newDate));
            localTimeSeries.add(lclTimeFormat.format(newDate));
            utcOfstSeries.add("+00:00");
        }

        stringTimeSeriesMap.put("Lcl Date", localDateSeries);
        stringTimeSeriesMap.put("Lcl Time", localTimeSeries);
        stringTimeSeriesMap.put("UTCOfst", utcOfstSeries);
    }

    private static List<InputStream> duplicateInputStream(InputStream inputStream, int copies) throws IOException {
        List<InputStream> inputStreams = new ArrayList<>();
        List<OutputStream> outputStreams = new ArrayList<>();

        for (int i = 0; i < copies; i++) {
            outputStreams.add(new ByteArrayOutputStream());
        }

        byte[] buffer = new byte[1024];
        while (inputStream.read(buffer) > -1) {
            for (OutputStream outputStream : outputStreams) {
                outputStream.write(buffer);
            }
        }

        for (OutputStream outputStream : outputStreams) {
            outputStream.flush();
            inputStreams.add(new ByteArrayInputStream(((ByteArrayOutputStream) outputStream).toByteArray()));
        }

        return inputStreams;
    }

    private static Map<String, String> getAttributeMap(InputStream stream) {
        Map<String, String> attributeMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(stream)))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[line.length - 1].contains("|")) {
                    String[] split = line[line.length - 1].split("\\|");
                    attributeMap.put(split[0], split[1]);
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        LOG.log(Level.INFO, "Attribute Map: {0}", attributeMap);

        return attributeMap;
    }

    private static void indexCols(String[] cols, Map<Integer, String> indexedCols) {
        int i = 0;
        for (String col : cols) {
            indexedCols.put(i++, col);
        }
    }

    private static void dropBlankCols(Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        for (String key : doubleTimeSeriesMap.keySet()) {
            if (doubleTimeSeriesMap.get(key).size() == 0) {
                doubleTimeSeriesMap.remove(key);
            }
        }

        for (String key : stringTimeSeriesMap.keySet()) {
            if (stringTimeSeriesMap.get(key).size() == 0) {
                stringTimeSeriesMap.remove(key);
            }
        }
    }

    private static void processCols(Connection connection, String[] cols, Map<Integer, String> indexedCols, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) throws SQLException {
        int i = 0;
        for (String col : cols) {
            indexedCols.put(i++, col);
            String category = col.split(":")[0];

            switch (category) {
                case "IMU_ATTI(0)":
                case "IMUEX(0)":
                    handleIMUDataType(connection, col, doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;
                case "GPS(0)":
                    handleGPSDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "Battery(0)":
                case "SMART_BATT":
                    handleBatteryDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "Motor":
                    handleMotorDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "General":
                    doubleTimeSeriesMap.put(col, new DoubleTimeSeries(connection, col, "ft"));
                    break;

                case "Controller":
                    doubleTimeSeriesMap.put(col, new DoubleTimeSeries(connection, col, "level"));
                    break;

            }

        }
    }

    private static void handleIMUDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) throws SQLException {
        String dataType;

        if (colName.contains("Longitude") || colName.contains("Latitude")) {
            dataType = "radians";
        } else if (colName.contains("roll") || colName.contains("pitch") || colName.contains("yaw") || colName.contains("directionOfTravel")) {
            dataType = "degrees";
        } else if (colName.contains("distance") || colName.contains("GPS-H") || colName.contains("Alti")) {
            dataType = "ft";
        } else if (colName.contains("temperature")) {
            dataType = "Celsius";
        } else {
            if (colName.contains("err")) {
                stringTimeSeriesMap.put("IMUEX(0):err", new StringTimeSeries(connection, "IMUEX Error", "error"));
                return;
            }

            dataType = "number";
            LOG.log(Level.WARNING, "IMU Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void handleGPSDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException {
        doubleTimeSeriesMap.put("GPS(0):sAcc", new DoubleTimeSeries(connection, "Speed Accuracy", "cm/s"));

        String dataType;

        if (colName.contains("Long") || colName.contains("Lat")) {
            dataType = "radians";
        } else if (colName.contains("vel")) {
            dataType = "m/s";
        } else if (colName.contains("height")) {
            dataType = "ft";
        } else if (colName.contains("DOP")) {
            dataType = "DOP Value";
        } else if (colName.contains("Date")) {
            dataType = "Date";
        } else if (colName.contains("Time")) {
            dataType = "Time";
        } else if (colName.contains("sAcc")) {
            dataType = "cm/s";
        } else {
            dataType = "number";
            if (!colName.contains("num")) {
                LOG.log(Level.WARNING, "GPS Unknown data type: {0}", colName);
            }
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void handleBatteryDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException {
        String dataType = "number";
        colName = colName.toLowerCase();

        if (colName.contains("volt")) {
            dataType = "Voltage";
        } else if (colName.contains("watts")) {
            dataType = "Watts";
        } else if (colName.contains("current")) {
            dataType = "Amps";
        } else if (colName.contains("cap")) {
            dataType = "Capacity";
        } else if (colName.contains("temp")) {
            dataType = "Celsius";
        } else if (colName.contains("%")) {
            dataType = "Percentage";
        } else if (colName.contains("time")) {
            dataType = "seconds";
        } else {
            LOG.log(Level.WARNING, "Battery Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void handleMotorDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException {
        String dataType = "number";

        if (colName.contains("V_out") || colName.contains("Volts")) {
            dataType = "Voltage";
        } else if (colName.contains("Current")) {
            dataType = "Amps";
        } else if (colName.contains("PPMrecv")) {
            dataType = "RC Stop Command";
        } else if (colName.contains("Temp")) {
            dataType = "Celsius";
        } else if (colName.contains("Status")) {
            dataType = "Status Number";
        } else {
            LOG.log(Level.WARNING, "Battery Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void processRC(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException {
        String dataType;

        if (colName.contains("Aileron")) {
            dataType = "Aileron";
        } else if (colName.contains("Elevator")) {
            dataType = "Elevator";
        } else if (colName.contains("Rudder")) {
            dataType = "Rudder";
        } else if (colName.contains("Throttle")) {
            dataType = "Throttle";
        } else {
            dataType = "number";
            LOG.log(Level.WARNING, "RC Unknown data type: {0}", colName);
        }
    }


    private static Map<String, DoubleTimeSeries> getDoubleTimeSeriesMap(Connection connection) throws SQLException {
        Map<String, DoubleTimeSeries> doubleTimeSeriesMap = new HashMap<>();
        doubleTimeSeriesMap.put("Tick#", new DoubleTimeSeries(connection, "Tick", "tick"));
        doubleTimeSeriesMap.put("offsetTime", new DoubleTimeSeries(connection, "Offset Time", "seconds"));

        doubleTimeSeriesMap.put("flightTime", new DoubleTimeSeries(connection, "Flight Time", "seconds"));
        doubleTimeSeriesMap.put("gpsHealth", new DoubleTimeSeries(connection, "GPS Health", "Health"));

        doubleTimeSeriesMap.put("AirComp:AirSpeedBody:X", new DoubleTimeSeries(connection, "Airspeed Body X", "knots"));
        doubleTimeSeriesMap.put("AirComp:AirSpeedBody:Y", new DoubleTimeSeries(connection, "Airspeed Body Y", "knots"));
        doubleTimeSeriesMap.put("AirComp:Alti", new DoubleTimeSeries(connection, "Airspeed Altitude ", "ft"));
        doubleTimeSeriesMap.put("AirComp:VelNorm", new DoubleTimeSeries(connection, "Airspeed Norm Velocity", "k/h"));
        doubleTimeSeriesMap.put("AirComp:AirSpeedGround:X", new DoubleTimeSeries(connection, "Airspeed Ground X", "knots"));
        doubleTimeSeriesMap.put("AirComp:AirSpeedGround:Y", new DoubleTimeSeries(connection, "Airspeed Ground Y", "knots"));
        doubleTimeSeriesMap.put("AirComp:VelLevel", new DoubleTimeSeries(connection, "Airspeed Level Velocity", "k/h"));

        doubleTimeSeriesMap.put("IMUEX(0):rtk_Longitude", new DoubleTimeSeries(connection, "RTK Longitude", "radians"));
        doubleTimeSeriesMap.put("IMUEX(0):rtk_Latitude", new DoubleTimeSeries(connection, "RTK Latitude", "radians"));
        doubleTimeSeriesMap.put("IMUEX(0):rtk_Alti", new DoubleTimeSeries(connection, "RTK Altitude", "ft"));

        return doubleTimeSeriesMap;
    }

    private static Map<String, StringTimeSeries> getStringTimeSeriesMap(Connection connection) throws SQLException {
        Map<String, StringTimeSeries> stringTimeSeriesMap = new HashMap<>();
        stringTimeSeriesMap.put("GPS:dateTimeStamp", new StringTimeSeries(connection, "GPS Date Time Stamp", "yyyy-mm-ddThh:mm:ssZ"));

        stringTimeSeriesMap.put("flyCState", new StringTimeSeries(connection, "Flight CState", "CState"));
        stringTimeSeriesMap.put("flycCommand", new StringTimeSeries(connection, "Flight Command", "Command"));
        stringTimeSeriesMap.put("flightAction", new StringTimeSeries(connection, "Flight Action", "Action"));
        stringTimeSeriesMap.put("nonGPSCause", new StringTimeSeries(connection, "Non GPS Cause", "GPS Cause"));
        stringTimeSeriesMap.put("connectedToRC", new StringTimeSeries(connection, "Connected To RC", "Connection"));
        stringTimeSeriesMap.put("Battery:lowVoltage", new StringTimeSeries(connection, "Battery:lowVoltage", "")); // Unknown. Does not appear in data
        stringTimeSeriesMap.put("RC:ModeSwitch", new StringTimeSeries(connection, "RC Mode Switch", "Mode")); // Unknown. Just shows P
        stringTimeSeriesMap.put("gpsUsed", new StringTimeSeries(connection, "GPS Used", "boolean"));
        stringTimeSeriesMap.put("visionUsed", new StringTimeSeries(connection, "Vision Used", "boolean"));
        stringTimeSeriesMap.put("Attribute|Value", new StringTimeSeries(connection, "Attribute|Value", "Key-Value Pair"));

        return stringTimeSeriesMap;
    }

}
