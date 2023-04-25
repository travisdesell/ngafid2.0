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

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.Upload;
import org.ngafid.flights.UploadError;
import org.ngafid.flights.process.*;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;


@FunctionalInterface
interface FlightFileProcessors {
    FlightFileProcessor create(InputStream stream, String filename, Object... args);
}

public class ProcessUpload {
    private static Connection connection = null;
    private static Logger LOG = Logger.getLogger(ProcessUpload.class.getName());
    private static final String ERROR_STATUS_STR = "ERROR";
    private static final Map<String, FlightFileProcessors> PROCESSORS;

    static {
        PROCESSORS = Map.of(
            "csv", CSVFileProcessor::new,
            "gpx", GPXFileProcessor::new,
            "json", JSONFileProcessor::new,
            "dat", DATFileProcessor::new
        );
    }

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
        List<MalformedFlightFileException> exceptions;

        public FlightInfo(int id, int length, String filename, List<MalformedFlightFileException> exceptions) {
            this.id = id;
            this.length = length;
            this.filename = filename;
            this.exceptions = exceptions;
        }
    }

    interface CheckedFunction<T, R, E extends Exception> {
        public R apply(T t) throws E;
    }

    static class CheckedMap<T, R, E extends Exception> implements Function<T, R> {
        final BiConsumer<T, E> exceptionHandler;
        final CheckedFunction<T, R, E> f;

        public CheckedMap(CheckedFunction<T, R, E> f, BiConsumer<T, E> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            this.f = f;
        }

        public R apply(T t) {
            try {
                return f.apply(t);
            } catch (Error | RuntimeException e) {
                throw e;
            } catch (Exception ex) {
                @SuppressWarnings("unchecked") E e = (E) ex;
                exceptionHandler.accept(t, e);
                return null;
            }
        }
    }


    // Generates a function which when called will call the supplied function f which may raise an exception. 
    // In the event of an exception the exception the exceptionHandler is called and supplied the value T as 
    // well as the exception object, and finally null is returned.
    private static <T, R, E extends Exception> CheckedMap<T, R, E> mapOrNull(CheckedFunction<T, R, E> f, BiConsumer<T, E> exceptionHandler) {
        return new CheckedMap<T, R, E>(f, exceptionHandler);
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
            BiConsumer<ZipEntry, FlightFileFormatException> handleFlightFileFormatException =
                (z, e) -> {
                    flightErrors.put(z.getName(), new UploadException("Unknown file type contained in zip file (flight logs should be .csv files).", z.getName()));
                    errorFlights++;
                };
    
            BiConsumer<FlightFileProcessor, FlightProcessingException> handleExceptionInProcessor =
                (p, e) -> {
                    flightErrors.put(p.filename, new UploadException(e.getMessage(), e, p.filename));
                    errorFlights++;
                };
            BiConsumer<FlightBuilder, FlightProcessingException> handleExceptionInBuilder =
                (b, e) -> {
                    flightErrors.put(b.meta.filename, new UploadException(e.getMessage(), e, b.meta.filename));
                    errorFlights++;
                };

            try {
                System.err.println("processing zip file: '" + filename + "'");
                ZipFile zipFile = new ZipFile(filename);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                Stream<? extends ZipEntry> validFiles = 
                    StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                        false
                    )
                    .filter(z -> !z.getName().contains("__MACOSX"))
                    .filter(z -> !z.isDirectory());

                Stream<Flight> pipeline =
                    validFiles
                    .map(mapOrNull(FlightFileProcessor::create, handleFlightFileFormatException)) // Create a FlightFileProcessor for each file
                    .filter(Objects::nonNull) // Filter out any null values (nulls indicate files we cant process)
                    .map(mapOrNull(p -> p.parse(), handleExceptionInProcessor)) // Parse the files (this is the initial parsing step
                    .filter(Objects::nonNull) // Filter out any null values (nulls indicate something went awry in the parsing step)
                    .flatMap(builder -> builder) // Merge streams together
                    .map(mapOrNull(builder -> builder.build(connection), handleExceptionInBuilder)) // 
                    .filter(Objects::nonNull);
                
                pipeline.forEach((Flight flight) -> {
                    flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                    if (flight.getStatus().equals("WARNING")) warningFlights++;
                });
                  
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (entry.isDirectory() || name.contains("__MACOSX")) {
                        //System.err.println("SKIPPING: " + entry.getName());
                        continue;
                    }

                    System.err.println("PROCESSING: " + name); // TODO: Use a logger

                    String entryName = entry.getName();
                    String entryExtension = entryName.substring(entryName.lastIndexOf("."), entryName.length()).toLowerCase();

                    if (!PROCESSORS.containsKey(entryExtension)) {
                        flightErrors.put(entry.getName(), new UploadException("Unknown file type contained in zip file", entry.getName()));
                        errorFlights.getAndIncrement();
                        continue;
                    }

                    FlightFileProcessor processor = PROCESSORS.get(entryExtension).create(zipFile.getInputStream(entry), entry.getName(), new Object[]{zipFile});
                    Stream<FlightBuilder> flights = processor.parse();

                    // } else if (entry.getName().endsWith(".DAT")) {
                    //     String zipName = entry.getName().substring(entry.getName().lastIndexOf("/"));
                    //     String parentFolder = zipFile.getName().substring(0, zipFile.getName().lastIndexOf("/"));
                    //     File tempExtractedFile = new File(parentFolder, zipName);

                    //     System.out.println("Extracting to " + tempExtractedFile.getAbsolutePath());
                    //     try (InputStream inputStream = zipFile.getInputStream(entry); FileOutputStream fileOutputStream = new FileOutputStream(tempExtractedFile)) {
                    //         int len;
                    //         byte[] buffer = new byte[1024];

                    //         while ((len = inputStream.read(buffer)) > 0) {
                    //             fileOutputStream.write(buffer, 0, len);
                    //         }
                    //     }

                    //     convertDATFile(tempExtractedFile);
                    //     File processedCSVFile = new File(tempExtractedFile.getAbsolutePath() + ".csv");
                    //     placeInZip(processedCSVFile.getAbsolutePath(), zipFile.getName().substring(zipFile.getName().lastIndexOf("/") + 1));

                    //     try (InputStream stream = new FileInputStream(processedCSVFile)) {
                    //         Flight flight = processDATFile(fleetId, entry.getName(), stream, connection);

                    //         if (connection != null) {
                    //             flight.updateDatabase(connection, uploadId, uploaderId, fleetId);
                    //         }

                    //         if (flight.getStatus().equals("WARNING")) warningFlights++;

                    //         flightInfo.add(new FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

                    //         validFlights++;
                }

            } catch (java.nio.file.NoSuchFileException e) {
                System.err.println("NoSuchFileException: " + e);
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
            } catch (FlightProcessingException e) {
                LOG.log(Level.SEVERE, "FlightProcessingException: {0}", e.toString());
                e.printStackTrace();

                UploadError.insertError(connection, uploadId, "Got an exception while parsing data");
                status = ERROR_STATUS_STR;
                uploadException = new Exception(e + "exception while parsing data");
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
        updateStatement.setInt(3, warningFlights.get());
        updateStatement.setInt(4, errorFlights.get());
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

                List<MalformedFlightFileException> exceptions = info.exceptions;
                if (exceptions.size() == 0) {
                    uploadProcessedEmail.flightImportOK(info.filename);
                }
            }

            uploadProcessedEmail.setErrorFlights(errorFlights.get());

            for (Map.Entry<String, UploadException> entry : flightErrors.entrySet()) {
                UploadException exception = entry.getValue();

                uploadProcessedEmail.flightImportError(exception.getFilename(), exception.getMessage());
            }

            uploadProcessedEmail.setWarningFlights(warningFlights.get());

            for (FlightInfo info : flightInfo) {
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

    private static void placeInZip(String file, String zipFileName) throws IOException {
        LOG.info("Placing " + file + " in zip");
        
        Map<String, String> zipENV = new HashMap<>();
        zipENV.put("create", "true");

        Path csvFilePath = Paths.get(file);
        Path zipFilePath = Paths.get(csvFilePath.getParent() + "/" + zipFileName);

        URI zipURI = URI.create("jar:" + zipFilePath.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(zipURI, zipENV)) {
            Path zipFileSystemPath = fileSystem.getPath(file.substring(file.lastIndexOf("/") + 1));
            Files.write(zipFileSystemPath, Files.readAllBytes(csvFilePath), StandardOpenOption.CREATE);
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
