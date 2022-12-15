package org.ngafid;

import java.io.*;

import java.net.URI;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.SendEmail;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.Upload;
import org.ngafid.flights.UploadError;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;

public class ProcessUpload {
    private static Connection connection = null;

    public static void main(String[] arguments) {
        System.out.println("arguments are:");
        System.out.println(Arrays.toString(arguments));

        connection = Database.getConnection();

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

    public static void operateAsDaemon() {
        while (true) {
            connection = Database.resetConnection();

            Instant start = Instant.now();

            try {
                PreparedStatement fleetPreparedStatement = connection.prepareStatement("SELECT id FROM fleet WHERE id != 107 AND EXISTS (SELECT id FROM uploads WHERE fleet.id = uploads.fleet_id AND uploads.status = 'UPLOADED')");
                ResultSet fleetSet = fleetPreparedStatement.executeQuery();

                while (fleetSet.next()) {
                    int targetFleetId = fleetSet.getInt(1);
                    System.err.println("Importing an upload from fleet: " + targetFleetId);

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

                    //TURN OFF FOR REGULAR USE
                    //System.exit(1);
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
            System.err.println("finished in " + elapsed_seconds);

            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }

    public static void processFleetUploads(int fleetId) {
        System.out.println("processing uploads from fleet with id: " + fleetId);
        try {
            Fleet fleet = Fleet.get(connection, fleetId);
            String f = fleet.getName() == null ? " NULL NAME " : fleet.getName();
            ArrayList<Upload> uploads = Upload.getUploads(connection, fleetId);
            System.out.print("Found " + uploads.size() + " uploads from fleet " + f + ". Would you like to reimport them? [Y/n] ");
            Console con = System.console();
            String s = con.readLine("");
            if (s.toLowerCase().startsWith("y")) {
                for (Upload upload : uploads) {
                    if (upload == null) {
                        System.err.println("Encountered a null upload. this should never happen, but moving on to the next upload");
                        continue;
                    }
                    processUpload(upload);
                }
            }
        } catch (SQLException e) {
            System.err.println("Encountered error");
            e.printStackTrace();
        }
    }

    public static void processUpload(int uploadId) {
        System.out.println("processing upload with id: " + uploadId);
        try {
            Upload upload = Upload.getUploadById(connection, uploadId);

            if (upload == null) {
                //An upload with this id did not exist in the database, report an error. This should not happen.
                System.err.println("ERROR: attempted to importing an upload with upload id " + uploadId + ", but the upload did not exist. This should never happen.");
                System.exit(1);
            }
            processUpload(upload);
        } catch (SQLException e) {
            System.err.println("ERROR processing upload: " + e);
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
            System.out.println("upload was reset!\n\n");


            UploadProcessedEmail uploadProcessedEmail = new UploadProcessedEmail(recipients, bccRecipients);

            boolean success = ingestFlights(connection, uploadId, fleetId, uploaderId, filename, uploadProcessedEmail);

            //only progress if the upload ingestion was successful
            if (success) {
                FindSpinEvents.findSpinEventsInUpload(upload);

                CalculateExceedences.calculateExceedences(connection, uploadId, uploadProcessedEmail);

                CalculateProximity.calculateProximity(connection, uploadId, uploadProcessedEmail);

                CalculateTTF.calculateTTF(connection, uploadId, uploadProcessedEmail);

                String endTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss z (O)"));

                uploadProcessedEmail.setSubject("NGAFID import of '" + filename + "' email at " + endTime);

            } else {
                uploadProcessedEmail.setSubject("NGAFID upload '" + filename + "' ERROR on import");
            }

            uploadProcessedEmail.sendEmail();

        } catch (SQLException e) {
            System.err.println("ERROR processing upload: " + e);
            e.printStackTrace();
        }
    }

    private static class FlightInfo {
        /**
         * This is a helper class so we don't keep all loaded flights in memory.
         */

        int id;
        int length;
        String filename;
        ArrayList<MalformedFlightFileException> exceptions = new ArrayList<MalformedFlightFileException>();

        public FlightInfo(int id, int length, String filename, ArrayList<MalformedFlightFileException> exceptions) {
            this.id = id;
            this.length = length;
            this.filename = filename;
            this.exceptions = exceptions;
        }
    }


    public static boolean ingestFlights(Connection connection, int uploadId, int fleetId, int uploaderId, String filename, UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();

        filename = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId + "/" + uploadId + "__" + filename;
        System.err.println("processing: '" + filename + "'");

        String extension = filename.substring(filename.length() - 4);
        System.err.println("extension: '" + extension + "'");

        String status = "IMPORTED";

        Exception uploadException = null;

        ArrayList<FlightInfo> flightInfo = new ArrayList<FlightInfo>();

        HashMap<String, UploadException> flightErrors = new HashMap<String, UploadException>();

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

                    String entryName = entry.getName();

                    if (entryName.contains(".csv")) {
                        try {
                            InputStream stream = zipFile.getInputStream(entry);
                            Flight flight = new Flight(fleetId, entry.getName(), stream, connection);

                            if (connection != null) {
                                flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                            }

                            if (flight.getStatus().equals("WARNING")) warningFlights++;

                            flightInfo.add(new FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

                            validFlights++;
                        } catch (IOException | FatalFlightFileException | FlightAlreadyExistsException e) {
                            System.err.println(e.getMessage());
                            flightErrors.put(entry.getName(), new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        }

                    } else if (entryName.contains(".gpx")) {
                        try {
                            InputStream stream = zipFile.getInputStream(entry);
                            ArrayList<Flight> flights = Flight.processGPXFile(fleetId, connection, stream, entry.getName());

                            if (connection != null) {
                                for (Flight flight : flights) {
                                    flightInfo.add(new FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));
                                }
                                for (Flight flight : flights) {
                                    flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                                    if (flight.getStatus().equals("WARNING")) warningFlights++;
                                    validFlights++;
                                }
                            }
                        } catch (IOException | FatalFlightFileException | FlightAlreadyExistsException |
                                 ParserConfigurationException | SAXException | SQLException | ParseException e) {
                            System.err.println(e.getMessage());
                            flightErrors.put(entry.getName(), new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        }
                    } else if (entry.getName().endsWith(".json")) {
                        try {
                            Flight flight = Flight.processJSON(fleetId, connection, zipFile.getInputStream(entry), entry.getName());

                            if (connection != null) {
                                flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                            }

                            if (flight.getStatus().equals("WARNING")) warningFlights++;

                            validFlights++;
                        } catch (IOException | FatalFlightFileException | FlightAlreadyExistsException |
                                 ParseException e) {
                            System.err.println("ERROR: " + e.getMessage());
                            flightErrors.put(entry.getName(), new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        }
                    } else if (entry.getName().endsWith(".DAT")) {
                        String zipName = entry.getName().substring(entry.getName().lastIndexOf("/"));
                        String parentFolder = zipFile.getName().substring(0, zipFile.getName().lastIndexOf("/"));
                        File tempExtractedFile = new File(parentFolder, zipName);

                        System.out.println("Extracting to " + tempExtractedFile.getAbsolutePath());
                        try (InputStream inputStream = zipFile.getInputStream(entry); FileOutputStream fileOutputStream = new FileOutputStream(tempExtractedFile)) {
                            int len;
                            byte[] buffer = new byte[1024];

                            while ((len = inputStream.read(buffer)) > 0) {
                                fileOutputStream.write(buffer, 0, len);
                            }
                        }

                        convertDATFile(tempExtractedFile);
                        File processedCSVFile = new File(tempExtractedFile.getAbsolutePath() + ".csv");
                        Thread thread = new Thread(() -> {
                            try {
                                placeInZip(processedCSVFile.getAbsolutePath(),  zipFile.getName().substring(zipFile.getName().lastIndexOf("/") + 1));
                            } catch (IOException e) {
                                System.err.println("Error placing converted DAT back into zip file: " + e.getMessage());
                            }
                        });
                        thread.start();

                        try (InputStream stream = new FileInputStream(processedCSVFile)) {
                            Flight flight = new Flight(fleetId, entry.getName(), stream, connection);

                            if (connection != null) {
                                flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                            }

                            if (flight.getStatus().equals("WARNING")) warningFlights++;

                            flightInfo.add(new FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

                            validFlights++;
                        } catch (IOException | FatalFlightFileException | FlightAlreadyExistsException | SQLException e) {
                            System.err.println(e.getMessage());
                            flightErrors.put(entry.getName(), new UploadException(e.getMessage(), e, entry.getName()));
                            errorFlights++;
                        }

                        processedCSVFile.delete();
                        tempExtractedFile.delete();
                    } else {
                        flightErrors.put(entry.getName(), new UploadException("Unknown file type contained in zip file (flight logs should be .csv files).", entry.getName()));
                        errorFlights++;
                    }
                }

            } catch (java.nio.file.NoSuchFileException e) {
                System.err.println("NoSuchFileException: " + e);
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Broken upload: please delete this upload and re-upload.");
                status = "ERROR";
                uploadException = new Exception(e.toString() + ", broken upload: please delete this upload and re-upload.");

            } catch (IOException e) {
                System.err.println("IOException: " + e);
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Could not read from zip file: please delete this upload and re-upload.");
                status = "ERROR";
                uploadException = new Exception(e.toString() + ", could not read from zip file: please delete this upload and re-upload.");
            } catch (NotDatFile e) {
                System.err.println("NotDatFile: " + e);
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Tried to process a non-DAT file as a DAT file.");
                status = "ERROR";
                uploadException = new Exception(e + ", tried to process a non-DAT file as a DAT file.");
            } catch (FileEnd e) {
                System.err.println("FileEnd: " + e);
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Reached the end of a file while doing DAT processing");
                status = "ERROR";
                uploadException = new Exception(e + ", reached the end of a file while doing DAT processing");
            }

        } else {
            //insert an upload error for this upload
            status = "ERROR";
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
            System.err.println("email in " + elapsed_seconds);
            uploadProcessedEmail.setImportElapsedTime(elapsed_seconds);

            System.out.println("valid flights: " + validFlights);
            System.out.println("warning flights: " + warningFlights);
            System.out.println("error flights: " + errorFlights);

            uploadProcessedEmail.setValidFlights(validFlights);
            //iterate over all the flights without warnings
            for (FlightInfo info : flightInfo) {
                uploadProcessedEmail.addFlight(info.filename, info.id, info.length);

                ArrayList<MalformedFlightFileException> exceptions = info.exceptions;
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

            for (FlightInfo info : flightInfo) {
                ArrayList<MalformedFlightFileException> exceptions = info.exceptions;

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

    private static void placeInZip(String file, String zipFileName) throws IOException {
        System.out.println("Placing " + file + " in zip");

        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        Path filePath = Paths.get(file);
        Path path = Paths.get(filePath.getParent() + "/" + zipFileName);

        URI uri = URI.create("jar:" + path.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
            Path fileSystemPath = fileSystem.getPath(file.substring(file.lastIndexOf("/") + 1));
            Files.write(fileSystemPath, Files.readAllBytes(filePath), StandardOpenOption.CREATE);
        }
    }

    private static File convertDATFile(File file) throws NotDatFile, IOException, FileEnd {
        System.out.println("Converting to CSV: " + file.getAbsolutePath());
        DatFile datFile = DatFile.createDatFile(file.getAbsolutePath());
        datFile.reset();
        datFile.preAnalyze();

        ConvertDat convertDat = datFile.createConVertDat();

        String csvFilename = file.getAbsolutePath() + ".csv";
        convertDat.csvWriter = new CsvWriter(csvFilename);
        convertDat.createRecordParsers();

        datFile.reset();
        AnalyzeDatResults results = convertDat.analyze(false);
        System.out.println(datFile.getFile().getAbsolutePath());

        return datFile.getFile();
    }
}
