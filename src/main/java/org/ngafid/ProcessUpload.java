package org.ngafid;

import java.io.InputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.*;

public class ProcessUpload {
    public static void main(String[] arguments) {
        System.out.println("arguments are:");
        System.out.println(Arrays.toString(argmuents));

        Connection connection = Database.getConnection();

        int uploadId = Integer.parseInt(arguments[1]);
        System.out.println("processing upload with id: " + uploadId);

    }

    public static void processFlight(Connection connection, int uploadId) throws SQLException {
        Instant start = Instant.now();


        Upload upload = Upload.getUploadById(connection, uploadId);

        if (upload == null) {
            //An upload with this id did not exist in the database, report an error. This should not happen.
            System.err.println("ERROR: attempted to process an upload with upload id " + uploadId + ", but the upload did not exist. This should never happen.");
            System.exit(1);
        }


        //send email to me and uploader that an import is starting at a particular time

        upload.reset(connection);

        ArrayList<UploadException> flightErrors = new ArrayList<UploadException>();

        int uploaderId = upload.getUploaderId();
        int fleetId = upload.getFleetId();
        String filename = upload.getFilename();

        filename = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId + "/" + uploadId + "__" + filename;
        System.err.println("processing: '" + filename + "'");

        String extension = filename.substring(filename.length() - 4);
        System.err.println("extension: '" + extension + "'");

        String status = "IMPORTED";

        Exception uploadException = null;

        int validFlights = 0;
        int warningFlights = 0;
        int errorFlights = 0;
        if (extension.equals(".zip")) {
            try {
                System.err.println("processing zip file: '" + filename + "'");
                ZipFile zipFile = new ZipFile(filename);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (entry.isDirectory()) {
                        //System.err.println("SKIPPING: " + entry.getName());
                        continue;
                    }

                    if (name.contains("__MACOSX")) {
                        //System.err.println("SKIPPING: " + entry.getName());
                        continue;
                    }

                    System.err.println("PROCESSING: " + name);

                    if (entry.getName().contains(".csv")) {
                        try {
                            InputStream stream = zipFile.getInputStream(entry);
                            Flight flight = new Flight(fleetId, entry.getName(), stream, connection);

                            if (connection != null) {
                                flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                            }

                            if (flight.getStatus().equals("WARNING")) warningFlights++;

                            validFlights++;
                        } catch (IOException e) {
                            System.err.println(e.getMessage());
                            flightErrors.add(new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        } catch (FatalFlightFileException e) {
                            System.err.println(e.getMessage());
                            flightErrors.add(new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        } catch (FlightAlreadyExistsException e) {
                            System.err.println(e.getMessage());
                            flightErrors.add(new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        }

                    } else {
                        flightErrors.add(new UploadException("Unknown file type contained in zip file (flight logs should be .csv files).", entry.getName()));
                        errorFlights++;
                    }
                } 

            } catch (java.nio.file.NoSuchFileException e) {
                System.err.println("IOException: " + e );
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Broken upload: please delete this upload and re-upload.");
                status = "ERROR";

            } catch (IOException e) {
                System.err.println("IOException: " + e );
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Could not read from zip file: please delete this upload and re-upload.");
                status = "ERROR";
            }

        } else {
            //insert an upload error for this upload
            status = "ERROR";
            UploadError.insertError(connection, uploadId, "Uploaded file was not a zip file.");
        }

        //update upload in database, add upload exceptions if there are any
        PreparedStatement updateStatement = connection.prepareStatement("UPDATE uploads SET status = ?, n_valid_flights = ?, n_warning_flights = ?, n_error_flights = ? WHERE id = ?");
        updateStatement.setString(1, status);
        updateStatement.setInt(2, validFlights);
        updateStatement.setInt(3, warningFlights);
        updateStatement.setInt(4, errorFlights);
        updateStatement.setInt(5, uploadId);
        updateStatement.executeUpdate();
        updateStatement.close();

        for (UploadException exception : flightErrors) {
            FlightError.insertError(connection, uploadId, exception.getFilename(), exception.getMessage());
        }

        Instant end = Instant.now();
        double elapsed_millis = (double) Duration.between(start, end).toMillis();
        double elapsed_seconds = elapsed_millis / 1000;
        System.err.println("finished in " + elapsed_seconds);
    }
}
