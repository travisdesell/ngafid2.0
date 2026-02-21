package org.ngafid.core.flights.export;

import org.ngafid.core.Database;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.uploads.Upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CachedCSVWriter extends CSVWriter {
    private File zipFile;
    private ZipEntry entry;
    private ZipFile zipArchive;

    /**
     * Constructor
     * Finds the zip file with the filght requested
     *
     * @param directoryRoot the root directory of the zipped files
     * @param flight        the {@link Flight} to write data for
     * @param outputCSVFile the output file to write to
     * @param isAirSync     whether the file is an air sync
     */
    public CachedCSVWriter(String directoryRoot, Flight flight, Optional<File> outputCSVFile, boolean isAirSync)
            throws SQLException {
        super(flight, outputCSVFile);

        int uploadId = flight.getUploadId();

        //CHECKSTYLE:OFF
        // TODO: Probably better to pass the connection in as an argument to the
        //CHECKSTYLE:ON
        // constructor
        Upload upload;
        try (Connection connection = Database.getConnection()) {
            upload = Upload.getUploadById(connection, uploadId);
        }

        String archiveFilename;
        if (isAirSync) {
            archiveFilename = directoryRoot + upload.getFilename();
        } else {
            archiveFilename = directoryRoot + uploadId + "__" + upload.getFilename();
        }

        this.zipFile = new File(archiveFilename);

        //CHECKSTYLE:OFF
        // TODO: reconstruct from database instead of existing on error
        //CHECKSTYLE:ON
        if (!this.zipFile.exists()) {
            throw new RuntimeException("Archive file did not exist: " + this.zipFile.getAbsolutePath());
        }

        if (!this.zipFile.canRead()) {
            throw new RuntimeException("Do not have read access to archive file: " + this.zipFile.getAbsolutePath());
        }

        try {
            this.zipArchive = new ZipFile(archiveFilename);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        String filename = flight.getFilename();
        Enumeration<? extends ZipEntry> entries = zipArchive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry currentEntry = entries.nextElement();

            if (currentEntry.getName().equals(filename)) {
                this.entry = currentEntry;
            }
        }
    }

    /**
     * Gets the CSV file as primitive (binary) data
     *
     * @return a primitive array of bytes
     */
    public byte[] toBinaryData() throws IOException {
        InputStream inputStream = zipArchive.getInputStream(this.entry);
        return inputStream.readAllBytes();
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
     * {@inheritDoc}
     */
    public String getFileContents() {
        String fileOut = "";

        try {
            fileOut = new String(toBinaryData());
            zipArchive.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }

        return fileOut;
    }

    /**
     * Writes to a file and gets the zip archive first
     *
     * @return a String with the file contents
     * @throws IOException if there is a problem with file i/o
     */
    public ZipEntry getZipEntry() throws IOException {
        ZipFile zipArchiveToWrite = new ZipFile(this.zipFile);
        String filename = super.flight.getFilename();
        Enumeration<? extends ZipEntry> entries = zipArchiveToWrite.entries();

        while (entries.hasMoreElements()) {
            ZipEntry currentEntry = entries.nextElement();

            if (currentEntry.getName().equals(filename)) {
                zipArchiveToWrite.close();
                return currentEntry;
            }
        }

        zipArchiveToWrite.close();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToFile() {
        if (super.outputCSVFile.isPresent()) {
            try {
                File outfile = this.outputCSVFile.get();

                if (!outfile.exists()) {
                    outfile.createNewFile();
                }

                FileOutputStream fos = new FileOutputStream(outfile);

                fos.write(toBinaryData());
                fos.close();
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        } else {
            // This should not happen!
            return;
        }
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
