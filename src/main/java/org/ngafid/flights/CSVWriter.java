package org.ngafid.flights;

import java.io.File;
import java.sql.Connection;
import java.util.Optional;
import java.util.zip.*;
import org.ngafid.Database;

/**
 * Creates CSV files for flights in the NGAFID
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
 */
public abstract class CSVWriter {
    protected Flight flight;
    protected Optional<File> outputCSVFile;
    protected Connection connection = Database.getConnection();

    /**
     * Creates a CSVWriter object
     *
     * @param flight the flight to write the CSV file for
     * @param outputCSVFile the output CSV file to use for writing
     */
    public CSVWriter(Flight flight, Optional<File> outputCSVFile) {
        this.flight = flight;
        this.outputCSVFile = outputCSVFile;
    }

    /**
     * Write to a string and return it to the method caller
     */
    public abstract String getFileContents();

    /**
     * Write to the file provided in the constructor
     */
    public abstract void writeToFile();
}
