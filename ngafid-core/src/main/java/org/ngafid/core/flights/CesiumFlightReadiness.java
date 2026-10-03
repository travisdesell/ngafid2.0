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

    /**
     * Reports whether the series required to build a Cesium replay are all present: latitude, longitude, and AltAGL,
     * plus a time source — either both local date and time, or a UTC date-time series.
     *
     * @param latitude the latitude series
     * @param longitude the longitude series
     * @param altAgl the altitude-above-ground series
     * @param date the local date series
     * @param time the local time series
     * @param utcDateTime the UTC date-time series
     * @return true if position, altitude, and a usable time source are all available
     */
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
     * Describes which flight data series required for a Cesium replay are missing.
     *
     * @param latitude the latitude series
     * @param longitude the longitude series
     * @param altAgl the altitude-above-ground series
     * @param date the local date series
     * @param time the local time series
     * @param utcDateTime the UTC date-time series
     * @return a human-readable description of the missing series, or null if all required series are present
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
     * Describes why a flight has no Cesium-playable samples, when that is the case.
     *
     * @param latitude the latitude series
     * @param longitude the longitude series
     * @param altAgl the altitude-above-ground series
     * @param date the local date series
     * @param time the local time series
     * @param utcDateTime the UTC date-time series
     * @return a human-readable description of why no playable sample exists, or null when at least one exists
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

    /**
     * Reports whether at least one sample index yields a complete, playable Cesium frame: a parseable timestamp (from
     * local date/time or the UTC series) together with a valid position and altitude at that index.
     *
     * @param latitude the latitude series
     * @param longitude the longitude series
     * @param altAgl the altitude-above-ground series
     * @param date the local date series
     * @param time the local time series
     * @param utcDateTime the UTC date-time series
     * @return true if any index has a usable timestamp, position, and altitude together
     */
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

    /**
     * Returns the number of sample indices safe to iterate over for Cesium: the shorter of the latitude and AltAGL
     * series lengths, so neither is indexed out of bounds.
     *
     * @param latitude the latitude series
     * @param altAgl the altitude-above-ground series
     * @return the smaller of the two series' sizes
     */
    public static int cesiumSampleCount(DoubleTimeSeries latitude, DoubleTimeSeries altAgl) {
        return Math.min(latitude.size(), altAgl.size());
    }

    /**
     * Reports whether the latitude/longitude pair at an index is a valid Cesium position: both in range, neither NaN,
     * and neither exactly zero (a zero coordinate is treated as missing GPS rather than a real point).
     *
     * @param latitude the latitude series
     * @param longitude the longitude series
     * @param index the sample index to test
     * @return true if the index holds a usable, non-zero, non-NaN coordinate pair
     */
    public static boolean hasValidCesiumPosition(DoubleTimeSeries latitude, DoubleTimeSeries longitude, int index) {
        if (index >= latitude.size() || index >= longitude.size()) {
            return false;
        }
        double lat = latitude.get(index);
        double lon = longitude.get(index);
        return !Double.isNaN(lat) && !Double.isNaN(lon) && lat != 0.0 && lon != 0.0;
    }

    /**
     * Reports whether the index holds a fully valid Cesium sample: a valid position (see
     * {@link #hasValidCesiumPosition}) and a non-NaN AltAGL value in range.
     *
     * @param latitude the latitude series
     * @param longitude the longitude series
     * @param altAgl the altitude-above-ground series
     * @param index the sample index to test
     * @return true if position and altitude are both valid at the index
     */
    public static boolean hasValidCesiumSample(
            DoubleTimeSeries latitude, DoubleTimeSeries longitude, DoubleTimeSeries altAgl, int index) {
        if (!hasValidCesiumPosition(latitude, longitude, index) || index >= altAgl.size()) {
            return false;
        }
        return !Double.isNaN(altAgl.get(index));
    }

    /**
     * Builds an ISO-8601 UTC timestamp string ({@code yyyy-MM-ddTHH:mm:ssZ}) for a sample index. Prefers the local
     * date/time series (parsed with a detected formatter); if that is unavailable or unparseable, falls back to the UTC
     * date-time series. Returns null when no source yields a parseable timestamp (unparseable rows are logged at fine
     * level and skipped).
     *
     * @param index the sample index to format
     * @param date the local date series (may be null)
     * @param time the local time series (may be null)
     * @param utcDateTime the UTC date-time series used as a fallback (may be null)
     * @param dateSize the usable length of the local date/time series (bounds the local lookup)
     * @return the ISO-8601 UTC timestamp, or null if no parseable timestamp exists at the index
     */
    public static String formatCesiumRowTimestamp(
            int index, StringTimeSeries date, StringTimeSeries time, StringTimeSeries utcDateTime, int dateSize) {
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
