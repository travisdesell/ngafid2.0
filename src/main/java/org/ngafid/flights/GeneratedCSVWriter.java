package org.ngafid.flights;

import java.sql.SQLException;
import java.util.*;
import java.io.*;
import java.sql.Connection;

import org.ngafid.flights.*;

public class GeneratedCSVWriter extends CSVWriter {
    private List<DoubleTimeSeries> timeSeries;

    public GeneratedCSVWriter(Flight flight, List<String> timeSeriesColumnNames, File outputCSVFile) {
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

    public String getHeader() {
        StringBuilder header = new StringBuilder();

        int nColumns = timeSeries.size();
        for (int i = 0; i < nColumns; i++) {
            DoubleTimeSeries column = timeSeries.get(i);
            String columnName = column.getName();

            String toAppend = (i == nColumns - 1 ? columnName : columnName + ", ");

            header.append(toAppend);
        }

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

        return lineString.toString();
    }

    public String getFileContents() {
        return this.getFileContents(0, this.flight.getNumberRows());
    }

    public String getFileContents(int startLine, int stopLine) {
        StringBuilder stringBuilder = new StringBuilder(this.getHeader());

        for (int i = startLine; i < stopLine; i++) {
            stringBuilder.append(getLine(i));
        }

        return stringBuilder.toString();
    }

    @Override
    public void writeToFile() {
        this.writeToFile(0, super.flight.getNumberRows());
    }

    public void writeToFile(int startLine, int stopLine) {
        try {
            FileWriter fileWriter = new FileWriter(this.outputCSVFile);

            fileWriter.write(this.getHeader());

            for (int i = startLine; i < stopLine; i++) {
                fileWriter.write(this.getLine(i));
            }

            fileWriter.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }
}
