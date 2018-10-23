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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FatalFlightFileException;

import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;

public class ProcessFile {
    public static void main(String[] arguments) {
        String filename = "/Users/travisdesell/Data/ngafid/und_single_week/C172/N507ND/log_180802_115429_KGFK.csv";

        try {
            Flight flight = new Flight(filename, null);

            DoubleTimeSeries pitchSeries = flight.getDoubleTimeSeries("Pitch");

            for (int i = 0; i < pitchSeries.size(); i++) {
                System.out.println(pitchSeries.get(i));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("finished!");
    }
}
