package org.ngafid.flights;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Represents Flights for the NGAFID, but for DJI drones
 */


public class DJIFlightProcessor {
    private static final Logger LOG = Logger.getLogger(DJIFlightProcessor.class.getName());

    private static final Set<String> STRING_COLS = new HashSet<>(List.of(new String[]{"flyCState", "flycCommand", "flightAction",
            "nonGPSCause", "connectedToRC", "Battery:lowVoltage", "RC:ModeSwitch", "gpsUsed", "visionUsed", "IMUEX(0):err"}));

    public static Flight processDATFile(int fleetId, String entry, InputStream stream, Connection connection) throws SQLException, CsvValidationException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
        Map<String, DoubleTimeSeries> doubleTimeSeriesMap = getDoubleTimeSeriesMap(connection);
        Map<String, StringTimeSeries> stringTimeSeriesMap = getStringTimeSeriesMap(connection);
        Map<Integer, String> indexedCols = new HashMap<>();
        Map<String, String> attributeMap = getAttributeMap(cloneInputStream(stream));


        CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(stream)));
        String[] line;
        String[] headers = reader.readNext();

        for (int i = 0; i < headers.length; i++) {
            indexedCols.put(i, headers[i]);
        }

        while ((line = reader.readNext()) != null) {
            for (int i = 0; i < line.length; i++) {
                String column = indexedCols.get(i);
                if (doubleTimeSeriesMap.containsKey(column)) {
                    DoubleTimeSeries colTimeSeries = doubleTimeSeriesMap.get(column);
                    colTimeSeries.add(Double.parseDouble(line[i]));
                } else {
                    StringTimeSeries colTimeSeries = stringTimeSeriesMap.get(column);
                    colTimeSeries.add(line[i]);
                }
            }
        }

        Flight flight = new Flight(fleetId, entry, attributeMap.get("mcID(SN)"), attributeMap.get("ACType"), doubleTimeSeriesMap, stringTimeSeriesMap, connection);
        flight.setStatus("SUCCESS"); // TODO: See if this needs to be updated
        flight.setAirframeType("UAS Rotorcraft");
        flight.setAirframeTypeID(4);

        return flight;
    }

    private static InputStream cloneInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrOStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (inputStream.read(buffer) > -1) {
            byteArrOStream.write(buffer, 0);
        }

        byteArrOStream.flush();

        return new ByteArrayInputStream(byteArrOStream.toByteArray());
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

    // TODO: Maybe find a pattern with names and datatypes to make this more manageable
    private static Map<String, DoubleTimeSeries> getDoubleTimeSeriesMap(Connection connection) throws SQLException {
        Map<String, DoubleTimeSeries> doubleTimeSeriesMap = new HashMap<>();
        doubleTimeSeriesMap.put("Tick#", new DoubleTimeSeries(connection, "Tick", "tick"));
        doubleTimeSeriesMap.put("offsetTime", new DoubleTimeSeries(connection, "Offset Time", "seconds"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):Longitude", new DoubleTimeSeries(connection, "Longitude", "degrees"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):Latitude", new DoubleTimeSeries(connection, "Latitude", "degrees"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):numSats", new DoubleTimeSeries(connection, "NumSats", "number"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):barometer:Raw", new DoubleTimeSeries(connection, "Barometer Raw", "atm"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):barometer:Smooth", new DoubleTimeSeries(connection, "Barometer Smooth", "atm"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:X", new DoubleTimeSeries(connection, "Acceleration X", "m/s^2"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:Y", new DoubleTimeSeries(connection, "Acceleration Y", "m/s^2"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:Z", new DoubleTimeSeries(connection, "Acceleration Z", "m/s^2"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:Composite", new DoubleTimeSeries(connection, "Composite Acceleration", "m/s^2"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:X", new DoubleTimeSeries(connection, "Gyro X", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:Y", new DoubleTimeSeries(connection, "Gyro Y", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:Z", new DoubleTimeSeries(connection, "Gyro Z", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:Composite", new DoubleTimeSeries(connection, "Composite Gyro", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:X", new DoubleTimeSeries(connection, "Mag X", "A/m"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:Y", new DoubleTimeSeries(connection, "Mag Y", "A/m"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:Z", new DoubleTimeSeries(connection, "Mag Z", "A/m"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:Mod", new DoubleTimeSeries(connection, "Mod Mag", "A/m"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velN", new DoubleTimeSeries(connection, "Velocity N", "m/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velD", new DoubleTimeSeries(connection, "Velocity D", "m/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velComposite", new DoubleTimeSeries(connection, "Velocity Composite", "m/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velH", new DoubleTimeSeries(connection, "Velocity H", "m/s"));

        doubleTimeSeriesMap.put("IMU_ATTI(0):GPS-H", new DoubleTimeSeries(connection, "H GPS", "ft"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):roll", new DoubleTimeSeries(connection, "Roll", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):pitch", new DoubleTimeSeries(connection, "Pitch", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):yaw", new DoubleTimeSeries(connection, "Yaw", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):yaw360", new DoubleTimeSeries(connection, "Yaw 360", "deg/s"));

        doubleTimeSeriesMap.put("IMU_ATTI(0):totalGyro:Z", new DoubleTimeSeries(connection, "Total Gyro Z", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):totalGyro:X", new DoubleTimeSeries(connection, "Total Gyro X", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):totalGyro:Y", new DoubleTimeSeries(connection, "Total Gyro Y", "deg/s"));


        doubleTimeSeriesMap.put("IMU_ATTI(0):magYaw", new DoubleTimeSeries(connection, "Mag Yaw", "deg/s"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):distanceHP", new DoubleTimeSeries(connection, "Distance HP", "ft"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):distanceTravelled", new DoubleTimeSeries(connection, "Distance Travelled", "ft"));
        doubleTimeSeriesMap.put("IMU_ATTI(0):directionOfTravel[mag]", new DoubleTimeSeries(connection, "Direction of Travel (mag)", "deg")); // Dont know about these types
        doubleTimeSeriesMap.put("IMU_ATTI(0):directionOfTravel[true]", new DoubleTimeSeries(connection, "Direction of Travel (true)", "deg"));

        doubleTimeSeriesMap.put("IMU_ATTI(0):temperature", new DoubleTimeSeries(connection, "Temperature", "Celsius"));
        doubleTimeSeriesMap.put("flightTime", new DoubleTimeSeries(connection, "Flight Time", "seconds"));
        doubleTimeSeriesMap.put("gpsHealth", new DoubleTimeSeries(connection, "GPS Health", "Health"));
        doubleTimeSeriesMap.put("General:vpsHeight", new DoubleTimeSeries(connection, "VPS Height", "ft"));
        doubleTimeSeriesMap.put("General:relativeHeight", new DoubleTimeSeries(connection, "Relative Height", "ft"));
        doubleTimeSeriesMap.put("General:absoluteHeight", new DoubleTimeSeries(connection, "Absolute Height", "ft"));
        doubleTimeSeriesMap.put("GPS(0):Long", new DoubleTimeSeries(connection, "Longitude", "degrees"));
        doubleTimeSeriesMap.put("GPS(0):Lat", new DoubleTimeSeries(connection, "Latitude", "degrees"));
        doubleTimeSeriesMap.put("GPS(0):Date", new DoubleTimeSeries(connection, "Date", "Date"));
        doubleTimeSeriesMap.put("GPS(0):Time", new DoubleTimeSeries(connection, "Time", "Time"));
        doubleTimeSeriesMap.put("GPS(0):heightMSL", new DoubleTimeSeries(connection, "MSL Height", "ft"));
        doubleTimeSeriesMap.put("GPS(0):hDOP", new DoubleTimeSeries(connection, "Horizontal Dilution of Precision", "DOP Value"));
        doubleTimeSeriesMap.put("GPS(0):pDOP", new DoubleTimeSeries(connection, "Vertical Dilution of Precision", "DOP Value"));
        doubleTimeSeriesMap.put("GPS(0):sAcc", new DoubleTimeSeries(connection, "Speed Accuracy", "cm/s"));
        doubleTimeSeriesMap.put("GPS(0):numGPS", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("GPS(0):numGLNAS", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("GPS(0):numSV", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("GPS(0):velE", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("GPS(0):velD", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("RC:Aileron", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("RC:Elevator", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("RC:Rudder", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("RC:Throttle", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("Controller:gpsLevel", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("Controller:ctrl_level", new DoubleTimeSeries(connection, "", ""));
        doubleTimeSeriesMap.put("Battery(0):cellVolts1", new DoubleTimeSeries(connection, "Battery Cell Volts 1", "Voltage"));
        doubleTimeSeriesMap.put("Battery(0):cellVolts2", new DoubleTimeSeries(connection, "Battery Cell Volts 2", "Voltage"));
        doubleTimeSeriesMap.put("Battery(0):cellVolts3", new DoubleTimeSeries(connection, "Battery Cell Volts 3", "Voltage"));
        doubleTimeSeriesMap.put("Battery(0):cellVolts4", new DoubleTimeSeries(connection, "Battery Cell Volts 4", "Voltage"));
        doubleTimeSeriesMap.put("Battery(0):cellVolts5", new DoubleTimeSeries(connection, "Battery Cell Volts 5", "Voltage"));
        doubleTimeSeriesMap.put("Battery(0):cellVolts6", new DoubleTimeSeries(connection, "Battery Cell Volts 6", "Voltage"));
        doubleTimeSeriesMap.put("Battery(0):current", new DoubleTimeSeries(connection, "Battery Current", "Amps"));
        doubleTimeSeriesMap.put("Battery(0):totalVolts", new DoubleTimeSeries(connection, "Battery Total Voltage", "Voltage"));

        doubleTimeSeriesMap.put("Battery(0):Temp", new DoubleTimeSeries(connection, "Battery Temperature", "Celsius")); // TODO: Fill params
        doubleTimeSeriesMap.put("Battery(0):battery%", new DoubleTimeSeries(connection, "Battery Percentage", "Percentage"));
        doubleTimeSeriesMap.put("Battery(0):FullChargeCap", new DoubleTimeSeries(connection, "Battery Full Charge Cap", "Capacity"));
        doubleTimeSeriesMap.put("Battery(0):RemainingCap", new DoubleTimeSeries(connection, "Battery Remaining Cap", "Capacity"));
        doubleTimeSeriesMap.put("Battery(0):voltSpread", new DoubleTimeSeries(connection, "Battery Voltage Spread", "Voltage"));

        doubleTimeSeriesMap.put("Battery(0):watts", new DoubleTimeSeries(connection, "Battery Watts", "Watts"));
        doubleTimeSeriesMap.put("Battery(0):minCurrent", new DoubleTimeSeries(connection, "Battery Minimum Current", "Amps"));
        doubleTimeSeriesMap.put("Battery(0):maxCurrent", new DoubleTimeSeries(connection, "Battery Maximum Current", "Amps"));
        doubleTimeSeriesMap.put("Battery(0):avgCurrent", new DoubleTimeSeries(connection, "Battery Average Current", "Amps"));
        doubleTimeSeriesMap.put("Battery(0):minVolts", new DoubleTimeSeries(connection, "Battery Minimum Volts", "Volts"));
        doubleTimeSeriesMap.put("Battery(0):maxVolts", new DoubleTimeSeries(connection, "Battery Maximum Volts", "Volts"));
        doubleTimeSeriesMap.put("Battery(0):avgVolts", new DoubleTimeSeries(connection, "Battery Average Volts", "Volts"));
        doubleTimeSeriesMap.put("Battery(0):minWatts", new DoubleTimeSeries(connection, "Battery Minimum Watts", "Watts"));
        doubleTimeSeriesMap.put("Battery(0):maxWatts", new DoubleTimeSeries(connection, "Battery Maximum Watts", "Watts"));
        doubleTimeSeriesMap.put("Battery(0):avgWatts", new DoubleTimeSeries(connection, "Battery Average Watts", "Watts"));

        doubleTimeSeriesMap.put("SMART_BATT:goHome%", new DoubleTimeSeries(connection, "Smart Battery Go Home Percentage", "percentage"));
        doubleTimeSeriesMap.put("SMART_BATT:land%", new DoubleTimeSeries(connection, "Smart Battery Land Percentage", "percentage"));
        doubleTimeSeriesMap.put("SMART_BATT:goHomeTime", new DoubleTimeSeries(connection, "Smart Battery Go Home Time", "seconds"));
        doubleTimeSeriesMap.put("SMART_BATT:landTime", new DoubleTimeSeries(connection, "Smart Battery Land Time", "seconds"));

        doubleTimeSeriesMap.put("Motor:Status:RFront", new DoubleTimeSeries(connection, "Right Front Motor Status", "Status Number"));
        doubleTimeSeriesMap.put("Motor:Status:LFront", new DoubleTimeSeries(connection, "Left Front Motor Status", "Status Number"));
        doubleTimeSeriesMap.put("Motor:Status:LSide", new DoubleTimeSeries(connection, "Left Side Motor Status", "Status Number"));
        doubleTimeSeriesMap.put("Motor:Status:LBack", new DoubleTimeSeries(connection, "Left Back Motor Status", "Status Number"));
        doubleTimeSeriesMap.put("Motor:Status:RBack", new DoubleTimeSeries(connection, "Right Back Motor Status", "Status Number"));
        doubleTimeSeriesMap.put("Motor:Status:RSide", new DoubleTimeSeries(connection, "Right Side Motor Status", "Status Number"));

        doubleTimeSeriesMap.put("Motor:Speed:RFront", new DoubleTimeSeries(connection, "Right Front Motor Speed", "m/s"));
        doubleTimeSeriesMap.put("Motor:Speed:LFront", new DoubleTimeSeries(connection, "Left Front Motor Speed", "m/s"));
        doubleTimeSeriesMap.put("Motor:Speed:LSide", new DoubleTimeSeries(connection, "Left Side Motor Speed", "m/s"));
        doubleTimeSeriesMap.put("Motor:Speed:LBack", new DoubleTimeSeries(connection, "Left Side Motor Speed", "m/s"));
        doubleTimeSeriesMap.put("Motor:Speed:RBack", new DoubleTimeSeries(connection, "Right Back Motor Speed", "m/s"));
        doubleTimeSeriesMap.put("Motor:Speed:RSide", new DoubleTimeSeries(connection, "Right Side Motor Speed", "m/s"));

        doubleTimeSeriesMap.put("Motor:Volts:RFront", new DoubleTimeSeries(connection, "Right Front Motor Voltage", "Volts"));
        doubleTimeSeriesMap.put("Motor:Volts:LFront", new DoubleTimeSeries(connection, "Left Front Motor Voltage", "Volts"));
        doubleTimeSeriesMap.put("Motor:Volts:LSide", new DoubleTimeSeries(connection, "Left Side Motor Voltage", "Volts"));
        doubleTimeSeriesMap.put("Motor:Volts:LBack", new DoubleTimeSeries(connection, "Left Back Motor Voltage", "Volts"));
        doubleTimeSeriesMap.put("Motor:Volts:RBack", new DoubleTimeSeries(connection, "Right Back Motor Voltage", "Volts"));
        doubleTimeSeriesMap.put("Motor:Volts:RSide", new DoubleTimeSeries(connection, "Right Side Motor Voltage", "Volts"));

        doubleTimeSeriesMap.put("Motor:EscTemp:RFront", new DoubleTimeSeries(connection, "Right Front Motor Temperature", "Celsius"));
        doubleTimeSeriesMap.put("Motor:EscTemp:LFront", new DoubleTimeSeries(connection, "Left Front Motor Temperature", "Celsius"));
        doubleTimeSeriesMap.put("Motor:EscTemp:LSide", new DoubleTimeSeries(connection, "Left Side Motor Temperature", "Celsius"));
        doubleTimeSeriesMap.put("Motor:EscTemp:LBack", new DoubleTimeSeries(connection, "Left Back Motor Temperature", "Celsius"));
        doubleTimeSeriesMap.put("Motor:EscTemp:RBack", new DoubleTimeSeries(connection, "Right Back Motor Temperature", "Celsius"));
        doubleTimeSeriesMap.put("Motor:EscTemp:RSide", new DoubleTimeSeries(connection, "Right Side Motor Temperature", "Celsius"));


        doubleTimeSeriesMap.put("Motor:PPMrecv:RFront", new DoubleTimeSeries(connection, "Right Front Motor Stop Command", "Stop Command"));
        doubleTimeSeriesMap.put("Motor:PPMrecv:LFront", new DoubleTimeSeries(connection, "Left Front Motor Stop Command", "Stop Command"));
        doubleTimeSeriesMap.put("Motor:PPMrecv:LSide", new DoubleTimeSeries(connection, "Left Side Motor Stop Command", "Stop Command"));
        doubleTimeSeriesMap.put("Motor:PPMrecv:LBack", new DoubleTimeSeries(connection, "Left Back Motor Stop Command", "Stop Command"));
        doubleTimeSeriesMap.put("Motor:PPMrecv:RBack", new DoubleTimeSeries(connection, "Right Back Motor Stop Command", "Stop Command"));
        doubleTimeSeriesMap.put("Motor:PPMrecv:RSide", new DoubleTimeSeries(connection, "Right Side Motor Stop Command", "Stop Command"));

        doubleTimeSeriesMap.put("Motor:V_out:RFront", new DoubleTimeSeries(connection, "Right Front Motor ", ""));
        doubleTimeSeriesMap.put("Motor:V_out:LFront", new DoubleTimeSeries(connection, "Left Front Motor Voltage Out", "Voltage"));
        doubleTimeSeriesMap.put("Motor:V_out:LSide", new DoubleTimeSeries(connection, "Left Side Motor Voltage Out", "Voltage"));
        doubleTimeSeriesMap.put("Motor:V_out:LBack", new DoubleTimeSeries(connection, "Left Back Motor Voltage Out", "Voltage"));
        doubleTimeSeriesMap.put("Motor:V_out:RBack", new DoubleTimeSeries(connection, "Right Back Motor Voltage Out", "Voltage"));
        doubleTimeSeriesMap.put("Motor:V_out:RSide", new DoubleTimeSeries(connection, "Right Side Motor Voltage Out", "Voltage"));

        doubleTimeSeriesMap.put("Motor:Current:RFront", new DoubleTimeSeries(connection, "Right Front Motor Current", "Amps"));
        doubleTimeSeriesMap.put("Motor:Current:LFront", new DoubleTimeSeries(connection, "Left Front Motor Current", "Amps"));
        doubleTimeSeriesMap.put("Motor:Current:LSide", new DoubleTimeSeries(connection, "Left Side Motor Current", "Amps"));
        doubleTimeSeriesMap.put("Motor:Current:LBack", new DoubleTimeSeries(connection, "Left Back Motor Current", "Amps"));
        doubleTimeSeriesMap.put("Motor:Current:RBack", new DoubleTimeSeries(connection, "Right Back Motor Current", "Amps"));
        doubleTimeSeriesMap.put("Motor:Current:RSide", new DoubleTimeSeries(connection, "Right Side Motor Current", "Amps"));


        doubleTimeSeriesMap.put("AirComp:AirSpeedBody:X", new DoubleTimeSeries(connection, "Airspeed Body X", ""));
        doubleTimeSeriesMap.put("AirComp:AirSpeedBody:Y", new DoubleTimeSeries(connection, "Airspeed Body Y", ""));
        doubleTimeSeriesMap.put("AirComp:Alti", new DoubleTimeSeries(connection, "Airspeed Altitude ", ""));
        doubleTimeSeriesMap.put("AirComp:VelNorm", new DoubleTimeSeries(connection, "Airspeed Norm Velocity", ""));
        doubleTimeSeriesMap.put("AirComp:AirSpeedGround:X", new DoubleTimeSeries(connection, "Airspeed Ground X", ""));
        doubleTimeSeriesMap.put("AirComp:AirSpeedGround:Y", new DoubleTimeSeries(connection, "Airspeed Ground Y", ""));
        doubleTimeSeriesMap.put("AirComp:VelLevel", new DoubleTimeSeries(connection, "Airspeed Level Velocity", ""));

        doubleTimeSeriesMap.put("IMUEX(0):rtk_Longitude", new DoubleTimeSeries(connection, "RTK Longitude", "degrees"));
        doubleTimeSeriesMap.put("IMUEX(0):rtk_Latitude", new DoubleTimeSeries(connection, "RTK Latitude", "degrees"));
        doubleTimeSeriesMap.put("IMUEX(0):rtk_Alti", new DoubleTimeSeries(connection, "RTK Altitude", "feet"));

        return doubleTimeSeriesMap;
    }

    private static Map<String, StringTimeSeries> getStringTimeSeriesMap(Connection connection) throws SQLException {
        Map<String, StringTimeSeries> stringTimeSeriesMap = new HashMap<>();
        stringTimeSeriesMap.put("GPS(0):dateTimeStamp", new StringTimeSeries(connection, "", "yyyy-mm-ddThh:mm:ssZ"));
        stringTimeSeriesMap.put("localDateSeries", new StringTimeSeries(connection, "Lcl Date", "yyyy-mm-dd"));
        stringTimeSeriesMap.put("localTimeSeries", new StringTimeSeries(connection, "Lcl Time", "hh:mm:ss"));

        stringTimeSeriesMap.put("flyCState", new StringTimeSeries(connection, "Flight CState", "CState"));
        stringTimeSeriesMap.put("flyCommand", new StringTimeSeries(connection, "Flight Command", "Command"));
        stringTimeSeriesMap.put("flightAction", new StringTimeSeries(connection, "Flight Action", "Action"));
        stringTimeSeriesMap.put("nonGPSCause", new StringTimeSeries(connection, "Non GPS Cause", "GPS Cause"));
        stringTimeSeriesMap.put("connectedToRC", new StringTimeSeries(connection, "Connected To RC", "Connection"));
        stringTimeSeriesMap.put("Battery:lowVoltage", new StringTimeSeries(connection, "Battery:lowVoltage", "")); // Unknown. Does not appear in data
        stringTimeSeriesMap.put("RC:ModeSwitch", new StringTimeSeries(connection, "RC Mode Switch", "Mode")); // Unknown. Just shows P
        stringTimeSeriesMap.put("gpsUsed", new StringTimeSeries(connection, "GPS Used", "boolean"));
        stringTimeSeriesMap.put("visionUsed", new StringTimeSeries(connection, "Vision Used", "boolean"));
        stringTimeSeriesMap.put("IMUEX(0):err", new StringTimeSeries(connection, "IMUEX Error", "error"));
        stringTimeSeriesMap.put("Attribute|Value", new StringTimeSeries(connection, "Attribute|Value", "Key-Value Pair"));

        return stringTimeSeriesMap;
    }
}
