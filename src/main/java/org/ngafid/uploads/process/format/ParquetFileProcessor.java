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


                flightBuilders.add(new FlightBuilder(flightMeta, doubleTimeSeries, stringTimeSeries));
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

    private void populateTimeSeries(GenericRecord record, Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {

        // Collect data for each time series
        List<Double> timeValues = new ArrayList<>();
        List<Double> latitudeValues = new ArrayList<>();
        List<Double> longitudeValues = new ArrayList<>();
        List<Double> speedValues = new ArrayList<>();
        List<Double> accelerationValues = new ArrayList<>();
        List<Double> altitudeValues = new ArrayList<>();
        List<String> utcDateTimes = new ArrayList<>();

        extractColumns(record,
                timeValues,
                latitudeValues,
                longitudeValues,
                altitudeValues,
                speedValues,
                accelerationValues);

        // Convert Unix timestamps to UTC DateTime
        for (Double unixTime : timeValues) {
            try {
                String utcDateTime = TimeUtils.convertUnixTimeToUTCDateTime(unixTime);
                utcDateTimes.add(utcDateTime);
            } catch (NumberFormatException e) {
                LOG.warning("Skipping invalid Unix time: " + unixTime);
                utcDateTimes.add("Error");  // Add empty value if conversion fails
            }
        }

        // Populate Double Time Series
        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, "second", toPrimitiveArray(timeValues), timeValues.size()));
        doubleTimeSeries.put(Parameters.LATITUDE, new DoubleTimeSeries(Parameters.LATITUDE, "degree", toPrimitiveArray(latitudeValues), latitudeValues.size()));
        doubleTimeSeries.put(Parameters.LONGITUDE, new DoubleTimeSeries(Parameters.LONGITUDE, "degree",  toPrimitiveArray(longitudeValues), longitudeValues.size()));
        doubleTimeSeries.put(Parameters.GND_SPD, new DoubleTimeSeries(Parameters.GND_SPD, "kt", toPrimitiveArray(speedValues), speedValues.size()));


        // Note! IAS and LAT_AC are the same values
        doubleTimeSeries.put(Parameters.IAS, new DoubleTimeSeries(Parameters.IAS, "kt", toPrimitiveArray(accelerationValues), accelerationValues.size()));
        doubleTimeSeries.put(Parameters.LAT_AC, new DoubleTimeSeries(Parameters.LAT_AC, "m/s", toPrimitiveArray(accelerationValues), accelerationValues.size()));

        // Note! ALT_AGL and ALT_MSL are the same values
        doubleTimeSeries.put(Parameters.ALT_AGL, new DoubleTimeSeries(Parameters.ALT_AGL, "m",toPrimitiveArray(altitudeValues), altitudeValues.size()));
        doubleTimeSeries.put(Parameters.ALT_MSL, new DoubleTimeSeries(Parameters.ALT_MSL, "m",toPrimitiveArray(altitudeValues), altitudeValues.size()));

        // Store UTC_DATE_TIME in String Time Series
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, new StringTimeSeries(Parameters.UTC_DATE_TIME, "ISO 8601", new ArrayList<>(utcDateTimes)));
        doubleTimeSeries.put(Parameters.ALT_AGL, new DoubleTimeSeries(Parameters.ALT_AGL, "m", toPrimitiveArray(altitudeValues), altitudeValues.size()));

    }

    /**
     * Extracts full column data from the parquet files.
     */
    private void extractColumns(GenericRecord record,
                                List<Double> timeValues,
                                List<Double> latitudeValues,
                                List<Double> longitudeValues,
                                List<Double> altitudeValues,
                                List<Double> speedValues,
                                List<Double> accelerationValues) {

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
            // Parquet files have time values in milliseconds, convert them to seconds.
            String rawTime = getStringOrNaN(element, "time");

            try {
                double timeValue = Double.parseDouble(rawTime);
                double timeInSeconds = (timeValue > 32503680000L) ? timeValue / 1000 : timeValue;
                timeValues.add(timeInSeconds);
            } catch (NumberFormatException e) {
                LOG.warning("Invalid time format, setting as NaN: " + rawTime);

            }

            latitudeValues.add(Double.valueOf(getStringOrNaN(element, "latitude")));
            longitudeValues.add(Double.valueOf(getStringOrNaN(element, "longitude")));
            altitudeValues.add(Double.valueOf(getStringOrNaN(element, "altitude")));
            speedValues.add(Double.valueOf(getStringOrNaN(element, "speed")));
            accelerationValues.add(Double.valueOf(getStringOrNaN(element, "acceleration")));
        }
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

        return MD5.computeHexHash(systemId + airframeName + primaryKey+ "sdfd");
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
