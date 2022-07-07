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

    public CSVWriter(Flight flight) {
        this.flight = flight;
    }

    /**
     * Gets what the contents of the file will be as a String
     *
     * @return a String containing the CSV file's contents
     */
    public abstract String getFileContents();
}
