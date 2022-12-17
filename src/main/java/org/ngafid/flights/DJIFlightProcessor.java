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
