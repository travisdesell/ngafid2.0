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
        Map<Integer, String> indexedCols = new HashMap<>();
        Map<String, DoubleTimeSeries> doubleTimeSeriesMap = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeriesMap = new HashMap<>();
        Map<String, String> attributeMap = getAttributeMap(inputStreams.remove(inputStreams.size() - 1));
        String flightStatus = "SUCCESS";


        if (!attributeMap.containsKey("mcID(SN)")) {
            throw new FatalFlightFileException("No DJI serial number provided in binary.");
        }

        try (CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(inputStreams.remove(inputStreams.size() - 1))))) {
            processCols(connection, reader.readNext(), indexedCols, doubleTimeSeriesMap, stringTimeSeriesMap);

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

    private static void calculateLatLonGPS(Connection connection, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException, FatalFlightFileException {
        DoubleTimeSeries lonRad = doubleTimeSeriesMap.get("GPS(0):Long");
        DoubleTimeSeries latRad = doubleTimeSeriesMap.get("GPS(0):Lat");

        if (lonRad == null || latRad == null) {
            LOG.log(Level.WARNING, "Could not find GPS(0):Long or GPS(0):Lat in time series map");
            throw new FatalFlightFileException("No GPS data found in binary.");
        }

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

            if (category.contains("(")) {
                category = category.substring(0, category.indexOf("("));
            }

            switch (category) {
                case "IMU_ATTI":
                case "IMUEX":
                    handleIMUDataType(connection, col, doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;
                case "GPS":
                    handleGPSDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "Battery":
                case "SMART_BATT":
                    handleBatteryDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "Motor":
                    handleMotorDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "RC":
                    handleRCDataType(connection, col, doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;

                case "AirComp":
                    handleAirCompDataType(connection, col, doubleTimeSeriesMap);
                    break;

                case "General":
                    doubleTimeSeriesMap.put(col, new DoubleTimeSeries(connection, col, "ft"));
                    break;

                case "Controller":
                    doubleTimeSeriesMap.put(col, new DoubleTimeSeries(connection, col, "level"));
                    break;

                default:
                    handleMiscDataType(connection, col, doubleTimeSeriesMap, stringTimeSeriesMap);
            }

        }
    }

    private static void handleIMUDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) throws SQLException {
        String dataType;

        if (colName.contains("accel")) {
            dataType = "m/s^2";
        } else if (colName.contains("gyro") || colName.contains("Gyro")) {
            dataType = "deg/s";
        } else if (colName.contains("vel") || colName.contains("Velocity")) {
            dataType = "m/s";
        } else if (colName.contains("mag")) {
            dataType = "A/m";
        } else if (colName.contains("Longitude") || colName.contains("Latitude")) {
            dataType = "radians";
        } else if (colName.contains("roll") || colName.contains("pitch") || colName.contains("yaw") || colName.contains("directionOfTravel")) {
            dataType = "degrees";
        } else if (colName.contains("distance") || colName.contains("GPS-H") || colName.contains("Alti")) {
            dataType = "ft";
        } else if (colName.contains("temperature")) {
            dataType = "Celsius";
        } else if (colName.contains("barometer")) {
            dataType = "atm";
        } else {
            if (colName.contains("err")) {
                stringTimeSeriesMap.put("IMUEX(0):err", new StringTimeSeries(connection, "IMUEX Error", "error"));
                return;
            }

            dataType = "number";
            if (!colName.contains("num")) {
                LOG.log(Level.WARNING, "IMU Unknown data type: {0}", colName);

            }
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
        String lowerColName = colName.toLowerCase();

        if (lowerColName.contains("volt")) {
            dataType = "Voltage";
        } else if (lowerColName.contains("watts")) {
            dataType = "Watts";
        } else if (lowerColName.contains("current")) {
            dataType = "Amps";
        } else if (lowerColName.contains("cap")) {
            dataType = "Capacity";
        } else if (lowerColName.contains("temp")) {
            dataType = "Celsius";
        } else if (lowerColName.contains("%")) {
            dataType = "Percentage";
        } else if (lowerColName.contains("time")) {
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
        } else if (colName.contains("Speed")) {
            dataType = "m/s";
        } else if (colName.contains("Current")) {
            dataType = "Amps";
        } else if (colName.contains("PPMrecv")) {
            dataType = "RC Stop Command";
        } else if (colName.contains("Temp")) {
            dataType = "Celsius";
        } else if (colName.contains("Status")) {
            dataType = "Status Number";
        } else if (colName.contains("Hz")) {
            dataType = "Status Number";
        } else {
            LOG.log(Level.WARNING, "Battery Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void handleRCDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) throws SQLException {
        String dataType = "number";

        if (colName.contains("Aileron")) {
            dataType = "Aileron";
        } else if (colName.contains("Elevator")) {
            dataType = "Elevator";
        } else if (colName.contains("Rudder")) {
            dataType = "Rudder";
        } else if (colName.contains("Throttle")) {
            dataType = "Throttle";
        } else {
            if (colName.equals("RC:ModeSwitch")) {
                stringTimeSeriesMap.put(colName, new StringTimeSeries(connection, "RC Mode Switch", "Mode"));
                return;
            }

            LOG.log(Level.WARNING, "RC Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void handleAirCompDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws SQLException {
        String dataType;

        if (colName.contains("AirSpeed")) {
            dataType = "knots";
        } else if (colName.contains("Alti")) {
            dataType = "ft";
        } else if (colName.contains("Vel")) {
            dataType = "k/h";
        } else {
            dataType = "number";
            LOG.log(Level.WARNING, "AirComp Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
    }

    private static void handleMiscDataType(Connection connection, String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) throws SQLException {
        String dataType;
        boolean isDouble = true;
        switch (colName) {
            case "Tick#":
                dataType = "tick";
                break;

            case "offsetTime":
            case "flightTime":
                dataType = "seconds";
                break;

            case "gpsHealth":
                dataType = "GPS Health";
                break;

            case "flyCState":
                dataType = "C State";
                isDouble = false;
                break;

            case "flycCommand":
                dataType = "Command";
                isDouble = false;
                break;

            case "flightAction":
                dataType = "Action";
                isDouble = false;
                break;

            case "nonGPSCause":
                dataType = "GPS Cause";
                isDouble = false;
                break;

            case "connectedToRC":
                dataType = "Connection";
                isDouble = false;
                break;

            case "gpsUsed":
            case "visionUsed":
                dataType = "boolean";
                isDouble = false;
                break;

            case "Attribute|Value":
                dataType = "Key-Value Pair";
                isDouble = false;
                break;

            case "Battery:lowVoltage":
                dataType = "Low Voltage";
                isDouble = false;
                break;

            case "GPS:dateTimeStamp":
                dataType = "yyyy-mm-ddThh:mm:ssZ";
                isDouble = false;
                break;

            default:
                dataType = "N/A";
                isDouble = false;
                LOG.log(Level.WARNING, "Misc Unknown data type: {0}", colName);
        }

        if (isDouble) {
            doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(connection, colName, dataType));
        } else {
            stringTimeSeriesMap.put(colName, new StringTimeSeries(connection, colName, dataType));
        }
    }

}
