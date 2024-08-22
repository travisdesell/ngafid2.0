package org.ngafid;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import Files.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.text.ParseException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import org.ngafid.proximity.CalculateProximity;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.Upload;
import org.ngafid.flights.UploadError;
import org.ngafid.flights.process.*;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;

import static org.ngafid.flights.DJIFlightProcessor.processDATFile;

public class ProcessUpload {
    private static Connection connection = null;
    private static Logger LOG = Logger.getLogger(ProcessUpload.class.getName());
    private static final String ERROR_STATUS_STR = "ERROR";

    static int poolSize = 1;
    static boolean batchedDB = true;

    public static void sendMonthlyFlightsUpdate(int fleetID) {
        try {
            // TODO: get env for port
            final URL url = new URL("http://localhost:8181/update_monthly_flights?fleetId=" + fleetID);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");
            connection.connect();
            final int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                LOG.info("Error updating monthly flights cache for fleet " + fleetID + ": " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    
    public static void main(String[] arguments) {
        String poolSizeS = System.getenv("POOL_SIZE");
        if (poolSizeS != null) poolSize = Integer.parseInt(poolSizeS);
        pool = new ForkJoinPool(poolSize);

        String batchedDBS = System.getenv("BATCHED_DB");
        if (batchedDBS != null) batchedDB = Boolean.parseBoolean(batchedDBS);

        connection = Database.getConnection();

        removeNoUploadFlights(connection);

        if (arguments.length >= 1) {
            if (arguments[0].equals("--fleet")) {
                processFleetUploads(Integer.parseInt(arguments[1]));
            } else {
                int uploadId = Integer.parseInt(arguments[0]);
                processUpload(uploadId);
            }
        } else {
            operateAsDaemon();
        }
    }

    /**
     * Sometimes in the process of removing an upload (probably via the webpage) this operation
     * does not complete and this results in flights being in the database with a non-existant
     * upload. This can cause the upload process to crash.
     *
     * @param connection is the connection to the database
     */
    public static void removeNoUploadFlights(Connection connection) {
        try {
            ArrayList<Flight> noUploadFlights = Flight.getFlights(connection, "NOT EXISTS (SELECT * FROM uploads WHERE uploads.id = flights.upload_id)");

            for (Flight flight : noUploadFlights) {
                LOG.info("flight had no related upload. flight id: " + flight.getId() + ", uplaod id: " + flight.getUploadId());
                flight.remove(connection);
            }

        } catch (SQLException e) {
            LOG.info("Error removing flights without an upload:" + e);
            e.printStackTrace();
            System.exit(1);
        }
    }


    public static void operateAsDaemon() {
        while (true) {
            connection = Database.resetConnection();

            Instant start = Instant.now();

            try {
                PreparedStatement fleetPreparedStatement = connection.prepareStatement("SELECT id FROM fleet WHERE EXISTS (SELECT id FROM uploads WHERE fleet.id = uploads.fleet_id AND uploads.status = 'UPLOADED')");
                ResultSet fleetSet = fleetPreparedStatement.executeQuery();
                while (fleetSet.next()) {
                    int targetFleetId = fleetSet.getInt(1);
                    LOG.info("Importing an upload from fleet: " + targetFleetId);

                    PreparedStatement uploadsPreparedStatement = connection.prepareStatement("SELECT id FROM uploads WHERE status = ? AND fleet_id = ? LIMIT 1");

                    uploadsPreparedStatement.setString(1, "UPLOADED");
                    uploadsPreparedStatement.setInt(2, targetFleetId);

                    ResultSet resultSet = uploadsPreparedStatement.executeQuery();

                    while (resultSet.next()) {
                        int uploadId = resultSet.getInt(1);
                        processUpload(uploadId);
                    }

                    resultSet.close();
                    uploadsPreparedStatement.close();
                    sendMonthlyFlightsUpdate(targetFleetId);
                }

                fleetSet.close();
                fleetPreparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }

            Instant end = Instant.now();
            double elapsed_millis = (double) Duration.between(start, end).toMillis();
            double elapsed_seconds = elapsed_millis / 1000;
            LOG.info("finished in " + elapsed_seconds);

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                LOG.info(e.toString());
                e.printStackTrace();
            }
        }
    }

    public static void processFleetUploads(int fleetId) {
        LOG.info("processing uploads from fleet with id: " + fleetId);
        try {
            Fleet fleet = Fleet.get(connection, fleetId);
            String f = fleet.getName() == null ? " NULL NAME " : fleet.getName();
            List<Upload> uploads = Upload.getUploads(connection, fleetId);
            System.out.print("Found " + uploads.size() + " uploads from fleet " + f + ". Would you like to reimport them? [Y/n] ");
            Console con = System.console();
            String s = con.readLine("");
            if (s.toLowerCase().startsWith("y")) {
                for (Upload upload : uploads) {
                    if (upload == null) {
                        LOG.severe("Encountered a null upload. this should never happen, but moving on to the next upload");
                        continue;
                    }
                    processUpload(upload);
                }
            }
            sendMonthlyFlightsUpdate(fleetId);

        } catch (SQLException e) {
            LOG.info("Encountered error");
            e.printStackTrace();
        }
    }

    public static void processUpload(int uploadId) {
        LOG.info("processing upload with id: " + uploadId);
        try {
            Upload upload = Upload.getUploadById(connection, uploadId);

            if (upload == null) {
                //An upload with this id did not exist in the database, report an error. This should not happen.
                LOG.severe("ERROR: attempted to importing an upload with upload id " + uploadId + ", but the upload did not exist. This should never happen.");
                System.exit(1);
            }
            processUpload(upload);
        } catch (SQLException e) {
            LOG.severe("ERROR processing upload: " + e);
            e.printStackTrace();
        }
    }

    public static void processUpload(Upload upload) {
        try {
            int uploadId = upload.getId();
            int uploaderId = upload.getUploaderId();
            int fleetId = upload.getFleetId();
            String filename = upload.getFilename();

            User uploader = User.get(connection, uploaderId, fleetId);
            String uploaderEmail = uploader.getEmail();

            ArrayList<String> recipients = new ArrayList<String>();
            recipients.add(uploaderEmail);
            ArrayList<String> bccRecipients = SendEmail.getAdminEmails(); //always email admins to keep tabs on things

            String formattedStartDateTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss z (O)"));

            String subject = "NGAFID processing upload '" + filename + "' started at " + formattedStartDateTime;
            String body = subject;
            SendEmail.sendEmail(recipients, bccRecipients, subject, body);

            upload.reset(connection);
            LOG.info("upload was reset!\n\n");

            UploadProcessedEmail uploadProcessedEmail = new UploadProcessedEmail(recipients, bccRecipients);

            long start = System.nanoTime();
                     
            boolean success = ingestFlights(connection, upload, uploadProcessedEmail);
            // only progress if the upload ingestion was successful
            if (success) {
                FindSpinEvents.findSpinEventsInUpload(connection, upload);

                FindLowEndingFuelEvents.findLowEndFuelEventsInUpload(connection, upload);

                CalculateExceedences.calculateExceedences(connection, uploadId, uploadProcessedEmail);

                CalculateProximity.calculateProximity(connection, uploadId, uploadProcessedEmail);

                CalculateTTF.calculateTTF(connection, uploadId, uploadProcessedEmail);

                String endTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss z (O)"));

                uploadProcessedEmail.setSubject("NGAFID import of '" + filename + "' email at " + endTime);

            } else {
                uploadProcessedEmail.setSubject("NGAFID upload '" + filename + "' ERROR on import");
            }

            long end = System.nanoTime();
            long diff = end - start;
            double asSeconds = ((double) diff) * 1.0e-9;

            sendMonthlyFlightsUpdate(fleetId);
            uploadProcessedEmail.sendEmail();

        } catch (SQLException e) {
            LOG.severe("ERROR processing upload: " + e);
            e.printStackTrace();
        }
    }

    public static class FlightInfo {
        /**
         * This is a helper class so we don't keep all loaded flights in memory.
         */

        int id;
        int length;
        String filename;
        List<MalformedFlightFileException> exceptions;

        public FlightInfo(int id, int length, String filename, List<MalformedFlightFileException> exceptions) {
            this.id = id;
            this.length = length;
            this.filename = filename;
            this.exceptions = exceptions;
        }
    }

    private static ForkJoinPool pool;

    public static boolean ingestFlights(Connection connection, Upload upload, UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();

        int uploadId = upload.getId();
        int uploaderId = upload.getUploaderId();
        int fleetId = upload.getFleetId();

        String filename = upload.getFilename().toLowerCase();
        filename = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId + "/" + uploadId + "__" + filename;
        LOG.info("processing: '" + filename + "'");

        String extension = filename.substring(filename.length() - 4);
        LOG.info("extension: '" + extension + "'");

        String status = "IMPORTED";

        Exception uploadException = null;

        Map<String, FlightInfo> flightInfo = Collections.emptyMap();

        Map<String, UploadException> flightErrors = Collections.emptyMap();

        int validFlights = 0;
        int warningFlights = 0;
        int errorFlights = 0;

        if (extension.equals(".zip")) {
            try (ZipFile zipFile = new ZipFile(filename)) {
                Pipeline pipeline = Pipeline.run(connection, upload, zipFile);

                flightInfo = pipeline.getFlightInfo();
                flightErrors = pipeline.getFlightErrors();
                errorFlights = flightErrors.size();
                warningFlights = pipeline.getWarningFlightsCount();
                validFlights = pipeline.getValidFlightsCount();

            } catch (java.nio.file.NoSuchFileException e) {
                LOG.log(Level.SEVERE, "NoSuchFileException: {0}", e.toString());
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Broken upload: please delete this upload and re-upload.");
                status = ERROR_STATUS_STR;
                uploadException = new Exception(e.toString() + ", broken upload: please delete this upload and re-upload.");

            } catch (IOException e) {
                LOG.log(Level.SEVERE, "IOException: {0}", e.toString());
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Could not read from zip file: please delete this upload and re-upload.");
                status = ERROR_STATUS_STR;
                uploadException = new Exception(e.toString() + ", could not read from zip file: please delete this upload and re-upload.");
            }
        } else {
            //insert an upload error for this upload
            status = ERROR_STATUS_STR;
            UploadError.insertError(connection, uploadId, "Uploaded file was not a zip file.");

            uploadException = new Exception("Uploaded file was not a zip file.");
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

        //insert all the flight errors to the database
        for (Map.Entry<String, UploadException> entry : flightErrors.entrySet()) {
            UploadException exception = entry.getValue();
            FlightError.insertError(connection, uploadId, exception.getFilename(), exception.getMessage());
        }

        //prepare the response email
        if (uploadException != null) {
            uploadProcessedEmail.addImportFailure("Could not import upload '" + filename + "' due to the following error:\n");
            uploadProcessedEmail.addImportFailure(uploadException.getMessage());

            //ingestion failed
            return false;
        } else {
            Instant end = Instant.now();
            double elapsed_millis = (double) Duration.between(start, end).toMillis();
            double elapsed_seconds = elapsed_millis / 1000;
            LOG.info("email in " + elapsed_seconds);
            uploadProcessedEmail.setImportElapsedTime(elapsed_seconds);

            LOG.info("valid flights: " + validFlights);
            LOG.info("warning flights: " + warningFlights);
            LOG.info("error flights: " + errorFlights);

            uploadProcessedEmail.setValidFlights(validFlights);

            //iterate over all the flights without warnings
            for (FlightInfo info : flightInfo.values()) {
                uploadProcessedEmail.addFlight(info.filename, info.id, info.length);

                List<MalformedFlightFileException> exceptions = info.exceptions;
                if (exceptions.size() == 0) {
                    uploadProcessedEmail.flightImportOK(info.filename);
                }
            }

            uploadProcessedEmail.setErrorFlights(errorFlights);

            for (Map.Entry<String, UploadException> entry : flightErrors.entrySet()) {
                UploadException exception = entry.getValue();

                uploadProcessedEmail.flightImportError(exception.getFilename(), exception.getMessage());
            }

            uploadProcessedEmail.setWarningFlights(warningFlights);

            for (FlightInfo info : flightInfo.values()) {
                List<MalformedFlightFileException> exceptions = info.exceptions;

                if (exceptions.size() > 0) {
                    for (MalformedFlightFileException exception : exceptions) {
                        uploadProcessedEmail.flightImportWarning(info.filename, exception.getMessage());
                    }
                }
            }
        }

        //ingestion was successfull
        return true;
    }
}
