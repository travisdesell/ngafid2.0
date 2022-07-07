package org.ngafid.flights;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.zip.*;

import java.util.Enumeration;

import spark.utils.IOUtils;

import org.ngafid.Database;

/**
 * Generates/Copies CSV files for flights in the NGAFID
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
 */
public abstract class CSVWriter {
    protected Flight flight;
    protected Connection connection = Database.getConnection();

    public CSVWriter(Flight flight) {
        this.flight = flight;
    }

    /**
     * Gets what the contents of the file will be as a String
     *
     * @return a String containing the CSV file's contents
     *
     * @throws IOException as there will be file read/write operations occuring
     */
    public abstract String getFileContents() throws IOException;
}
