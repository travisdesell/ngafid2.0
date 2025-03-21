package org.ngafid.core.flights.export;

import org.ngafid.core.flights.Flight;

import java.io.File;
import java.util.Optional;

/**
 * Creates CSV files for flights in the NGAFID
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
 */
public abstract class CSVWriter {
    protected Flight flight;
    protected Optional<File> outputCSVFile;

    /**
     * Creates a CSVWriter object
     *
     * @param flight        the flight to write the CSV file for
     * @param outputCSVFile the output CSV file to use for writing
     */
    public CSVWriter(Flight flight, Optional<File> outputCSVFile) {
        this.flight = flight;
        this.outputCSVFile = outputCSVFile;
    }

    /**
     * Write to a string and return it to the method caller
     *
     * @return the contents of the file as a string
     */
    public abstract String getFileContents();

    /**
     * Write to the file provided in the constructor
     */
    public abstract void writeToFile();
}
