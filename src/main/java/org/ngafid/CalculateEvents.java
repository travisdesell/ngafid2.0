package org.ngafid;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.MalformedFlightFileException;

import org.ngafid.events2.PitchEvent;
import org.ngafid.events2.RollEvent;
import org.ngafid.events2.LateralAccelerationEvent;
import org.ngafid.events2.LongitudinalAccelerationEvent;
import org.ngafid.events2.VerticalAccelerationEvent;
import org.ngafid.events2.VsiOnFinalEvent;

import org.ngafid.events2.c172Cessna.C172HighAltitudeEvent;
import org.ngafid.events2.c172Cessna.C172HighCHTEvent;
import org.ngafid.events2.c172Cessna.C172IndicatedAirspeedEvent;
import org.ngafid.events2.c172Cessna.C172LowAirspeedOnApproachEvent;
import org.ngafid.events2.c172Cessna.C172LowAirspeedOnClimboutEvent;
import org.ngafid.events2.c172Cessna.C172LowFuelEvent;
import org.ngafid.events2.c172Cessna.C172LowOilPressureEvent;

import org.ngafid.events2.c182Cessna.C182HighAltitudeEvent;
import org.ngafid.events2.c182Cessna.C182HighCHTEvent;
import org.ngafid.events2.c182Cessna.C182IndicatedAirspeedEvent;
import org.ngafid.events2.c182Cessna.C182LowAirspeedOnApproachEvent;
import org.ngafid.events2.c182Cessna.C182LowAirspeedOnClimboutEvent;
import org.ngafid.events2.c182Cessna.C182LowFuelEvent;
import org.ngafid.events2.c182Cessna.C182LowOilPressureEvent;

import org.ngafid.events2.pa28Piper.PA28HighAltitudeEvent;
import org.ngafid.events2.pa28Piper.PA28HighCHTEvent;
import org.ngafid.events2.pa28Piper.PA28IndicatedAirspeedEvent;
import org.ngafid.events2.pa28Piper.PA28LowAirspeedOnApproachEvent;
import org.ngafid.events2.pa28Piper.PA28LowAirspeedOnClimboutEvent;
import org.ngafid.events2.pa28Piper.PA28LowFuelEvent;
import org.ngafid.events2.pa28Piper.PA28LowOilPressureEvent;

import org.ngafid.events2.pa44Piper.PA44HighAltitudeEvent;
import org.ngafid.events2.pa44Piper.PA44HighCHTEvent;
import org.ngafid.events2.pa44Piper.PA44IndicatedAirspeedEvent;
import org.ngafid.events2.pa44Piper.PA44LowAirspeedOnApproachEvent;
import org.ngafid.events2.pa44Piper.PA44LowAirspeedOnClimboutEvent;
import org.ngafid.events2.pa44Piper.PA44LowFuelEvent;
import org.ngafid.events2.pa44Piper.PA44LowOilPressureEvent;

import org.ngafid.events2.sr20Cirrus.SR20HighAltitudeEvent;
import org.ngafid.events2.sr20Cirrus.SR20HighCHTEvent;
import org.ngafid.events2.sr20Cirrus.SR20IndicatedAirspeedEvent;
import org.ngafid.events2.sr20Cirrus.SR20LowAirspeedOnApproachEvent;
import org.ngafid.events2.sr20Cirrus.SR20LowAirspeedOnClimboutEvent;
import org.ngafid.events2.sr20Cirrus.SR20LowFuelEvent;
import org.ngafid.events2.sr20Cirrus.SR20LowOilPressureEvent;

import org.ngafid.flights.Flight;

public class CalculateEvents {
    private static Connection connection;

    static {
        if (System.getenv("NGAFID_DB_INFO") == null) {
            System.err.println("ERROR: 'NGAFID_DB_INFO' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_DB_INFO=<path/to/db_info_file>");
            System.exit(1);
        }
        String NGAFID_DB_INFO = System.getenv("NGAFID_DB_INFO");

        String dbHost = "";
        String dbName = "";
        String dbUser = "";
        String dbPassword = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(NGAFID_DB_INFO));
            bufferedReader.readLine();

            dbUser = bufferedReader.readLine();
            dbUser = dbUser.substring(dbUser.indexOf("'") + 1);
            dbUser = dbUser.substring(0, dbUser.indexOf("'"));

            dbName = bufferedReader.readLine();
            dbName = dbName.substring(dbName.indexOf("'") + 1);
            dbName = dbName.substring(0, dbName.indexOf("'"));

            dbHost = bufferedReader.readLine();
            dbHost = dbHost.substring(dbHost.indexOf("'") + 1);
            dbHost = dbHost.substring(0, dbHost.indexOf("'"));

            dbPassword = bufferedReader.readLine();
            dbPassword = dbPassword.substring(dbPassword.indexOf("'") + 1);
            dbPassword = dbPassword.substring(0, dbPassword.indexOf("'"));

            System.out.println("dbHost: '" + dbHost + "'");
            System.out.println("dbName: '" + dbName + "'");
            System.out.println("dbUser: '" + dbUser + "'");
            System.out.println("dbPassword: '" + dbPassword + "'");

        } catch (IOException e) {
            System.err.println("Error reading from NGAFID_DB_INFO: '" + NGAFID_DB_INFO + "'");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName, dbUser, dbPassword);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] arguments) throws Exception {
        // We need to provide file path as the parameter:
        // double backquote is to avoid compiler interpret words
        // like \test as \t (ie. as a escape sequence)

        PitchEvent.calculateEvents(connection);
        RollEvent.calculateEvents(connection);
        LateralAccelerationEvent.calculateEvents(connection);
        LongitudinalAccelerationEvent.calculateEvents(connection);
        VerticalAccelerationEvent.calculateEvents(connection);
        VsiOnFinalEvent.calculateEvents(connection);

        C172HighAltitudeEvent.calculateEvents(connection);
        C172HighCHTEvent.calculateEvents(connection);
        C172IndicatedAirspeedEvent.calculateEvents(connection);
        C172LowAirspeedOnApproachEvent.calculateEvents(connection);
        C172LowAirspeedOnClimboutEvent.calculateEvents(connection);
        C172LowFuelEvent.calculateEvents(connection);
        C172LowOilPressureEvent.calculateEvents(connection);

        C182HighAltitudeEvent.calculateEvents(connection);
        C182HighCHTEvent.calculateEvents(connection);
        C182IndicatedAirspeedEvent.calculateEvents(connection);
        C182LowAirspeedOnApproachEvent.calculateEvents(connection);
        C182LowAirspeedOnClimboutEvent.calculateEvents(connection);
        C182LowFuelEvent.calculateEvents(connection);
        C182LowOilPressureEvent.calculateEvents(connection);

        PA28HighAltitudeEvent.calculateEvents(connection);
        PA28HighCHTEvent.calculateEvents(connection);
        PA28IndicatedAirspeedEvent.calculateEvents(connection);
        PA28LowAirspeedOnApproachEvent.calculateEvents(connection);
        PA28LowAirspeedOnClimboutEvent.calculateEvents(connection);
        PA28LowFuelEvent.calculateEvents(connection);
        PA28LowOilPressureEvent.calculateEvents(connection);

        PA44HighAltitudeEvent.calculateEvents(connection);
        PA44HighCHTEvent.calculateEvents(connection);
        PA44IndicatedAirspeedEvent.calculateEvents(connection);
        PA44LowAirspeedOnApproachEvent.calculateEvents(connection);
        PA44LowAirspeedOnClimboutEvent.calculateEvents(connection);
        PA44LowFuelEvent.calculateEvents(connection);
        PA44LowOilPressureEvent.calculateEvents(connection);

        SR20HighAltitudeEvent.calculateEvents(connection);
        SR20HighCHTEvent.calculateEvents(connection);
        SR20IndicatedAirspeedEvent.calculateEvents(connection);
        SR20LowAirspeedOnApproachEvent.calculateEvents(connection);
        SR20LowAirspeedOnClimboutEvent.calculateEvents(connection);
        SR20LowFuelEvent.calculateEvents(connection);
        SR20LowOilPressureEvent.calculateEvents(connection);

    }
}
