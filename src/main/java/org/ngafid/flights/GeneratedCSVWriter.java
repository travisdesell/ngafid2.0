package org.ngafid.flights;

import java.sql.SQLException;
import java.util.*;

import java.sql.Connection;

public class GeneratedCSVWriter extends CSVWriter {
    private Map<String, DoubleTimeSeries> timeSeriesColumns;

    public GeneratedCSVWriter(Flight flight, List<String> timeSeriesColumnNames) {
        super(flight);

        this.timeSeriesColumns = new HashMap<>();

        try {
            for (String columnName : timeSeriesColumnNames) {
                timeSeriesColumns.put(columnName, super.flight.getDoubleTimeSeries(connection, columnName));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public String getFileContents() {
        return "";
    }

    /**
     * Gets what the contents of the file will be as a String for the given
     * interval
     *
     * @param startLine the start index for the file
     * @param stopLine the stop index for the file
     *
     * @return a String containing the CSV file's contents
     */
    public String getFileContents(int startLine, int stopLine) {
        return "";
    }
}
