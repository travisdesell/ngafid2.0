package org.ngafid.uploads.process.format;

import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.InputFile;
import org.jline.utils.Log;
import org.ngafid.common.MD5;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Parameters;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.FlightMeta;

import java.io.IOException;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Processes a Parquet file and extracts flights.
 */
public class ParquetFileProcessor {
    private static final Logger LOG = Logger.getLogger(ParquetFileProcessor.class.getName());
    private final Connection connection;
    private final InputFile inputFile;
    private final String filename;
    private FlightMeta meta = new FlightMeta();

    public ParquetFileProcessor(Connection connection, InputFile inputFile, String filename) {
        this.connection = connection;
        this.inputFile = inputFile;
        this.filename = filename;
    }

    /**
     * Parses the Parquet file and extracts FlightBuilder instances.
     * @return A stream of FlightBuilder objects
     */
    public Stream<FlightBuilder> parse() {
        LOG.info("Parsing Parquet file: " + filename);
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();

        // Prepare lists to collect column data
        List<String> timeValues = new ArrayList<>();
        List<String> latitudeValues = new ArrayList<>();
        List<String> longitudeValues = new ArrayList<>();
        List<String> speedValues = new ArrayList<>();
        List<String> accelerationValues = new ArrayList<>();

        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(inputFile).build()) {
            GenericRecord record;
            while ((record = reader.read()) != null) {
                processMetaData(record);
                extractColumns(record, timeValues, latitudeValues, longitudeValues, speedValues, accelerationValues);

                //we process just one record for now
                break;
            }
        } catch (IOException e) {
            LOG.severe("Error reading Parquet file: " + e.getMessage());
            throw new RuntimeException("Failed to parse Parquet file", e);
        } catch (FatalFlightFileException e) {
            throw new RuntimeException(e);
        }

        // Convert collected data into time series

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, "second", new ArrayList<>(timeValues)));

        Log.info("Time first: " + timeValues.getFirst());
        Log.info("Time last: " + timeValues.getLast());
        Log.info("Number of time stamps: " + timeValues.size());

        doubleTimeSeries.put(Parameters.LATITUDE, new DoubleTimeSeries(Parameters.LATITUDE, "degree", new ArrayList<>(latitudeValues)));
        doubleTimeSeries.put(Parameters.LONGITUDE, new DoubleTimeSeries(Parameters.LONGITUDE, "degree", new ArrayList<>(longitudeValues)));
        doubleTimeSeries.put(Parameters.GND_SPD, new DoubleTimeSeries(Parameters.GND_SPD, "kt", new ArrayList<>(speedValues)));
        doubleTimeSeries.put(Parameters.LAT_AC, new DoubleTimeSeries(Parameters.LAT_AC, "m/s²", new ArrayList<>(accelerationValues)));

        return Stream.of(new FlightBuilder(meta, doubleTimeSeries, new HashMap<>()));
    }

    /**
     * Extracts full column data instead of row-wise extraction.
     */
    private void extractColumns(GenericRecord record,
                                List<String> timeValues,
                                List<String> latitudeValues,
                                List<String> longitudeValues,
                                List<String> speedValues,
                                List<String> accelerationValues) {

        List<?> pointsList = (List<?>) record.get("points");
        if (pointsList == null || pointsList.isEmpty()) {
            LOG.warning("Skipping record - 'points' field is empty.");
            return;
        }

        for (Object pointObject : pointsList) {
            if (!(pointObject instanceof GenericRecord pointRecord)) {
                LOG.warning("Skipping invalid point entry.");
                continue;
            }

            GenericRecord element = (GenericRecord) pointRecord.get("element");
            if (element == null) {
                LOG.warning("Skipping point - missing 'element' field.");
                continue;
            }

            // Collect values for each column
            timeValues.add(getStringOrNaN(element, "time"));
            latitudeValues.add(getStringOrNaN(element, "latitude"));
            longitudeValues.add(getStringOrNaN(element, "longitude"));
            speedValues.add(getStringOrNaN(element, "speed"));
            accelerationValues.add(getStringOrNaN(element, "acceleration"));
        }
    }

    /**
     * Processes metadata from the first Parquet record.
     */
    private void processMetaData(GenericRecord metadataRecord) throws FatalFlightFileException {
        if (metadataRecord == null) {
            throw new FatalFlightFileException("Metadata record is missing in the Parquet file.");
        }

        meta.filename = filename;
        meta.startDateTime = convertToOffsetDateTime(getLong(metadataRecord, "start_time"));
        meta.endDateTime = convertToOffsetDateTime(getLong(metadataRecord, "end_time"));

        String aircraftType = getString(metadataRecord, "aircraft_type");
        String modeSCode = getString(metadataRecord, "mode_s_code");

        meta.systemId = (modeSCode != null && !modeSCode.isEmpty()) ? modeSCode : "Unknown";
        meta.airframe = new Airframes.Airframe(
                (aircraftType != null && !aircraftType.isEmpty()) ? aircraftType : "Unknown",
                new Airframes.Type("Fixed Wing")
        );

        meta.md5Hash = computeMD5Hash(meta);
        LOG.info("Processed metadata: airframe=" + meta.airframe.getName() + ", system_id=" + meta.systemId );
    }

    /**
     * Converts a timestamp in milliseconds to OffsetDateTime.
     * @param timestamp The timestamp in milliseconds.
     * @return OffsetDateTime or null if invalid.
     */
    private OffsetDateTime convertToOffsetDateTime(long timestamp) {
        if (timestamp <= 0) return null;
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    /**
     * Extracts a String field from the record.
     * @param record The GenericRecord
     * @param fieldName The field to extract
     * @return String value or null if missing
     */
    private String getString(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * Extracts a Long field from the record.
     * @param record The GenericRecord
     * @param fieldName The field to extract
     * @return Long value or 0L if missing
     */
    private long getLong(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * Extracts a numeric field from the GenericRecord as a string or returns "NaN" if missing.
     * Used for column-based storage.
     */
    private String getStringOrNaN(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return (value instanceof Number) ? value.toString() : "NaN";
    }

    /**
     * Computes an MD5 hash using systemId and Airframe name.
     * @param meta FlightMeta object
     * @return MD5 hash string
     */
    private String computeMD5Hash(FlightMeta meta) {
        return MD5.computeHexHash(meta.systemId + meta.airframe.getName());
    }
}
