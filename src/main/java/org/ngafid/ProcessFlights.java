package org.ngafid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.MalformedFlightFileException;

import org.ngafid.flights.Flight;

public class ProcessFlights {
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

    public static void processFlightFile(String filename) {
        try {
            Flight flight = new Flight(filename);
            flight.calculateAGL("AltAGL", "AltMSL", "Latitude", "Longitude");
            flight.calculateAirportProximity("Latitude", "Longitude");
            flight.calculateStartEndTime("Lcl Date", "Lcl Time");

            flight.printValues(new String[]{
                "Latitude",
                    "Longitude",
                    "AltAGL",
                    "NearestAirport",
                    "AirportDistance",
                    "NearestRunway",
                    "RunwayDistance"
            });
        } catch (MalformedFlightFileException e) {
            System.err.println("Could not parse flight file '" + filename + "'");
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void processZipFile(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if (entry.isDirectory()) {
                //System.err.println("SKIPPING: " + entry.getName());
                continue;
            }

            if (name.contains("__MACOSX")) {
                //System.err.println("SKIPPING: " + entry.getName());
                continue;
            }

            System.err.println("PROCESSING: " + name);

            if (entry.getName().contains(".csv")) {
                try {
                    InputStream stream = zipFile.getInputStream(entry);
                    Flight flight = new Flight(entry.getName(), stream);

                    if (connection != null) {
                        flight.updateDatabase(connection, 1, 1, 1);
                    }
                } catch (IOException e) {
                    System.err.println("ERROR: processing ZipEntry '" + name + "' from zip file: '" + zipFile.getName() + "'");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    public static void main(String[] arguments) throws Exception {
        // We need to provide file path as the parameter:
        // double backquote is to avoid compiler interpret words
        // like \test as \t (ie. as a escape sequence)

        System.out.println("Command Line Arguments:");
        for (int i = 0; i < arguments.length; i++) {
            System.out.println("arguments[" + i + "]: '" + arguments[i] + "'");
        }

        if (arguments.length != 1) {
            System.err.println("Incorrect arguments, usage:");
            System.err.println("java org.ngafid.ProcessFlights <flight csv/zip>");
            System.exit(1);
        }


        //The first argument should be the directory we're going to parse
        //For each subdirectory:
        //  get the airframe type (C172, C182, PA28, etc.)
        //  for each N-Number in that directory:
        //      for each flight in that directory:
        //          create the appropriate airframe object (C172 for C172s)
        //          get events
        //          insert the events and flight information into the database

        String filename = arguments[0];
        String extension = filename.substring(filename.length() - 4);
        System.err.println("extension: '" + extension + "'");
        if (extension.equals(".csv")) {
            System.err.println("processing flight file!");
            processFlightFile(filename);
        } else if (extension.equals(".zip")) {
            System.err.println("processing zip file!");
            processZipFile(new ZipFile(filename));
        }
    }
}
