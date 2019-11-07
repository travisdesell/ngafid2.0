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


import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FatalFlightFileException;

import org.ngafid.flights.Flight;

public class ExtractFlights {
    private static Connection connection = Database.getConnection();


    public static void main(String[] arguments) throws Exception {
        Options options = new Options();

        Option flightIds = new Option("f", "flight_ids", true, "list of flight ids to extract");
        flightIds.setRequired(true);
        flightIds.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(flightIds);

        Option outfilePrefix = new Option("o", "output_file_prefix", true, "prefix for output file names");
        outfilePrefix.setRequired(true);
        options.addOption(outfilePrefix);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, arguments);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ExtractFlights", options);

            System.exit(1);
        }

        String outputBase = cmd.getOptionValue('o');
        System.out.println("output prefix: '" + outputBase + "'");
        System.out.println(Arrays.toString(cmd.getOptionValues('f')));

        ArrayList<Flight> flights = new ArrayList<Flight>();

        for (String flightIdStr : cmd.getOptionValues('f')) {
            int flightId = Integer.parseInt(flightIdStr);

            Flight flight = Flight.getFlight(connection, flightId);

            if (flight == null) {
                System.err.println("WARNING: flight id: '" + flightId + "' did not exist in the database.");
                continue;
            }

            flights.add(flight);
        }

        for (int i = 0; i < flights.size(); i++) {
            String outputFilename = outputBase + flights.get(i).getId() + ".csv";

            flights.get(i).writeToFile(connection, outputFilename);
        }

        System.out.println("total flights in array list: " + flights.size());

        connection.close();
    }
}
