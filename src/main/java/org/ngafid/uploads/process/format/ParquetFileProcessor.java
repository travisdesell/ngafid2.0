package org.ngafid.uploads.process.format;

import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.InputFile;
import org.jline.utils.Log;
import org.ngafid.common.MD5;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.FlightMeta;

import java.io.IOException;
import java.sql.Connection;
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

    public ParquetFileProcessor(Connection connection, InputFile inputFile, String filename) {
        this.connection = connection;
        this.inputFile = inputFile;
        this.filename = filename;
    }

    public Stream<FlightBuilder> parse() {
        LOG.info("Parsing Parquet file: " + filename);
        List<FlightBuilder> flightBuilders = new ArrayList<>();

        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(inputFile).build()) {
            GenericRecord record;

           int counter = 0;
            while ((record = reader.read()) != null) {
                FlightMeta flightMeta = new FlightMeta();

                Log.info("Processing metadata for flight " + counter);
                processMetaData(record, flightMeta);

                Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
                Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

                populateTimeSeries(record, doubleTimeSeries, stringTimeSeries);

                int numberRows = doubleTimeSeries.get(Parameters.UNIX_TIME_SECONDS).size();
                LOG.info("Number of rows for flight " + counter + ": " + numberRows);


                flightBuilders.add(new ParquetFlightBuilder(flightMeta, doubleTimeSeries, stringTimeSeries));
                counter++;

                // How many flights we are processing (testing)
                  if (counter > 10) break;

            }
        } catch (IOException e) {
            LOG.severe("Error reading Parquet file: " + e.getMessage());
            throw new RuntimeException("Failed to parse Parquet file", e);
        } catch (FatalFlightFileException e) {
            throw new RuntimeException(e);
        }
        return flightBuilders.stream();
    }

    private double parseSafeDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }


    private void populateTimeSeries(GenericRecord record, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        DoubleTimeSeries timeValues = new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, "second");
        DoubleTimeSeries latitudeValues = new DoubleTimeSeries(Parameters.LATITUDE, "degree");
        DoubleTimeSeries longitudeValues = new DoubleTimeSeries(Parameters.LONGITUDE, "degree");
        DoubleTimeSeries speedValues = new DoubleTimeSeries(Parameters.GND_SPD, "kt");
        DoubleTimeSeries accelerationValues = new DoubleTimeSeries(Parameters.IAS, "kt"); // reused for LAT_AC
        DoubleTimeSeries altitudeValues = new DoubleTimeSeries(Parameters.ALT_AGL, "m");  // reused for ALT_MSL

        //String is always a reference type, no performance loss here
        List<String> utcDateTimes = new ArrayList<>();

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

            try {
                double unixTime = parseSafeDouble(getStringOrNaN(element, "time"));
                double timeInSeconds = (unixTime > 32503680000L) ? unixTime / 1000 : unixTime;
                timeValues.add(timeInSeconds);
                try {
                    utcDateTimes.add(TimeUtils.convertUnixTimeToUTCDateTime(timeInSeconds));
                } catch (NumberFormatException e) {
                    utcDateTimes.add("Error");
                }
            } catch (NumberFormatException e) {
                timeValues.add(Double.NaN);
                utcDateTimes.add("Error");
            }

            latitudeValues.add(parseSafeDouble(getStringOrNaN(element, "latitude")));
            longitudeValues.add(parseSafeDouble(getStringOrNaN(element, "longitude")));
            altitudeValues.add(parseSafeDouble(getStringOrNaN(element, "altitude")));
            speedValues.add(parseSafeDouble(getStringOrNaN(element, "speed")));
            accelerationValues.add(parseSafeDouble(getStringOrNaN(element, "acceleration")));
        }

        // Now just add them directly — no need to convert to arrays
        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, timeValues);
        doubleTimeSeries.put(Parameters.LATITUDE, latitudeValues);
        doubleTimeSeries.put(Parameters.LONGITUDE, longitudeValues);
        doubleTimeSeries.put(Parameters.GND_SPD, speedValues);
        doubleTimeSeries.put(Parameters.IAS, accelerationValues);
        doubleTimeSeries.put(Parameters.LAT_AC, accelerationValues); // reused
        doubleTimeSeries.put(Parameters.ALT_AGL, altitudeValues);
        doubleTimeSeries.put(Parameters.ALT_MSL, altitudeValues);    // reused

        stringTimeSeries.put(Parameters.UTC_DATE_TIME, new StringTimeSeries(Parameters.UTC_DATE_TIME, "ISO 8601", (ArrayList<String>) utcDateTimes));
    }




    /**
     * Processes metadata from the first Parquet record.
     */
    private void processMetaData(GenericRecord metadataRecord, FlightMeta flightMeta) throws FatalFlightFileException {
        if (metadataRecord == null) {
            throw new FatalFlightFileException("Metadata record is missing in the Parquet file.");
        }

        flightMeta.filename = filename;

        String aircraftType = getString(metadataRecord, "aircraft_type");
        String modeSCode = getString(metadataRecord, "mode_s_code");
        String primaryKey = getString(metadataRecord, "primary_key"); // Get primary key

        flightMeta.systemId = (modeSCode != null && !modeSCode.isEmpty()) ? modeSCode : "Unknown";
        flightMeta.airframe = new Airframes.Airframe(
                (aircraftType != null && !aircraftType.isEmpty()) ? aircraftType : "Unknown",
                new Airframes.Type("Fixed Wing")
        );

        flightMeta.md5Hash = computeMD5Hash(flightMeta, primaryKey);

        LOG.info("Processed metadata: airframe=" + flightMeta.airframe.getName() +
                ", system_id=" + flightMeta.systemId +
                ", primary_key=" + primaryKey);
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
    private String computeMD5Hash(FlightMeta meta, String primaryKey) {

        if (primaryKey == null) primaryKey = "UNKNOWN_KEY";

        String systemId = (meta.systemId != null) ? meta.systemId : "UNKNOWN_SYSTEM";
        String airframeName = (meta.airframe.getName() != null) ? meta.airframe.getName() : "UNKNOWN_AIRFRAME";

        return MD5.computeHexHash(primaryKey);
    }
    /**
    Converts list to primitive array
     */
    public static double[] toPrimitiveArray(List<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
