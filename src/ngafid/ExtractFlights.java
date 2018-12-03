package org.ngafid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FatalFlightFileException;

import org.ngafid.flights.Flight;

public class ExtractFlights {

    public static void main(String[] arguments) throws Exception {
        String filename = "/Users/travisdesell/Data/ngafid/und_single_week/C172/one_week_c172.zip";

        System.err.println("processing zip file: '" + filename + "'");
        ZipFile zipFile = new ZipFile(filename);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        ArrayList<Flight> flights = new ArrayList<Flight>();

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
                    Flight flight = new Flight(entry.getName(), stream, null);

                    if (flight.getNumberRows() > 3600) {
                        flights.add(flight);
                    }

                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } catch (FatalFlightFileException e) {
                    System.err.println(e.getMessage());
                } catch (FlightAlreadyExistsException e) {
                    System.err.println(e.getMessage());
                }
            }
        } 

        System.out.println("Flights processed:");
        for (int i = 0; i < flights.size(); i++) {
            System.out.println(flights.get(i));
        }
        Collections.shuffle(flights);

        for (int i = 0; i < 10; i++) {
            String outputFilename = "/Users/travisdesell/Data/ngafid/flight_" + i + ".csv";
            String[] columnNames = new String[]{
                "AltAGL", "E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4",
                "E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4", 
                "E1 OilP", "E1 OilT", "E1 RPM", "FQtyL", "FQtyR",
                "GndSpd", "IAS", "LatAc", "NormAc", "OAT",
                "Pitch", "Roll", "TAS", "volt1", "volt2",
                "VSpd", "VSpdG"
            };

            flights.get(i).writeToFile(outputFilename, columnNames);
        }

        System.out.println("total flights in array list: " + flights.size());
    }
}
