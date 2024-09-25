package org.ngafid.flights;

import java.sql.SQLException;
import java.util.*;

import static org.ngafid.flights.calculations.Parameters.*;

import java.io.*;
import java.sql.Connection;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.*;

public class GeneratedCSVWriter extends CSVWriter {
    private List<DoubleTimeSeries> timeSeries;

    public GeneratedCSVWriter(Flight flight, String [] timeSeriesColumnNames, Optional<File> outputCSVFile) {
        super(flight, outputCSVFile);

        this.timeSeries = new ArrayList<>();

        try {
            for (String columnName : timeSeriesColumnNames) {
                timeSeries.add(super.flight.getDoubleTimeSeries(connection, columnName));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public GeneratedCSVWriter(Flight flight, Optional<File> outputCSVFile, List<DoubleTimeSeries> timeSeries) {
        super(flight, outputCSVFile);

        this.timeSeries = timeSeries;
    }

    public String getHeader() {
        StringBuilder header = new StringBuilder();

        int nColumns = timeSeries.size();
        for (int i = 0; i < nColumns; i++) {
            DoubleTimeSeries column = timeSeries.get(i);
            System.out.println(column);
            String columnName = column.getName();

            String toAppend = (i == nColumns - 1 ? columnName : columnName + ", ");

            header.append(toAppend);
        }

        header.append("\n");
        return header.toString();
    }

    public String getLine(int line) {
        StringBuilder lineString = new StringBuilder();

        int nColumns = timeSeries.size();
        for (int i = 0; i < nColumns; i++) {
            DoubleTimeSeries column = timeSeries.get(i);
            String value = String.valueOf(column.get(line));

            String toAppend = (i == nColumns - 1 ? value : value + ", ");

            lineString.append(toAppend);
        }

        lineString.append("\n");
        return lineString.toString();
    }

    public String getFileContents() {
        return this.getFileContents(0, super.flight.getNumberRows());
    }

    public String getFileContents(int startLine, int stopLine) {
        String header = this.getHeader();

        StringBuilder stringBuilder = new StringBuilder("#" + header);
        stringBuilder.append(header);

        for (int i = startLine; i < stopLine; i++) {
            stringBuilder.append(getLine(i));
        }

        return stringBuilder.toString();
    }

    public void writeToFile(Event event, int padding) {
        assert event.getFlightId() == super.flight.getId();
        int flightLength = flight.getNumberRows();

        int startLine = event.getStartLine() - padding;
        int stopLine = event.getEndLine() + padding;

        if (startLine < 0) startLine = 0;
        if (stopLine > flightLength) stopLine = flightLength;

        writeToFile(startLine, stopLine);
    }

    @Override
    public void writeToFile() {
        this.writeToFile(0, super.flight.getNumberRows());
    }

    public void writeToFile(int startLine, int stopLine) {
        if (super.outputCSVFile.isPresent()) {
            try {
                FileWriter fileWriter = new FileWriter(this.outputCSVFile.get());

                String header = this.getHeader();
                fileWriter.write("#" + header);
                fileWriter.write(header);

                for (int i = startLine; i < stopLine; i++) {
                    fileWriter.write(this.getLine(i));
                }

                fileWriter.close();
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        } else {
            // This should not happen!
            return;
        }
    }

    public static void main(String [] args) {
        System.out.println("Generating new CSV files from the NGAFID for Aidan's SSL project.");

        Connection connection = Database.getConnection();

        String [] colNames = {HDG, IAS, LAT_AC, NORM_AC, IAS, VSPD, ALT_AGL, ALT_MSL, ALT_B, AOA_SIMPLE, CAS};

        String outputDirectory = args[0];
        System.out.println("Will save files to: " + outputDirectory);

        try { 
            List<Flight> flights = Flight.getFlights(connection, 1, 10);

            for (Flight flight : flights) {
                String filename = outputDirectory + "/flight_" + flight.getId() + ".csv";
                File file = new File(filename);

                GeneratedCSVWriter writer = new GeneratedCSVWriter(flight, colNames, Optional.of(file));
                writer.writeToFile();
                System.out.println("Wrote to CSV file: " + filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Finished!");
        System.exit(0);
    }
}
