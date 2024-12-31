package org.ngafid;

import org.ngafid.accounts.EmailType;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;
import org.ngafid.flights.*;
import org.ngafid.flights.process.Pipeline;
import org.ngafid.proximity.CalculateProximity;

import java.io.Console;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public class ProcessUpload {
    private static final String ERROR_STATUS_STR = "ERROR";
    static int poolSize = 1;
    static boolean batchedDB = true;
    private static final Logger LOG = Logger.getLogger(ProcessUpload.class.getName());

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

    public static void main(String[] arguments) throws SQLException {
        String batchedDBS = System.getenv("BATCHED_DB");
        if (batchedDBS != null) batchedDB = Boolean.parseBoolean(batchedDBS);

        removeNoUploadFlights();

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
     * Sometimes in the process of removing an upload (probably via the webpage)
     * this operation
     * does not complete and this results in flights being in the database with a
     * non-existant
     * upload. This can cause the upload process to crash.
     *
     * @param connection is the connection to the database
     */
    public static void removeNoUploadFlights() {
        try (Connection connection = Database.getConnection()) {
            ArrayList<Flight> noUploadFlights = Flight.getFlights(connection, "NOT EXISTS (SELECT * FROM uploads " +
                    "WHERE uploads.id = flights.upload_id)");

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

    public static void operateAsDaemon() throws SQLException {
        while (true) {
            Instant start = Instant.now();

            try (Connection connection = Database.getConnection()) {
                PreparedStatement fleetPreparedStatement = connection.prepareStatement("SELECT id FROM fleet WHERE " +
                        "EXISTS (SELECT id FROM uploads WHERE fleet.id = uploads.fleet_id AND uploads.status = " +
                        "'UPLOADED')");
                ResultSet fleetSet = fleetPreparedStatement.executeQuery();
                while (fleetSet.next()) {
                    int targetFleetId = fleetSet.getInt(1);
                    LOG.info("Importing an upload from fleet: " + targetFleetId);
                    System.err.println("Importing an upload from fleet: " + targetFleetId);
                    if (targetFleetId == 164 || targetFleetId == 105) {
                        System.err.println("SKIPPING 164 because we do not support this fleet type yet.");
                        continue;
                    }

                    PreparedStatement uploadsPreparedStatement = connection.prepareStatement("SELECT id FROM uploads " +
                            "WHERE status = ? AND fleet_id = ?");

                    uploadsPreparedStatement.setString(1, "UPLOADED");
                    uploadsPreparedStatement.setInt(2, targetFleetId);

                    ResultSet resultSet = uploadsPreparedStatement.executeQuery();

                    while (resultSet.next()) {
                        int uploadId = resultSet.getInt(1);
                        processUpload(uploadId);
                    }

                    resultSet.close();
                    uploadsPreparedStatement.close();
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
        try (Connection connection = Database.getConnection()) {
            Fleet fleet = Fleet.get(connection, fleetId);
            String f = fleet.getName() == null ? " NULL NAME " : fleet.getName();
            List<Upload> uploads = Upload.getUploads(connection, fleetId);
            System.out.print("Found " + uploads.size() + " uploads from fleet " + f + ". Would you like to reimport " +
                    "them? [Y/n] ");
            Console con = System.console();
            String s = con.readLine("");
            if (s.toLowerCase().startsWith("y")) {
                for (Upload upload : uploads) {
                    if (upload == null) {
                        LOG.severe("Encountered a null upload. this should never happen, but moving on to the next " +
                                "upload");
                        continue;
                    }
                    processUpload(upload);
                }
            }
            // sendMonthlyFlightsUpdate(fleetId); [EX] Disabling ALL monthly flight update
            // calls for now!

        } catch (SQLException e) {
            LOG.info("Encountered error");
            e.printStackTrace();
        }
    }

    public static void processUpload(int uploadId) {
        LOG.info("processing upload with id: " + uploadId);
        try (Connection connection = Database.getConnection()) {
            Upload upload = Upload.getUploadById(connection, uploadId);

            if (upload == null) {
                // An upload with this id did not exist in the database, report an error. This
                // should not happen.
                LOG.severe("ERROR: attempted to importing an upload with upload id " + uploadId + ", but the upload " +
                        "did not exist. This should never happen.");
                System.exit(1);
            }

            processUpload(upload);
        } catch (SQLException e) {
            LOG.severe("ERROR processing upload: " + e);
            e.printStackTrace();
        }
    }

    public static void processUpload(Upload upload) {
        try (Connection connection = Database.getConnection()) {
            int uploaderId = upload.getUploaderId();
            int fleetId = upload.getFleetId();
            String filename = upload.getFilename();

            User uploader = User.get(connection, uploaderId, fleetId);
            String uploaderEmail = uploader.getEmail();

            ArrayList<String> recipients = new ArrayList<String>();
            recipients.add(uploaderEmail);
            ArrayList<String> bccRecipients = SendEmail.getAdminEmails(); // always email admins to keep tabs on things

            String formattedStartDateTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd " +
                    "hh:mm:ss z (O)"));

            String subject = "NGAFID processing upload '" + filename + "' started at " + formattedStartDateTime;
            String body = subject;
            SendEmail.sendEmail(recipients, bccRecipients, subject, body, EmailType.UPLOAD_PROCESS_START, connection);

            upload.reset(connection);
            LOG.info("upload was reset!\n\n");

            UploadProcessedEmail uploadProcessedEmail = new UploadProcessedEmail(recipients, bccRecipients);

            long start = System.nanoTime();

            boolean success = ingestFlights(connection, upload, uploadProcessedEmail);
            // only progress if the upload ingestion was successful
            if (success) {
                String endTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss z (O)"));
                uploadProcessedEmail.setSubject("NGAFID import of '" + filename + "' email at " + endTime);

            } else {
                uploadProcessedEmail.setSubject("NGAFID upload '" + filename + "' ERROR on import");
            }

            long end = System.nanoTime();
            long diff = end - start;
            double asSeconds = ((double) diff) * 1.0e-9;

            LOG.info("Processing upload took " + asSeconds + "s");

            uploadProcessedEmail.sendEmail(connection);

        } catch (SQLException e) {
            LOG.severe("ERROR processing upload: " + e);
            e.printStackTrace();
        }
    }

    public static boolean ingestFlights(Connection connection, Upload upload,
                                        UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();

        int uploadId = upload.getId();
        int uploaderId = upload.getUploaderId();
        int fleetId = upload.getFleetId();

        String filename = upload.getArchivePath().toString();
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
            // Pipeline must be closed after use as it may open some files / resources.
            try (ZipFile zipFile = new ZipFile(filename); Pipeline pipeline = new Pipeline(connection, upload,
                    zipFile)) {
                pipeline.execute();

                flightInfo = pipeline.getFlightInfo();
                flightErrors = pipeline.getFlightErrors();
                errorFlights = flightErrors.size();
                warningFlights = pipeline.getWarningFlightsCount();
                validFlights = pipeline.getValidFlightsCount();

            } catch (java.nio.file.NoSuchFileException e) {
                LOG.log(Level.SEVERE, "NoSuchFileException: {0}", e.toString());
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Broken upload: please delete this upload " +
                        "and re-upload.");
                status = ERROR_STATUS_STR;
                uploadException = new Exception(e + ", broken upload: please delete this upload and " +
                        "re-upload.");

            } catch (IOException e) {
                LOG.log(Level.SEVERE, "IOException: {0}", e.toString());
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Could not read from zip file: please delete this " +
                        "upload and re-upload.");
                status = ERROR_STATUS_STR;
                uploadException = new Exception(e + ", could not read from zip file: please delete this " +
                        "upload and re-upload.");
            }
        } else {
            // insert an upload error for this upload
            status = ERROR_STATUS_STR;
            UploadError.insertError(connection, uploadId, "Uploaded file was not a zip file.");

            uploadException = new Exception("Uploaded file was not a zip file.");
        }

        // update upload in database, add upload exceptions if there are any
        try (

                PreparedStatement updateStatement = connection.prepareStatement("UPDATE uploads SET status = ?, " +
                        "n_valid_flights = ?, n_warning_flights = ?, n_error_flights = ? WHERE id = ?")) {
            updateStatement.setString(1, status);
            updateStatement.setInt(2, validFlights);
            updateStatement.setInt(3, warningFlights);
            updateStatement.setInt(4, errorFlights);
            updateStatement.setInt(5, uploadId);
            updateStatement.executeUpdate();
        }

        // insert all the flight errors to the database
        for (Map.Entry<String, UploadException> entry : flightErrors.entrySet()) {
            UploadException exception = entry.getValue();
            FlightError.insertError(connection, uploadId, exception.getFilename(), exception.getMessage());
        }

        // prepare the response email
        if (uploadException != null) {
            uploadProcessedEmail.addImportFailure("Could not import upload '" + filename + "' due to the following " +
                    "error:\n");
            uploadProcessedEmail.addImportFailure(uploadException.getMessage());

            // ingestion failed
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

            // iterate over all the flights without warnings
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

        if (status.equals("IMPORTED")) {
            try {
                FindSpinEvents.findSpinEventsInUpload(connection, upload);
                FindLowEndingFuelEvents.findLowEndFuelEventsInUpload(connection, upload);
                CalculateExceedences.calculateExceedences(connection, upload.id, uploadProcessedEmail);
                CalculateProximity.calculateProximity(connection, upload.id, uploadProcessedEmail);
                CalculateTTF.calculateTTF(connection, upload.id, uploadProcessedEmail);
            } catch (IOException | SQLException | FatalFlightFileException | MalformedFlightFileException |
                     ParseException e) {
                LOG.log(Level.SEVERE, "Got exception calculating events: {0}", e.toString());
                status = ERROR_STATUS_STR;
                uploadException = new Exception(e + "\nFailed computing events...");
            }
        }

        // ingestion was successfull
        return true;
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
}
