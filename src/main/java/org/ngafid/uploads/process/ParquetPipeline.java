package org.ngafid.uploads.process;

import org.apache.parquet.io.InputFile;
import org.jline.utils.Log;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.Flight;
import org.ngafid.uploads.Upload;
import org.ngafid.uploads.UploadException;
import org.ngafid.uploads.process.format.ParquetFileProcessor;
import org.ngafid.uploads.process.format.FlightBuilder;
import org.ngafid.uploads.ProcessUpload;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
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
            processParquetFile();
        } catch (Exception e) {
            LOG.severe("Failed to process Parquet file: " + e.getMessage());
        }

        errorFlights = flightErrors.size();  // Track error flights

        LOG.info("Processing completed. Flights processed: Valid=" + validFlights + ", Warnings=" + warningFlights + ", Errors=" + errorFlights);
    }

    private void processParquetFile() throws UploadException {
        LOG.info("Reading Parquet file: " + parquetFilePath.toString());

        try {
            InputFile inputFile = new NioInputFile(parquetFilePath);
            ParquetFileProcessor processor = new ParquetFileProcessor(connection, inputFile, parquetFilePath.getFileName().toString());

            Stream<FlightBuilder> flightStream = processor.parse();
            List<FlightBuilder> flightBuilders = flightStream.toList();

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
                    finalizeFlight(fb);  // NEW: Track flight metadata and validity
                } else {
                    errorFlights++;
                }
            }

            if (!flights.isEmpty()) {

                for (Flight flight : flights) {
                    Log.info("Parquet pipeline");
                    LOG.info("Preparing to insert flight: " + flight.getFilename());
                    LOG.info("  Flight ID (pre-insert): " + flight.getId());
                    LOG.info("  Number of rows: " + flight.getNumberRows());
                    LOG.info("  DoubleTimeSeries keys: " + flight.getDoubleTimeSeriesMap().keySet());
                    LOG.info("  StringTimeSeries keys: " + flight.getStringTimeSeriesMap().keySet());
                }
                Flight.batchUpdateDatabase(connection, flights);
            }else{
                LOG.severe("Flights are empty!!!");
            }

        } catch (Exception e) {
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
        } catch (Exception e) {
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

        // Store flight info
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
