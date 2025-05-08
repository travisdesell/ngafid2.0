package org.ngafid.uploads;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.ngafid.accounts.EmailType;
import org.ngafid.accounts.User;
import org.ngafid.bin.WebServer;
import org.ngafid.common.Database;
import org.ngafid.common.MD5;
import org.ngafid.common.SendEmail;
import org.ngafid.flights.FlightError;
import org.ngafid.uploads.process.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * Contains a static method processUpload which will process an upload with the supplied uploadId. This upload should
 * already be fully uploaded to the website.
 * <p>
 * Processing an upload has a few steps:
 * 1. Acquiring a SQL lock for the upload to ensure unique access.
 * 2. Clearing the upload if it has already been processed (i.e. deleting all flights, events, etc.).
 * 3. Ingesting the archive (this work is encapsulated in {@link org.ngafid.uploads.process.Pipeline}).
 * 4. Updating the database with the results, exceptions, etc.
 */
public final class ProcessUpload {

    private static final Logger LOG = Logger.getLogger(ProcessUpload.class.getName());

    public static boolean processUpload(int uploadId) throws SQLException, UploadDoesNotExistException, UploadAlreadyLockedException {
        LOG.info("processing upload with id: " + uploadId);
        Upload upload = null;

        try (Connection connection = Database.getConnection()) {
            // We need to set the upload status to PROCESSING
            upload = Upload.getUploadById(connection, uploadId);
            if (upload == null)
                throw new UploadDoesNotExistException(uploadId);

            // If this is the first time an upload is being processed, it still exists in pieces on the disk -- combine them here.

            return processUpload(connection, upload);
        }
    }

    private static void tryCombinePieces(Connection connection, Upload upload, Upload.LockedUpload locked) throws IOException {
        LOG.info("Combining pieces");
        Path chunkDirectory = Paths.get(WebServer.NGAFID_UPLOAD_DIR + "/" + upload.fleetId + "/" + upload.uploaderId + "/" + upload.identifier);
        Path targetDirectory = Paths.get(upload.getArchiveDirectory());

        targetDirectory.toFile().mkdirs();
        Path targetFilename = Paths.get(targetDirectory + "/" + upload.getArchiveFilename());

        // Chunks have already been combined.
        if (Files.exists(targetFilename) && Files.isRegularFile(targetFilename)) {
            return;
        }

        try (FileOutputStream out = new FileOutputStream(targetFilename.toFile())) {
            for (int i = 0; i < upload.getNumberChunks(); i++) {
                byte[] bytes = Files.readAllBytes(Paths.get(chunkDirectory + "/" + i + ".part"));
                out.write(bytes);
            }

            if (!upload.checkSize()) {
                out.close();
                targetFilename.toFile().delete();
                throw new IOException("Combined file had incorrect size!");
            }
        }

        LOG.info("Computing md5");
        try (InputStream is = new BufferedInputStream(new FileInputStream(targetFilename.toFile()))) {
            String newMd5Hash = MD5.computeHexHash(is);

            if (!newMd5Hash.equals(upload.getMd5Hash())) {
                throw new IOException("MD5 hash did not match!");
            }

            deleteDirectory(chunkDirectory.toFile());
        }
        LOG.info("Done");
    }

    // TODO: Refactor Pipeline and ParquetPipeline into a shared superclass (e.g., AbstractPipeline)
    // to reduce code duplication and simplify support for future formats.


    private static boolean processUpload(Connection connection, Upload upload) throws SQLException, UploadAlreadyLockedException {
        try (Upload.LockedUpload lockedUpload = upload.getLockedUpload(connection)) {
            try {
                tryCombinePieces(connection, upload, lockedUpload);
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
            } catch (IOException e) {
                lockedUpload.updateStatus(Upload.Status.FAILED_UNKNOWN);
                e.printStackTrace();
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

        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        LOG.info("extension: '" + extension + "'");

        Upload.Status status = Upload.Status.PROCESSED_OK;

        Exception uploadException = null;

        Map<String, FlightInfo> flightInfo = Collections.emptyMap();

        Map<String, UploadException> flightErrors = Collections.emptyMap();

        int validFlights = 0;
        int warningFlights = 0;
        int errorFlights = 0;

        if (extension.equals("zip")) {
            // Pipeline must be closed after use as it may open some files / resources.
            try (ZipFile zipFile = ZipFile.builder().setFile(filename).get(); Pipeline pipeline = new Pipeline(connection, upload, zipFile)) {
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


        }

        else if (extension.equals("parquet")) {

            LOG.info("Processing Parquet file: " + filename);

            Path parquetFilePath = Paths.get(filename);
            ParquetPipeline parquetPipeline = new ParquetPipeline(connection, upload, parquetFilePath);
            parquetPipeline.execute();
            flightInfo = parquetPipeline.getFlightInfo();
            flightErrors = parquetPipeline.getFlightErrors();
            errorFlights = flightErrors.size();
            warningFlights = parquetPipeline.getWarningFlightsCount();
            validFlights = parquetPipeline.getValidFlightsCount();

            LOG.info("Successfully processed Parquet file." + filename);


        } else {
            // insert an upload error for this upload
            status = Upload.Status.FAILED_ARCHIVE_TYPE;
            UploadError.insertError(connection, uploadId, "Uploaded file was not a zip or a parquet file.");

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
                if (exceptions.isEmpty()) {
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

                if (!exceptions.isEmpty()) {
                    for (MalformedFlightFileException exception : exceptions) {
                        uploadProcessedEmail.flightImportWarning(info.filename, exception.getMessage());
                    }
                }
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
