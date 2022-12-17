package org.ngafid.flights;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Represents Flights for the NGAFID, but for DJI drones
 */


public class DJIFlightProcessor {
    private static final Set<String> STRING_COLS = new HashSet<>(List.of(new String[]{"flyCState", "flycCommand", "flightAction",
            "nonGPSCause", "connectedToRC", "Battery:lowVoltage", "RC:ModeSwitch", "gpsUsed", "visionUsed", "IMUEX(0):err"}));

    public static Flight processDATFile(int fleetId, String entry, InputStream stream, Connection connection) throws SQLException, CsvValidationException, IOException {
        int len = -1; // TODO: Get length
        Map<String, DoubleTimeSeries> doubleTimeSeriesMap = new HashMap<>();
        doubleTimeSeriesMap.put("Tick#", new DoubleTimeSeries(connection, "Tick", "tick", len));
        doubleTimeSeriesMap.put("offsetTime", new DoubleTimeSeries(connection, "Offset Time", "seconds", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):Longitude", new DoubleTimeSeries(connection, "Longitude", "degrees", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):Latitude", new DoubleTimeSeries(connection, "Latitude", "degrees", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):numSats", new DoubleTimeSeries(connection, "NumSats", "number", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):barometer:Raw", new DoubleTimeSeries(connection, "Barometer Raw", "Nu", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):barometer:Smooth", new DoubleTimeSeries(connection, "Barometer Smooth", "tick", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:X", new DoubleTimeSeries(connection, "Acceleration X", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:Y", new DoubleTimeSeries(connection, "Acceleration Y", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:Z", new DoubleTimeSeries(connection, "Acceleration Z", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):accel:Composite", new DoubleTimeSeries(connection, "Composite Acceleration", "", len)); // TODO: Figure out datatype
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:X", new DoubleTimeSeries(connection, "Gyro X", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:Y", new DoubleTimeSeries(connection, "Gyro Y", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:Z", new DoubleTimeSeries(connection, "Gyro Z", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):gyro:Composite", new DoubleTimeSeries(connection, "Composite Gyro", "", len)); // TODO: Figure out datatype
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:X", new DoubleTimeSeries(connection, "Mag X", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:Y", new DoubleTimeSeries(connection, "Mag Y", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:Z", new DoubleTimeSeries(connection, "Mag Z", "m/s^2", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):mag:Mod", new DoubleTimeSeries(connection, "Mod Mag", "", len)); // TODO: Figure out datatype
        doubleTimeSeriesMap.put("IMU_ATTI(0):velN", new DoubleTimeSeries(connection, "Velocity N", "m/s", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velD", new DoubleTimeSeries(connection, "Velocity D", "m/s", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velComposite", new DoubleTimeSeries(connection, "Velocity Composite", "m/s", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):velH", new DoubleTimeSeries(connection, "Velocity H", "m/s", len));

        doubleTimeSeriesMap.put("IMU_ATTI(0):GPS-H", new DoubleTimeSeries(connection, "H GPS", "m/s", len)); // TODO: Figure out datatype
        doubleTimeSeriesMap.put("IMU_ATTI(0):roll", new DoubleTimeSeries(connection, "Roll", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):pitch", new DoubleTimeSeries(connection, "Pitch", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):yaw", new DoubleTimeSeries(connection, "Yaw", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):yaw360", new DoubleTimeSeries(connection, "Yaw 360", "", len));

        doubleTimeSeriesMap.put("IMU_ATTI(0):totalGyro:Z", new DoubleTimeSeries(connection, "Total Gyro Z", "", len)); // TODO: You know what to do
        doubleTimeSeriesMap.put("IMU_ATTI(0):totalGyro:X", new DoubleTimeSeries(connection, "Total Gyro X", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):totalGyro:Y", new DoubleTimeSeries(connection, "Total Gyro Y", "", len));


        doubleTimeSeriesMap.put("IMU_ATTI(0):magYaw", new DoubleTimeSeries(connection, "Mag Yaw", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):distanceHP", new DoubleTimeSeries(connection, "Distance HP", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):distanceTravelled", new DoubleTimeSeries(connection, "Distance Travelled", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):directionOfTravel[mag]", new DoubleTimeSeries(connection, "Direction of Travel (mag)", "", len));
        doubleTimeSeriesMap.put("IMU_ATTI(0):directionOfTravel[true]", new DoubleTimeSeries(connection, "Direction of Travel (true)", "", len));

        doubleTimeSeriesMap.put("IMU_ATTI(0):temperature", new DoubleTimeSeries(connection, "Temperature", "Celsius", len));
        doubleTimeSeriesMap.put("flightTime", new DoubleTimeSeries(connection, "Flight Time", "seconds", len));
        doubleTimeSeriesMap.put("gpsHealth", new DoubleTimeSeries(connection, "GPS Health", "Health", len));
        doubleTimeSeriesMap.put("General:vpsHeight", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("General:relativeHeight", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("General:absoluteHeight", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):Long", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):Lat", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):Date", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):Time", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):dateTimeStamp", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):heightMSL", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):hDOP", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):pDOP", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):sAcc", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):numGPS", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):numGLNAS", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):numSV", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):velE", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("GPS(0):velD", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("RC:Aileron", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("RC:Elevator", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("RC:Rudder", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("RC:Throttle", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("Controller:gpsLevel", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("Controller:ctrl_level", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("Battery(0):cellVolts1", new DoubleTimeSeries(connection, "Battery Cell Volts 1", "Voltage", len));
        doubleTimeSeriesMap.put("Battery(0):cellVolts2", new DoubleTimeSeries(connection, "Battery Cell Volts 2", "Voltage", len));
        doubleTimeSeriesMap.put("Battery(0):cellVolts3", new DoubleTimeSeries(connection, "Battery Cell Volts 3", "Voltage", len));
        doubleTimeSeriesMap.put("Battery(0):cellVolts4", new DoubleTimeSeries(connection, "Battery Cell Volts 4", "Voltage", len));
        doubleTimeSeriesMap.put("Battery(0):cellVolts5", new DoubleTimeSeries(connection, "Battery Cell Volts 5", "Voltage", len));
        doubleTimeSeriesMap.put("Battery(0):cellVolts6", new DoubleTimeSeries(connection, "Battery Cell Volts 6", "Voltage", len));
        doubleTimeSeriesMap.put("Battery(0):current", new DoubleTimeSeries(connection, "Battery Current", "Amps", len));
        doubleTimeSeriesMap.put("Battery(0):totalVolts", new DoubleTimeSeries(connection, "Battery Total Voltage", "Voltage", len));

        doubleTimeSeriesMap.put("Battery(0):Temp", new DoubleTimeSeries(connection, "", "", len)); // TODO: Fill params
        doubleTimeSeriesMap.put("Battery(0):battery%", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("Battery(0):FullChargeCap", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("Battery(0):RemainingCap", new DoubleTimeSeries(connection, "", "", len));
        doubleTimeSeriesMap.put("Battery(0):voltSpread", new DoubleTimeSeries(connection, "", "", len));

        doubleTimeSeriesMap.put("Battery(0):watts", new DoubleTimeSeries(connection, "Battery Watts", "Watts", len));
        doubleTimeSeriesMap.put("Battery(0):minCurrent", new DoubleTimeSeries(connection, "Battery Minimum Current", "Amps", len));
        doubleTimeSeriesMap.put("Battery(0):maxCurrent", new DoubleTimeSeries(connection, "Battery Maximum Current", "Amps", len));
        doubleTimeSeriesMap.put("Battery(0):avgCurrent", new DoubleTimeSeries(connection, "Battery Average Current", "Amps", len));
        doubleTimeSeriesMap.put("Battery(0):minVolts", new DoubleTimeSeries(connection, "Battery Minimum Volts", "Volts", len));
        doubleTimeSeriesMap.put("Battery(0):maxVolts", new DoubleTimeSeries(connection, "Battery Maximum Volts", "Volts", len));
        doubleTimeSeriesMap.put("Battery(0):avgVolts", new DoubleTimeSeries(connection, "Battery Average Volts", "Volts", len));
        doubleTimeSeriesMap.put("Battery(0):minWatts", new DoubleTimeSeries(connection, "Battery Minimum Watts", "Watts", len));
        doubleTimeSeriesMap.put("Battery(0):maxWatts", new DoubleTimeSeries(connection, "Battery Maximum Watts", "Watts", len));
        doubleTimeSeriesMap.put("Battery(0):avgWatts", new DoubleTimeSeries(connection, "Battery Average Watts", "Watts", len));

        doubleTimeSeriesMap.put("SMART_BATT:goHome%", new DoubleTimeSeries(connection, "Smart Battery Go Home Percentage", "percentage", len));
        doubleTimeSeriesMap.put("SMART_BATT:land%", new DoubleTimeSeries(connection, "Smart Battery Land Percentage", "percentage", len));
        doubleTimeSeriesMap.put("SMART_BATT:goHomeTime", new DoubleTimeSeries(connection, "Smart Battery Go Home Time", "seconds", len));
        doubleTimeSeriesMap.put("SMART_BATT:landTime", new DoubleTimeSeries(connection, "Smart Battery Land Time", "seconds", len));

        doubleTimeSeriesMap.put("Motor:Status:RFront", new DoubleTimeSeries(connection, "Right Front Motor Status", "Status Number", len));
        doubleTimeSeriesMap.put("Motor:Status:LFront", new DoubleTimeSeries(connection, "Left Front Motor Status", "Status Number", len));
        doubleTimeSeriesMap.put("Motor:Status:LSide", new DoubleTimeSeries(connection, "Left Side Motor Status", "Status Number", len));
        doubleTimeSeriesMap.put("Motor:Status:LBack", new DoubleTimeSeries(connection, "Left Back Motor Status", "Status Number", len));
        doubleTimeSeriesMap.put("Motor:Status:RBack", new DoubleTimeSeries(connection, "Right Back Motor Status", "Status Number", len));
        doubleTimeSeriesMap.put("Motor:Status:RSide", new DoubleTimeSeries(connection, "Right Side Motor Status", "Status Number", len));

        doubleTimeSeriesMap.put("Motor:Speed:RFront", new DoubleTimeSeries(connection, "Right Front Motor Speed", "m/s", len));
        doubleTimeSeriesMap.put("Motor:Speed:LFront", new DoubleTimeSeries(connection, "Left Front Motor Speed", "m/s", len));
        doubleTimeSeriesMap.put("Motor:Speed:LSide", new DoubleTimeSeries(connection, "Left Side Motor Speed", "m/s", len));
        doubleTimeSeriesMap.put("Motor:Speed:LBack", new DoubleTimeSeries(connection, "Left Side Motor Speed", "m/s", len));
        doubleTimeSeriesMap.put("Motor:Speed:RBack", new DoubleTimeSeries(connection, "Right Back Motor Speed", "m/s", len));
        doubleTimeSeriesMap.put("Motor:Speed:RSide", new DoubleTimeSeries(connection, "Right Side Motor Speed", "m/s", len));

        doubleTimeSeriesMap.put("Motor:Volts:RFront", new DoubleTimeSeries(connection, "Right Front Motor Voltage", "Volts", len));
        doubleTimeSeriesMap.put("Motor:Volts:LFront", new DoubleTimeSeries(connection, "Left Front Motor Voltage", "Volts", len));
        doubleTimeSeriesMap.put("Motor:Volts:LSide", new DoubleTimeSeries(connection, "Left Side Motor Voltage", "Volts", len));
        doubleTimeSeriesMap.put("Motor:Volts:LBack", new DoubleTimeSeries(connection, "Left Back Motor Voltage", "Volts", len));
        doubleTimeSeriesMap.put("Motor:Volts:RBack", new DoubleTimeSeries(connection, "Right Back Motor Voltage", "Volts", len));
        doubleTimeSeriesMap.put("Motor:Volts:RSide", new DoubleTimeSeries(connection, "Right Side Motor Voltage", "Volts", len));

        doubleTimeSeriesMap.put("Motor:EscTemp:RFront", new DoubleTimeSeries(connection, "Right Front Motor Temperature", "Celsius", len));
        doubleTimeSeriesMap.put("Motor:EscTemp:LFront", new DoubleTimeSeries(connection, "Left Front Motor Temperature", "Celsius", len));
        doubleTimeSeriesMap.put("Motor:EscTemp:LSide", new DoubleTimeSeries(connection, "Left Side Motor Temperature", "Celsius", len));
        doubleTimeSeriesMap.put("Motor:EscTemp:LBack", new DoubleTimeSeries(connection, "Left Back Motor Temperature", "Celsius", len));
        doubleTimeSeriesMap.put("Motor:EscTemp:RBack", new DoubleTimeSeries(connection, "Right Back Motor Temperature", "Celsius", len));
        doubleTimeSeriesMap.put("Motor:EscTemp:RSide", new DoubleTimeSeries(connection, "Right Side Motor Temperature", "Celsius", len));


        doubleTimeSeriesMap.put("Motor:PPMrecv:RFront", new DoubleTimeSeries(connection, "Right Front Motor Stop Command", "Stop Command", len));
        doubleTimeSeriesMap.put("Motor:PPMrecv:LFront", new DoubleTimeSeries(connection, "Left Front Motor Stop Command", "Stop Command", len));
        doubleTimeSeriesMap.put("Motor:PPMrecv:LSide", new DoubleTimeSeries(connection, "Left Side Motor Stop Command", "Stop Command", len));
        doubleTimeSeriesMap.put("Motor:PPMrecv:LBack", new DoubleTimeSeries(connection, "Left Back Motor Stop Command", "Stop Command", len));
        doubleTimeSeriesMap.put("Motor:PPMrecv:RBack", new DoubleTimeSeries(connection, "Right Back Motor Stop Command", "Stop Command", len));
        doubleTimeSeriesMap.put("Motor:PPMrecv:RSide", new DoubleTimeSeries(connection, "Right Side Motor Stop Command", "Stop Command", len));

        doubleTimeSeriesMap.put("Motor:V_out:RFront", new DoubleTimeSeries(connection, "Right Front Motor ", "", len));
        doubleTimeSeriesMap.put("Motor:V_out:LFront", new DoubleTimeSeries(connection, "Left Front Motor Voltage Out", "Voltage", len));
        doubleTimeSeriesMap.put("Motor:V_out:LSide", new DoubleTimeSeries(connection, "Left Side Motor Voltage Out", "Voltage", len));
        doubleTimeSeriesMap.put("Motor:V_out:LBack", new DoubleTimeSeries(connection, "Left Back Motor Voltage Out", "Voltage", len));
        doubleTimeSeriesMap.put("Motor:V_out:RBack", new DoubleTimeSeries(connection, "Right Back Motor Voltage Out", "Voltage", len));
        doubleTimeSeriesMap.put("Motor:V_out:RSide", new DoubleTimeSeries(connection, "Right Side Motor Voltage Out", "Voltage", len));

        doubleTimeSeriesMap.put("Motor:Current:RFront", new DoubleTimeSeries(connection, "Right Front Motor Current", "Amps", len));
        doubleTimeSeriesMap.put("Motor:Current:LFront", new DoubleTimeSeries(connection, "Left Front Motor Current", "Amps", len));
        doubleTimeSeriesMap.put("Motor:Current:LSide", new DoubleTimeSeries(connection, "Left Side Motor Current", "Amps", len));
        doubleTimeSeriesMap.put("Motor:Current:LBack", new DoubleTimeSeries(connection, "Left Back Motor Current", "Amps", len));
        doubleTimeSeriesMap.put("Motor:Current:RBack", new DoubleTimeSeries(connection, "Right Back Motor Current", "Amps", len));
        doubleTimeSeriesMap.put("Motor:Current:RSide", new DoubleTimeSeries(connection, "Right Side Motor Current", "Amps", len));



        doubleTimeSeriesMap.put("", new DoubleTimeSeries(connection, "", "", len));


        CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(stream)));
        String[] line;
        String[] headers = reader.readNext();
        for (String header : headers) {
            if (STRING_COLS.contains(header)) {
                stringTimeSeriesMap.put(header, new StringTimeSeries(header));
            } else {
                doubleTimeSeriesMap.put(header, new DoubleTimeSeries(header));
            }
        }


        return new Flight(null, null); // TODO: Update this later
    }
}
