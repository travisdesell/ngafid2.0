package org.ngafid.uploads;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

import org.ngafid.accounts.EmailType;
import org.ngafid.accounts.User;
import org.ngafid.bin.CalculateExceedences;
import org.ngafid.bin.CalculateTTF;
import org.ngafid.bin.FindLowEndingFuelEvents;
import org.ngafid.bin.FindSpinEvents;
import org.ngafid.bin.UploadHelper;
import org.ngafid.common.Database;
import org.ngafid.common.SendEmail;
import org.ngafid.events.proximity.CalculateProximity;
import org.ngafid.flights.FlightError;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.Pipeline;

public final class ProcessUpload {

    private static final Logger LOG = Logger.getLogger(UploadHelper.class.getName());

    public static boolean processUpload(int uploadId) throws SQLException, UploadDoesNotExistException, UploadAlreadyLockedException {
        LOG.info("processing upload with id: " + uploadId);
        Upload upload = null;

        try (Connection connection = Database.getConnection()) {
            // We need to set the upload status to PROCESSING
            upload = Upload.getUploadById(connection, uploadId);
            if (upload == null)
                throw new UploadDoesNotExistException(uploadId);
        }

        return processUpload(upload);
    }

    private static boolean processUpload(Upload upload) throws SQLException, UploadAlreadyLockedException {
        try (Connection connection = Database.getConnection();
             Upload.LockedUpload lockedUpload = upload.getLockedUpload(connection)) {

            try {
                lockedUpload.updateStatus(Upload.Status.PROCESSING);

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
                SendEmail.sendEmail(recipients, bccRecipients, subject, body, EmailType.UPLOAD_PROCESS_START);

                lockedUpload.reset();
                LOG.info("upload was reset!\n\n");

                UploadProcessedEmail uploadProcessedEmail = new UploadProcessedEmail(recipients, bccRecipients);

                long start = System.nanoTime();

                Upload.Status status = ingestFlights(connection, upload, uploadProcessedEmail);

                // only progress if the upload ingestion was successful
                if (status.isProcessed()) {
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

                upload.setStatus(status);
                lockedUpload.updateStatus(status);

                return status.isProcessed();
            } catch (Exception e) {

                LOG.info("Caught exception, will return 'false': " + e.toString());
                return false;
            }
        }
    }

    public static Upload.Status ingestFlights(Connection connection, Upload upload,
                                              UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();

        int uploadId = upload.getId();

        String filename = upload.getArchivePath().toString();
        LOG.info("processing: '" + filename + "'");

        String extension = filename.substring(filename.length() - 4);
        LOG.info("extension: '" + extension + "'");

        Upload.Status status = Upload.Status.PROCESSED_OK;

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
                status = Upload.Status.FAILED_UNKNOWN;
                uploadException = new Exception(e + ", broken upload: please delete this upload and " +
                        "re-upload.");

            } catch (IOException e) {
                LOG.log(Level.SEVERE, "IOException: {0}", e.toString());
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Could not read from zip file: please delete this " +
                        "upload and re-upload.");
                status = Upload.Status.FAILED_ARCHIVE_TYPE;
                uploadException = new Exception(e + ", could not read from zip file: please delete this " +
                        "upload and re-upload.");
            }
        } else {
            // insert an upload error for this upload
            status = Upload.Status.FAILED_ARCHIVE_TYPE;
            UploadError.insertError(connection, uploadId, "Uploaded file was not a zip file.");

            uploadException = new Exception("Uploaded file was not a zip file.");
        }

        // update upload in database, add upload exceptions if there are any
        try (

                PreparedStatement updateStatement = connection.prepareStatement("UPDATE uploads SET " +
                        "n_valid_flights = ?, n_warning_flights = ?, n_error_flights = ? WHERE id = ?")) {
            updateStatement.setInt(1, validFlights);
            updateStatement.setInt(2, warningFlights);
            updateStatement.setInt(3, errorFlights);
            updateStatement.setInt(4, uploadId);
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
            return status;
        } else {
            Instant end = Instant.now();
            double elapsedMillis = (double) Duration.between(start, end).toMillis();
            double elapsedSeconds = elapsedMillis / 1000;
            LOG.info("email in " + elapsedSeconds);
            uploadProcessedEmail.setImportElapsedTime(elapsedSeconds);

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

        if (status.isProcessed()) {
            try {
                FindSpinEvents.findSpinEventsInUpload(connection, upload);
                FindLowEndingFuelEvents.findLowEndFuelEventsInUpload(connection, upload);
                CalculateExceedences.calculateExceedences(connection, upload.id, uploadProcessedEmail);
                CalculateProximity.calculateProximity(connection, upload.id, uploadProcessedEmail);
                CalculateTTF.calculateTTF(connection, upload.id, uploadProcessedEmail);
            } catch (IOException | SQLException | FatalFlightFileException | MalformedFlightFileException |
                     ParseException e) {
                LOG.log(Level.SEVERE, "Got exception calculating events: {0}", e.toString());
                status = Upload.Status.FAILED_UNKNOWN;
                uploadException = new Exception(e.toString() + "\nFailed computing events...");
            }
        }

        // ingestion was successful
        return status;
    }

    public static class FlightInfo {
        /**
         * This is a helper class so we don't keep all loaded flights in memory.
         */

        //CHECKSTYLE:OFF
        int id;
        int length;
        String filename;
        List<MalformedFlightFileException> exceptions;

        //CHECKSTYLE:ON
        public FlightInfo(int id, int length, String filename, List<MalformedFlightFileException> exceptions) {
            this.id = id;
            this.length = length;
            this.filename = filename;
            this.exceptions = exceptions;
        }
    }
}
