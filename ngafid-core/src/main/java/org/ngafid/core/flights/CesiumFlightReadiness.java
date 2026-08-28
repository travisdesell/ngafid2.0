package org.ngafid.core.flights;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.ngafid.core.util.TimeUtils;

/**
 * Checks whether stored flight series are sufficient for the Cesium 3D viewer.
 * Uses the same rules as {@code CesiumDataJavalinRoutes}.
 */
public final class CesiumFlightReadiness {

    private static final Logger LOG = Logger.getLogger(CesiumFlightReadiness.class.getName());

    private static final DateTimeFormatter CESIUM_ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter CESIUM_ISO_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Outcome of a Cesium readiness check for one flight. */
    public record Result(int flightId, boolean ready, String errorMessage) {}

    private CesiumFlightReadiness() {}

    /**
     * Loads series from the database and evaluates Cesium readiness.
     *
     * @param connection database connection
     * @param flightId flight to inspect
     * @return readiness result with a descriptive {@code errorMessage} when not ready
     * @throws SQLException if series cannot be loaded
     */
    public static Result evaluate(Connection connection, int flightId) throws SQLException {
        DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LATITUDE);
        DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LONGITUDE);
        DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_AGL);
        StringTimeSeries date = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
        StringTimeSeries time = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
        StringTimeSeries utcDateTime =
                StringTimeSeries.getStringTimeSeries(connection, flightId, Parameters.UTC_DATE_TIME);

        String missing = describeMissingCesiumSeries(latitude, longitude, altAgl, date, time, utcDateTime);
        if (missing != null) {
            return new Result(flightId, false, missing);
        }

        if (!hasPlayableCesiumSample(latitude, longitude, altAgl, date, time, utcDateTime)) {
            return new Result(
                    flightId, false, describeNoPlayableSamples(latitude, longitude, altAgl, date, time, utcDateTime));
        }

        return new Result(flightId, true, null);
    }

    public static boolean hasRequiredCesiumSeries(
            DoubleTimeSeries latitude,
            DoubleTimeSeries longitude,
            DoubleTimeSeries altAgl,
            StringTimeSeries date,
            StringTimeSeries time,
            StringTimeSeries utcDateTime) {
        if (latitude == null || longitude == null || altAgl == null) {
            return false;
        }
        return (date != null && time != null) || utcDateTime != null;
    }

    /**
     * Checks that the series needed to render Cesium flight data are available.
     *
     * @param latitude latitude series
     * @param longitude longitude series
     * @param altAgl altitude-above-ground-level series
     * @param date local date series
     * @param time local time series
     * @param utcDateTime UTC date-time series
     * @return null if all required series are present
     */
    public static String describeMissingCesiumSeries(
            DoubleTimeSeries latitude,
            DoubleTimeSeries longitude,
            DoubleTimeSeries altAgl,
            StringTimeSeries date,
            StringTimeSeries time,
            StringTimeSeries utcDateTime) {
        List<String> missing = new ArrayList<>();
        if (latitude == null) {
            missing.add("Latitude");
        }
        if (longitude == null) {
            missing.add("Longitude");
        }
        if (altAgl == null) {
            missing.add("AltAGL");
        }
        boolean hasLocalTime = date != null && time != null;
        boolean hasUtcTime = utcDateTime != null;
        if (!hasLocalTime && !hasUtcTime) {
            if (date == null) {
                missing.add("Lcl Date");
            }
            if (time == null) {
                missing.add("Lcl Time");
            }
            if (utcDateTime == null) {
                missing.add(Parameters.UTC_DATE_TIME);
            }
        }
        if (missing.isEmpty()) {
            return null;
        }
        return "Missing required flight data for Cesium: " + String.join(", ", missing) + ".";
    }

    /**
     * Identifies why no Cesium sample can be rendered.
     *
     * @param latitude latitude series
     * @param longitude longitude series
     * @param altAgl altitude-above-ground-level series
     * @param date local date series
     * @param time local time series
     * @param utcDateTime UTC date-time series
     * @return null when at least one playable sample exists
     */
    public static String describeNoPlayableSamples(
            DoubleTimeSeries latitude,
            DoubleTimeSeries longitude,
            DoubleTimeSeries altAgl,
            StringTimeSeries date,
            StringTimeSeries time,
            StringTimeSeries utcDateTime) {
        if (hasPlayableCesiumSample(latitude, longitude, altAgl, date, time, utcDateTime)) {
            return null;
        }

        int sampleCount = cesiumSampleCount(latitude, altAgl);
        int dateSize = date != null ? date.size() : 0;
        int validPosition = 0;
        int validAlt = 0;
        int validTime = 0;

        for (int i = 0; i < sampleCount; i++) {
            if (hasValidCesiumPosition(latitude, longitude, i)) {
                validPosition++;
            }
            if (i < altAgl.size() && !Double.isNaN(altAgl.get(i))) {
                validAlt++;
            }
            if (formatCesiumRowTimestamp(i, date, time, utcDateTime, dateSize) != null) {
                validTime++;
            }
        }

        List<String> issues = new ArrayList<>();
        if (validPosition == 0) {
            issues.add("no valid Latitude/Longitude samples (non-zero, non-NaN)");
        }
        if (validAlt == 0) {
            issues.add("no valid AltAGL samples");
        }
        if (validTime == 0) {
            issues.add("no parseable timestamps (Lcl Date/Lcl Time or " + Parameters.UTC_DATE_TIME + ")");
        }
        if (issues.isEmpty()) {
            return "Required fields are present but the flight path could not be built for Cesium.";
        }
        return "Cannot build Cesium flight path: " + String.join("; ", issues) + ".";
    }

    public static boolean hasPlayableCesiumSample(
            DoubleTimeSeries latitude,
            DoubleTimeSeries longitude,
            DoubleTimeSeries altAgl,
            StringTimeSeries date,
            StringTimeSeries time,
            StringTimeSeries utcDateTime) {
        int sampleCount = cesiumSampleCount(latitude, altAgl);
        int dateSize = date != null ? date.size() : 0;
        for (int i = 0; i < sampleCount; i++) {
            String timestamp = formatCesiumRowTimestamp(i, date, time, utcDateTime, dateSize);
            if (timestamp != null && hasValidCesiumSample(latitude, longitude, altAgl, i)) {
                return true;
            }
        }
        return false;
    }

    public static int cesiumSampleCount(DoubleTimeSeries latitude, DoubleTimeSeries altAgl) {
        return Math.min(latitude.size(), altAgl.size());
    }

    public static boolean hasValidCesiumPosition(
            DoubleTimeSeries latitude, DoubleTimeSeries longitude, int index) {
        if (index >= latitude.size() || index >= longitude.size()) {
            return false;
        }
        double lat = latitude.get(index);
        double lon = longitude.get(index);
        return !Double.isNaN(lat) && !Double.isNaN(lon) && lat != 0.0 && lon != 0.0;
    }

    public static boolean hasValidCesiumSample(
            DoubleTimeSeries latitude,
            DoubleTimeSeries longitude,
            DoubleTimeSeries altAgl,
            int index) {
        if (!hasValidCesiumPosition(latitude, longitude, index) || index >= altAgl.size()) {
            return false;
        }
        return !Double.isNaN(altAgl.get(index));
    }

    public static String formatCesiumRowTimestamp(
            int index,
            StringTimeSeries date,
            StringTimeSeries time,
            StringTimeSeries utcDateTime,
            int dateSize) {
        String fromLocal = formatCesiumIsoTimestamp(
                date != null && index < dateSize ? date.get(index) : null,
                time != null && index < dateSize ? time.get(index) : null);
        if (fromLocal != null) {
            return fromLocal;
        }
        if (utcDateTime == null || index >= utcDateTime.size()) {
            return null;
        }
        String utcSample = utcDateTime.get(index);
        if (utcSample == null || utcSample.isBlank()) {
            return null;
        }
        try {
            LocalDateTime local = TimeUtils.parseUTC(utcSample.trim()).toLocalDateTime();
            return CESIUM_ISO_DATE.format(local) + "T" + CESIUM_ISO_TIME.format(local) + "Z";
        } catch (DateTimeParseException e) {
            LOG.fine("Skipping row with unparseable UTC Cesium timestamp: " + utcSample);
            return null;
        }
    }

    private static String formatCesiumIsoTimestamp(String date, String time) {
        if (date == null || time == null) {
            return null;
        }
        String trimmedDate = date.trim();
        String trimmedTime = TimeUtils.normalizeLocalTimeForParsing(time);
        if (trimmedDate.isEmpty() || trimmedTime.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = TimeUtils.findCorrectFormatter(trimmedDate, trimmedTime);
            LocalDateTime local = LocalDateTime.parse(trimmedDate + " " + trimmedTime, formatter);
            return CESIUM_ISO_DATE.format(local) + "T" + CESIUM_ISO_TIME.format(local) + "Z";
        } catch (TimeUtils.UnrecognizedDateTimeFormatException | DateTimeParseException e) {
            LOG.fine("Skipping row with unparseable Cesium timestamp: " + trimmedDate + " " + trimmedTime);
            return null;
        }
    }
}
