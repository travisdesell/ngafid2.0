package org.ngafid.flights;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.zip.*;

import java.util.Enumeration;

import spark.utils.IOUtils;

import org.ngafid.Database;

/**
 * Creates CSV files for flights in the NGAFID
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
 */
public abstract class CSVWriter {
    protected Flight flight;
    protected File outputCSVFile;
    protected Connection connection = Database.getConnection();

    /**
     * Creates a CSVWriter object
     *
     * @param flight the flight to write the CSV file for
     * @param outputCSVFile the output CSV file to use for writing
     */
    public CSVWriter(Flight flight, File outputCSVFile) {
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
