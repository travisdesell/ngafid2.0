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
public class CSVWriter {
    private File file;
    private Flight flight;
    private ZipEntry entry;
    private ZipFile zipArchive;

    /**
     * Constructor
     * Finds the zip file with the filght requested
     *
     * @param directoryRoot the root directory of the zipped files
     * @param flight the {@link Flight} to write data for
     */
    public CSVWriter(String directoryRoot, Flight flight) throws SQLException {
        System.out.println("creating file from: '" + directoryRoot + "'");

        //File root = new File(directoryRoot);
        //File[] dirs = root.listFiles();

        this.flight = flight;

        int uploadId = flight.getUploadId();
        System.out.println("target upload id is: " + uploadId);

        //TODO: Probably better to pass the connection in as an argument to the constructor
        Connection connection = Database.getConnection();
        Upload upload = Upload.getUploadById(connection, uploadId);

        System.out.println("got an upload with filename: '" + upload.getFilename() + "'");

        String archiveFilename = directoryRoot + uploadId + "__" + upload. getFilename();
        System.out.println("archive filename will be: '" + archiveFilename + "'");

        this.file = new File(archiveFilename);

        if (!this.file.exists()) {
            //TODO: reconstruct from database instead of existing on error
            System.err.println("ERROR: archive file did not exist!");
            System.exit(1);
        } else {
            System.out.println("file exists!");
        }

        if (!this.file.canRead()) {
            System.err.println("ERROR: do not have read access to this file!");
            System.exit(1);
        } else {
            System.out.println("file is readable!");
        }

        try {
            //this.zipArchive = new ZipFile(this.file);
            this.zipArchive = new ZipFile(archiveFilename);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String filename = flight.getFilename();
        Enumeration<? extends ZipEntry> entries = zipArchive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.getName().equals(filename)) {
                this.entry = entry;
            } 
        } 
    }

    /**
     * Gets the CSV file as primitive (binary) data
     *
     * @return a primitive array of bytes 
     */
    public byte [] toBinaryData() throws IOException {
        InputStream inputStream = zipArchive.getInputStream(this.entry);
        return IOUtils.toByteArray(inputStream);
    }
        
    /**
     * Accessor method for the {@link ZipEntry} associated with this flight
     *
     * @return an instance of {@link ZipEntry}
     */
    public ZipEntry getFlightEntry() {
        return this.entry;
    }

    /**
     * Writes to a file and gets the zip archive first
     *
     * @return a String with the file contents
     *
     * @throws IOException if there is a problem with file i/o
     */
    public String write() throws IOException {
        String fileOut = "";
        fileOut = new String(toBinaryData());
        zipArchive.close();
        return fileOut;
    }

    /**
     * Writes to a file and gets the zip archive first
     *
     * @return a String with the file contents
     *
     * @throws IOException if there is a problem with file i/o
     */
    public ZipEntry getZipEntry() throws IOException {
        ZipFile zipArchive = new ZipFile(this.file);
        String filename = flight.getFilename();
        Enumeration<? extends ZipEntry> entries = zipArchive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.getName().equals(filename)) {
                zipArchive.close();
                return entry;
            } 
        } 
        zipArchive.close();
        return null;
    }

    /**
     * Gets a string representation of a CSV Writer object
     *
     * @return a {@link String} with the zip file and archive paths
     */
    @Override
    public String toString() {
        return this.zipArchive.toString() + " " + this.entry.toString();
    }
}
