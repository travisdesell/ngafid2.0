package org.ngafid.uploads.process;

import org.apache.parquet.io.InputFile;
import org.ngafid.flights.Flight;
import org.ngafid.uploads.Upload;
import org.ngafid.uploads.UploadException;
import org.ngafid.uploads.process.format.ParquetFileProcessor;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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

        LOG.info("Processing completed. Flights processed: Valid=" + validFlights + ", Warnings=" + warningFlights + ", Errors=" + errorFlights);
    }

    /**
     * Processes the Parquet file, extracts flights, and inserts them into the database.
     */
    private void processParquetFile() throws UploadException {
        LOG.info("Reading Parquet file: " + parquetFilePath.toString());

        try {
            // Initialize ParquetFileProcessor
            InputFile inputFile = new NioInputFile(parquetFilePath);
            ParquetFileProcessor processor = new ParquetFileProcessor(connection, inputFile, parquetFilePath.getFileName().toString());

            // Parse flights from Parquet
            Stream<FlightBuilder> flightStream = processor.parse();
            List<FlightBuilder> flightBuilders = flightStream.toList();

            if (flightBuilders.isEmpty()) {
                LOG.warning("No valid flights extracted from Parquet file.");
                return;
            }

            // Build flights and insert into database
            for (FlightBuilder builder : flightBuilders) {
                try {
                    Flight flight = builder.build(connection).getFlight();

                    Flight.batchUpdateDatabase(connection, List.of(flight));
                    validFlights++;

                    LOG.info("Successfully processed flight: " + flight.getFilename());
                } catch (Exception e) {
                    LOG.severe("Error processing a flight: " + e.getMessage());
                    errorFlights++;
                }
            }


        } catch (Exception e) {
            LOG.severe("Error processing Parquet file: " + e.getMessage());
            throw new UploadException("Failed to process Parquet file: " + e.getMessage(), e, parquetFilePath.toString());
        }
    }
}
