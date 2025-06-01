package org.ngafid.processor;


import org.apache.parquet.io.InputFile;
import org.ngafid.core.Database;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.FlightProcessingException;
import org.ngafid.core.uploads.Upload;
import org.ngafid.core.uploads.UploadException;
import org.ngafid.processor.format.FlightBuilder;
import org.ngafid.processor.format.ParquetFileProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Processes a Parquet file by parsing its records, converting them to flights, and inserting them into the database.
 */
public class ParquetPipeline {
    private static final Logger LOG = Logger.getLogger(ParquetPipeline.class.getName());

    private final Connection connection;
    private final Upload upload;
    private final Path parquetFilePath;

    private int validFlights = 0;
    private int warningFlights = 0;
    private int errorFlights = 0;

    // Maps filenames to a FlightInfo object (like in original Pipeline)
    private final Map<String, ProcessUpload.FlightInfo> flightInfo = new ConcurrentHashMap<>();

    // Maps filenames to the exception that caused processing to fail (like in original Pipeline)
    private final Map<String, UploadException> flightErrors = new ConcurrentHashMap<>();

    private static ForkJoinPool pool = null;




    public ParquetPipeline(Connection connection, Upload upload, Path parquetFilePath) {
        this.connection = connection;
        this.upload = upload;
        this.parquetFilePath = parquetFilePath;
    }



    /**
     * Main execution method for processing the Parquet file.
     */
    public void execute() {
        LOG.info("Starting processing for Parquet file: " + parquetFilePath.toString());

        try {
            //processParquetFileSequential();
            processParquetFileParallel();
        } catch (UploadException e) {
            LOG.severe("Failed to process Parquet file: " + e.getMessage());
        }

        errorFlights = flightErrors.size();

        LOG.info("Processing completed. Flights processed: Valid=" + validFlights + ", Warnings=" + warningFlights + ", Errors=" + errorFlights);
    }

    private void processParquetFileParallel() throws UploadException {
        LOG.info("Reading Parquet file: " + parquetFilePath.toString());

        try {
            InputFile inputFile = new NioInputFile(parquetFilePath);
            ParquetFileProcessor processor = new ParquetFileProcessor(connection, inputFile, parquetFilePath.getFileName().toString());

            Stream<FlightBuilder> flightStream = processor.parse();
            List<FlightBuilder> flightBuilders = flightStream.toList();

            LOG.info(flightBuilders.size() + " flights extracted from Parquet file.");

            if (flightBuilders.isEmpty()) {
                LOG.warning("No valid flights extracted from Parquet file.");
                return;
            }

            // Build flights in parallel
            List<FlightBuilder> successfulBuilders = flightBuilders.parallelStream()
                    .map(fb -> {
                        try (Connection threadConn = Database.getConnection()) {
                            fb.meta.setFleetId(upload.fleetId);
                            fb.meta.setUploaderId(upload.uploaderId);
                            fb.meta.setUploadId(upload.id);
                            fb.meta.airframe = new Airframes.Airframe(threadConn, fb.meta.airframe.getName(), fb.meta.airframe.getType());
                            fb.build(threadConn);
                            return fb;
                        } catch (Exception e) {
                            LOG.severe("Error building flight " + fb.meta.filename + ": " + e.getMessage());
                            flightErrors.put(fb.meta.filename, new UploadException(e.getMessage(), e, fb.meta.filename));
                            errorFlights++;
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (successfulBuilders.isEmpty()) {
                LOG.severe("All flight builds failed.");
                return;
            }

            //  Insert flights into DB (main thread)
            final int BATCH_SIZE = 10;
            List<Flight> buffer = new ArrayList<>(BATCH_SIZE);

            for (FlightBuilder fb : successfulBuilders) {
                buffer.add(fb.getFlight());

                if (buffer.size() == BATCH_SIZE) {
                    Flight.batchUpdateDatabase(connection, buffer);
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                Flight.batchUpdateDatabase(connection, buffer);
            }

            for (FlightBuilder fb : successfulBuilders) {
                finalizeFlight(fb);
            }

        } catch (IOException | SQLException | FlightProcessingException e) {
            LOG.severe("Error processing Parquet file: " + e.getMessage());
            throw new UploadException("Failed to process Parquet file: " + e.getMessage(), e, parquetFilePath.toString());
        }
    }


    private void processParquetFileSequential() throws UploadException {
        LOG.info("Reading Parquet file: " + parquetFilePath.toString());

        try {
            InputFile inputFile = new NioInputFile(parquetFilePath);
            ParquetFileProcessor processor = new ParquetFileProcessor(connection, inputFile, parquetFilePath.getFileName().toString());

            Stream<FlightBuilder> flightStream = processor.parse();
            List<FlightBuilder> flightBuilders = flightStream.toList();
            LOG.info( flightBuilders.size() + " flights extracted from Parquet file.");

            if (flightBuilders.isEmpty()) {
                LOG.warning("No valid flights extracted from Parquet file.");
                return;
            }

            // Build flights and insert them into the database
            List<Flight> flights = new ArrayList<>();
            for (FlightBuilder fb : flightBuilders) {
                Flight flight = buildFlight(connection, fb);
                if (flight != null) {
                    flights.add(flight);
                    finalizeFlight(fb);
                } else {
                    errorFlights++;
                }
            }

            if (!flights.isEmpty()) {

                //Insert flights into database in batches of 10
                final int BATCH_SIZE = 10;
                List<Flight> buffer = new ArrayList<>(BATCH_SIZE);

                for (Flight flight : flights) {
                    buffer.add(flight);

                    if (buffer.size() == BATCH_SIZE) {
                        Flight.batchUpdateDatabase(connection, buffer);
                        buffer.clear();
                    }
                }

                // remaining flights
                if (!buffer.isEmpty()) {
                    Flight.batchUpdateDatabase(connection, buffer);
                }
            }else{
                LOG.severe("Flights are empty!");
            }
        } catch (IOException | SQLException | FlightProcessingException e) {
            LOG.severe("Error processing Parquet file: " + e.getMessage());
            throw new UploadException("Failed to process Parquet file: " + e.getMessage(), e, parquetFilePath.toString());
        }
    }

    /**
     * Builds a Flight object from a FlightBuilder, setting necessary metadata.
     *
     * @param connection The database connection.
     * @param fb The FlightBuilder instance.
     * @return The built Flight object or null if an error occurs.
     */
    private Flight buildFlight(Connection connection, FlightBuilder fb) {
        try {
            fb.meta.setFleetId(this.upload.fleetId);
            fb.meta.setUploaderId(this.upload.uploaderId);
            fb.meta.setUploadId(this.upload.id);
            fb.meta.airframe = new Airframes.Airframe(connection, fb.meta.airframe.getName(), fb.meta.airframe.getType());

            return fb.build(connection).getFlight();
        } catch (SQLException | FlightProcessingException e) {
            LOG.severe("Error building flight '" + fb.meta.filename + "': " + e.getMessage());
            flightErrors.put(fb.meta.filename, new UploadException(e.getMessage(), e, fb.meta.filename));
            errorFlights++;
            return null;
        }
    }

    /**
     * Finalizes a flight by categorizing it as valid or warning and storing necessary metadata.
     *
     * @param builder The FlightBuilder instance.
     */
    private void finalizeFlight(FlightBuilder builder) {
        Flight flight = builder.getFlight();
        //  LOG.info("Finalizing flight: " + flight.getFilename());

        if (flight.getStatus().equals("WARNING")) {
            warningFlights++;
        } else {
            validFlights++;
        }

        flightInfo.put(flight.getFilename(), new ProcessUpload.FlightInfo(
                flight.getId(),
                flight.getNumberRows(),
                flight.getFilename(),
                flight.getExceptions()
        ));

        // LOG.info("Flight " + flight.getFilename() + " finalized. Status: " + flight.getStatus());
    }

    /**
     * Get processed flight information.
     *
     * @return Map of filenames to FlightInfo objects.
     */
    public Map<String, ProcessUpload.FlightInfo> getFlightInfo() {
        return flightInfo;
    }

    /**
     * Get flight processing errors.
     *
     * @return Map of filenames to UploadException objects.
     */
    public Map<String, UploadException> getFlightErrors() {
        return Collections.unmodifiableMap(flightErrors);
    }

    /**
     * Get the count of flights that triggered warnings.
     *
     * @return Number of flights with warnings.
     */
    public int getWarningFlightsCount() {
        return warningFlights;
    }

    /**
     * Get the count of valid flights.
     *
     * @return Number of valid flights.
     */
    public int getValidFlightsCount() {
        return validFlights;
    }
}
