package org.ngafid.flights.export;

import org.ngafid.common.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GeneratedCSVWriter extends CSVWriter {
    private List<DoubleTimeSeries> timeSeries;

    public GeneratedCSVWriter(Flight flight, String[] timeSeriesColumnNames, Optional<File> outputCSVFile) {
        super(flight, outputCSVFile);

        this.timeSeries = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
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

        if (startLine < 0)
            startLine = 0;
        if (stopLine > flightLength)
            stopLine = flightLength;

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
}
