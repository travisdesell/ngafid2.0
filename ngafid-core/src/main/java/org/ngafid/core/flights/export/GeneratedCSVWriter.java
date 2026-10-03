package org.ngafid.core.flights.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;

/**
 * A {@link CSVWriter} that regenerates a flight's CSV from its stored time series in the database.
 *
 * <p>Loads the requested double time series columns for a flight and writes them row by row, rather than reading back a
 * previously cached export file.
 */
public class GeneratedCSVWriter extends CSVWriter {
    private List<DoubleTimeSeries> timeSeries;

    /**
     * Constructs a CSV writer for a flight, loading the named double time series from the database to form the output
     * columns.
     *
     * @param flight the flight whose data will be written
     * @param timeSeriesColumnNames the names of the double series to include as columns
     * @param outputCSVFile the destination file, or empty to only build in-memory contents
     */
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

    /**
     * Constructs a CSV writer for a flight using an already-loaded set of double time series as the output columns.
     *
     * @param flight the flight whose data will be written
     * @param outputCSVFile the destination file, or empty to only build in-memory contents
     * @param timeSeries the double series to include as columns
     */
    public GeneratedCSVWriter(Flight flight, Optional<File> outputCSVFile, List<DoubleTimeSeries> timeSeries) {
        super(flight, outputCSVFile);

        this.timeSeries = timeSeries;
    }

    /**
     * Builds the CSV header line: the column (series) names joined by commas.
     *
     * @return the header line, terminated with a newline
     */
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

    /**
     * Builds the CSV row for a single sample index: each column's value at that index, joined by commas.
     *
     * @param line the sample index
     * @return the CSV row, terminated with a newline
     */
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

    /**
     * Builds the full CSV text for a range of sample indices: a commented header line, the plain header line, and one
     * row per sample in {@code [startLine, stopLine)}.
     *
     * @param startLine the inclusive first sample index
     * @param stopLine the exclusive last sample index
     * @return the CSV contents as a string
     */
    public String getFileContents(int startLine, int stopLine) {
        String header = this.getHeader();

        StringBuilder stringBuilder = new StringBuilder("#" + header);
        stringBuilder.append(header);

        for (int i = startLine; i < stopLine; i++) {
            stringBuilder.append(getLine(i));
        }

        return stringBuilder.toString();
    }

    /**
     * Writes a CSV covering the samples around an event to the output file: the event's line range expanded by
     * {@code padding} on each side and clamped to the flight's bounds.
     *
     * @param event the event to export the surrounding data for (must belong to this writer's flight)
     * @param padding the number of extra samples to include before and after the event
     */
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

    /**
     * Writes the CSV for a range of sample indices (a commented header, the plain header, then one row per sample in
     * {@code [startLine, stopLine)}) to the configured output file, if one is present.
     *
     * @param startLine the inclusive first sample index
     * @param stopLine the exclusive last sample index
     */
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
