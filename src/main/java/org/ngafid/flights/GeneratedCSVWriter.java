package org.ngafid.flights;

import java.sql.SQLException;
import java.util.*;
import java.io.*;
import java.sql.Connection;

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

    public void writeToFile(Event event, int nTimeSteps) {
        assert event.getFlightId() == super.flight.getId();

        int flightLength = flight.getNumberRows();
        int eventLength = event.getDuration();

        if (nTimeSteps % 2 != 0) {
            System.err.println("ERROR: Please use even numbers for the time step size!");
            System.exit(1);
        }

        if (eventLength > flightLength) {
            System.err.println("ERROR: event length for event " + event.getId() + " is too long");
        }

        int startLine = event.getStartLine(), endLine = event.getEndLine();

        int padding = nTimeSteps - eventLength;

        if (padding % 2 == 0) {
            startLine -= padding / 2;
            endLine += padding / 2;
        } else {
            startLine -= padding / 2;
            endLine += (padding / 2) + 1;
        }

        writeToFile(startLine, endLine);
    }

    @Override
    public void writeToFile() {
        this.writeToFile(0, super.flight.getNumberRows());
    }

    public void writeToFile(int startLine, int stopLine) {
        if (startLine < 0 || stopLine > super.flight.getNumberRows()) {
            //TODO handle padding values
            System.err.println("SKIPPING EVENT....");
            return;
        }

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
}
