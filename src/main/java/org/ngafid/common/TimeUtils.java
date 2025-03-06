package org.ngafid.common;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import us.dustinj.timezonemap.TimeZoneMap;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public enum TimeUtils {
    ;
    private static final Logger LOG = Logger.getLogger(TimeUtils.class.getName());
    private static TimeZoneMap TIME_ZONE_MAP = null;

    public static DateTimeFormatter ISO_8601_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    public static DateTimeFormatter MYSQL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static TimeZoneMap getTimeZoneMap() {
        if (TIME_ZONE_MAP == null)
            TIME_ZONE_MAP = TimeZoneMap.forRegion(18.91, -179.15, 71.538800, -66.93457);

        return TIME_ZONE_MAP;
    }

    public static class OffsetDateTimeJSONAdapter extends TypeAdapter<OffsetDateTime> {

        @Override
        public void write(JsonWriter jsonWriter, OffsetDateTime offsetDateTime) throws IOException {
            jsonWriter.value(TimeUtils.UTCtoSQL(offsetDateTime));
        }

        @Override
        public OffsetDateTime read(JsonReader jsonReader) throws IOException {
            return null;
        }
    }

    /**
     * Fixes bad offsets that Java cant handle by default (outside -18 and +18). Will do nothing
     * if the offset is okay.
     *
     * @param offset the bad offset
     * @return the fixed offset
     */
    public static String updateBadOffset(String offset) {
        // weird input data
        switch (offset) {
            case "+19:00", "+23:00", "+22:00", "+21:00", "+20:00" -> {
                offset = "+18:00";
            }
            case "-19:00", "-22:00", "-23:00", "-21:00", "-20:00" -> {
                offset = "-18:00";
            }
            default -> {
            }
        }
        return offset;
    }

    private static DateTimeFormatter STANDARD_FORMAT = DateTimeFormatter.ofPattern("yyyy-M-d H:m:s");

    public static String toString(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(STANDARD_FORMAT);
    }

    public static long toEpochSecond(String date, String time, String offset) {
        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(date.trim() + " " + time.trim(),
                DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"));

        // fix bad offset values
        offset = updateBadOffset(offset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(offset.trim());

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        // System.out.println(zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // create a ZonedDateTime from the OffsetDateTime and use UTC as time zone
        ZonedDateTime utcZdt = odt.atZoneSameInstant(ZoneOffset.UTC);

        // print the date time in UTC using the ISO ZONED DATE TIME format
        // System.out.print(" -- UTC (zoned):\t" + utcZdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

        // and then print it again using your desired format
        // System.out.println(" -- UTC:\t" + utcZdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " --
        // epoch second: " + utcZdt.toEpochSecond());

        return utcZdt.toEpochSecond();
    }

    public static String toUTC(String date, String time, String offset) {
        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"));

        // fix bad offset values
        offset = updateBadOffset(offset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(offset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        // System.out.println(zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // create a ZonedDateTime from the OffsetDateTime and use UTC as time zone
        ZonedDateTime utcZdt = odt.atZoneSameInstant(ZoneOffset.UTC);

        return utcZdt.format(DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"));

    }

    public static OffsetDateTime convertToOffset(String originalDate, String originalTime, String originalOffset,
                                                 String newOffset) throws UnrecognizedDateTimeFormatException {
        return convertToOffset(originalDate + " " + originalTime, originalOffset, newOffset);
    }

    public static OffsetDateTime convertToOffset(String originalDateTime, String originalOffset, String newOffset) throws UnrecognizedDateTimeFormatException {
        DateTimeFormatter dateTimeFormat = findCorrectFormatter(originalDateTime);
        return convertToOffset(dateTimeFormat, originalDateTime, originalOffset, newOffset);
    }

    public static OffsetDateTime convertToOffset(DateTimeFormatter formatter, String originalDateTime, String originalOffset, String newOffset) {
        LOG.info("Date is " + originalDateTime);
        LocalDateTime ldt = LocalDateTime.parse(originalDateTime, formatter);

        // fix bad offset values
        originalOffset = updateBadOffset(originalOffset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(originalOffset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        ZoneOffset offset2 = ZoneOffset.of(newOffset);

        return odt.withOffsetSameInstant(offset2);
    }

    // Date formats WITHOUT TIMEZONE
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:m:s"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:m:s"),
            DateTimeFormatter.ofPattern("M-d-yyyy H:m:s"));

    public static LocalDateTime parseLocalDateTime(String dateTimeString)
            throws UnrecognizedDateTimeFormatException {
        var formatter = findCorrectFormatter(dateTimeString);
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    public static double calculateDurationInSeconds(String startDateTime, String endDateTime)
            throws UnrecognizedDateTimeFormatException {
        DateTimeFormatter formatter = findCorrectFormatter(startDateTime);
        LocalDateTime start = LocalDateTime.parse(startDateTime, formatter);
        LocalDateTime end = LocalDateTime.parse(endDateTime, formatter);

        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * Add seconds to a Date object
     *
     * @param date
     * @param seconds
     * @return date with added seconds
     */
    public static Date addSeconds(Date date, Integer seconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    public static Date subtractSeconds(Date date, Integer seconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, -seconds);
        return cal.getTime();
    }

    public static Date addMilliseconds(Date date, Integer milliseconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MILLISECOND, milliseconds);
        return cal.getTime();
    }

    /**
     * Calculates local date, local time, and UTC offset for each entry based on given UTC dates, times, latitudes, and longitudes.
     *
     * @param utcDates   time series of UTC dates as strings (e.g., "yyyy-MM-dd")
     * @param utcTimes   time series of UTC times as strings (e.g., "HH:mm:ss")
     * @param latitudes  time series of latitude strings
     * @param longitudes time series of longitude strings
     * @return a LocalDateTimeResult containing lists of local dates, times, and UTC offsets.
     */
    public static LocalDateTimeResult calculateLocalDateTimeFromTimeSeries(
            StringTimeSeries utcDates,
            StringTimeSeries utcTimes,
            DoubleTimeSeries latitudes,
            DoubleTimeSeries longitudes) throws UnrecognizedDateTimeFormatException {
        TimeZoneMap map = getTimeZoneMap();

        // Prepare lists for the results.
        ArrayList<String> localDates = new ArrayList<>(utcDates.size());
        ArrayList<String> localTimes = new ArrayList<>(utcDates.size());
        ArrayList<String> utcOffsets = new ArrayList<>(utcDates.size());

        String dateTimeString = utcDates.get(0) + " " + utcTimes.get(0);
        DateTimeFormatter formatter = findCorrectFormatter(dateTimeString);

        // Iterate over each index and calculate the corresponding local date, time, and offset.
        for (int i = 0; i < utcDates.size(); i++) {
            String dateTime = utcDates.get(i) + " " + utcTimes.get(i);
            LocalDateTime utcDateTime = LocalDateTime.parse(dateTime, formatter);

            double latitude = latitudes.get(i);
            double longitude = longitudes.get(i);
            String date = "", time = "", offset = "";

            // If our latitude and / or longitude are NaN, we will get an illegal argument exception.
            try {
                String zoneIdStr = map.getOverlappingTimeZone(latitude, longitude).getZoneId();
                ZoneId zoneId = ZoneId.of(zoneIdStr);
                ZonedDateTime localZonedDateTime = utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId);

                date = localZonedDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-M-d"));
                time = localZonedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("H:m:s"));
                offset = localZonedDateTime.getOffset().getId();
            } catch (IllegalArgumentException e) {
                // lat long values were invalid (nan or out of bounds).
            }

            // Add a default value.
            localDates.add(date);
            localTimes.add(time);
            utcOffsets.add(offset);
        }

        return new LocalDateTimeResult(localDates, localTimes, utcOffsets);
    }

    public static DateTimeFormatter findCorrectFormatter(String date, String time) throws UnrecognizedDateTimeFormatException {
        return findCorrectFormatter(date + " " + time);
    }

    public static String UTCtoSQL(String timeUTC) {
        return LocalDateTime.parse(timeUTC, ISO_8601_FORMAT).format(MYSQL_FORMAT);
    }

    public static String UTCtoSQL(OffsetDateTime odt) {
        return odt.atZoneSameInstant(ZoneOffset.UTC).format(MYSQL_FORMAT);
    }

    public static OffsetDateTime parseUTC(String dateTimeString) {
        return OffsetDateTime.parse(dateTimeString, ISO_8601_FORMAT);
    }

    public static OffsetDateTime SQLtoOffsetDateTime(String sqlDateTime) {
        return LocalDateTime.parse(sqlDateTime, MYSQL_FORMAT).atOffset(ZoneOffset.UTC);
    }

    public static class UnrecognizedDateTimeFormatException extends Exception {
    }

    /**
     * Finds the correct DateTimeFormatter by trying to parse a date/time string.
     *
     * @param dateTimeString
     * @return specific DateTimeFormatter
     */
    public static DateTimeFormatter findCorrectFormatter(String dateTimeString) throws UnrecognizedDateTimeFormatException {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDateTime.parse(dateTimeString, formatter);
                return formatter;
            } catch (DateTimeParseException e) {
                // Continue trying other formatters
            }
        }
        throw new DateTimeException("Could not deduce date-time formatter for the following: " + dateTimeString);
    }

    /**
     * Store the results of the date-time calculation.
     */
    public static class LocalDateTimeResult {
        private final ArrayList<String> localDates;
        private final ArrayList<String> localTimes;
        private final ArrayList<String> utcOffsets;

        public LocalDateTimeResult(ArrayList<String> localDates, ArrayList<String> localTimes, ArrayList<String> utcOffsets) {
            this.localDates = localDates;
            this.localTimes = localTimes;
            this.utcOffsets = utcOffsets;
        }

        public ArrayList<String> getLocalDates() {
            return localDates;
        }

        public ArrayList<String> getLocalTimes() {
            return localTimes;
        }

        public ArrayList<String> getUtcOffsets() {
            return utcOffsets;
        }
    }

    public static int getCurrentYearUTC() {
        return LocalDateTime.now().atOffset(ZoneOffset.UTC).getYear();
    }

    public static int getCurrentMonthUTC() {
        return LocalDateTime.now().atOffset(ZoneOffset.UTC).getMonthValue();
    }
}