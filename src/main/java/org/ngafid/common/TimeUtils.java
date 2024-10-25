package org.ngafid.common;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import us.dustinj.timezonemap.TimeZoneMap;
import java.time.ZoneId;
import java.util.Optional;
import org.ngafid.flights.FatalFlightFileException;

import static org.ngafid.FindLowEndingFuelEvents.LOG;

public class TimeUtils {

    /**
     * Fixes bad offsets that Java cant handle by default (outside -18 and +18). Will do nothing
     * if the offset is okay.
     *
     * @param the LocalDateTime value which will be updated
     * @param the bad offset
     * @return the fixed offset
     */
    public static String updateBadOffset(LocalDateTime ldt, String offset) {
        // weird input data
        if (offset.equals("+19:00")) {
            ldt = ldt.plusHours(1);
            offset = "+18:00";
        } else if (offset.equals("+20:00")) {
            ldt = ldt.plusHours(2);
            offset = "+18:00";
        } else if (offset.equals("+21:00")) {
            ldt = ldt.plusHours(3);
            offset = "+18:00";
        } else if (offset.equals("+22:00")) {
            ldt = ldt.plusHours(4);
            offset = "+18:00";
        } else if (offset.equals("+23:00")) {
            ldt = ldt.plusHours(5);
            offset = "+18:00";
        } else if (offset.equals("-19:00")) {
            ldt = ldt.minusHours(1);
            offset = "-18:00";
        } else if (offset.equals("-20:00")) {
            ldt = ldt.minusHours(2);
            offset = "-18:00";
        } else if (offset.equals("-21:00")) {
            ldt = ldt.minusHours(3);
            offset = "-18:00";
        } else if (offset.equals("-22:00")) {
            ldt = ldt.minusHours(4);
            offset = "-18:00";
        } else if (offset.equals("-23:00")) {
            ldt = ldt.minusHours(5);
            offset = "-18:00";
        }

        return offset;
    }

    public static long toEpochSecond(String date, String time, String offset) {
        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(date.trim() + " " + time.trim(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // fix bad offset values
        offset = updateBadOffset(ldt, offset);

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
        LocalDateTime ldt = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // fix bad offset values
        offset = updateBadOffset(ldt, offset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(offset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        // System.out.println(zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // create a ZonedDateTime from the OffsetDateTime and use UTC as time zone
        ZonedDateTime utcZdt = odt.atZoneSameInstant(ZoneOffset.UTC);

        return utcZdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }

    /**
     * Helper method to parse the date and time using the declared formatters
     * @param dateTimeString
     * @return
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        String normalizedDateTime = dateTimeString.replaceAll("\\s+", " ");  // Replaces multiple spaces with a single space
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalizedDateTime, formatter);
            } catch (Exception e) {
                // Continue trying other formats
            }
        }
        // If none of the formats work, throw an exception or handle it appropriately
        throw new IllegalArgumentException("Date format not supported: " + normalizedDateTime);
    }

    public static OffsetDateTime convertToOffset(String originalDate, String originalTime, String originalOffset,
            String newOffset) {
        // System.out.println("original: \t" + originalTime + " " + originalOffset + " new offset: "+ newOffset);

        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = parseDateTime(originalDate + " " + originalTime);

        // fix bad offset values
        originalOffset = updateBadOffset(ldt, originalOffset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(originalOffset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        // System.out.println("with offset:\t" + zoneOffset.toString() + ":\t" +
        // odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // System.out.println("with offset: \t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        ZoneOffset offset2 = ZoneOffset.of(newOffset);
        OffsetDateTime odt3 = odt.withOffsetSameInstant(offset2);

        // String newTime = odt3.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // String newTime = odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // System.out.println("with offset (same instant):\t" + newTime);

        return odt3;
    }

    public static String convertToOffset(String originalDateTime, String originalOffset, String newOffset) {
        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(originalDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // fix bad offset values
        // originalOffset = updateBadOffset(ldt, originalOffset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(originalOffset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        // System.out.println("with offset:\t" + zoneOffset.toString() + ":\t" +
        // odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // System.out.println("with offset: \t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        ZoneOffset offset2 = ZoneOffset.of(newOffset);
        OffsetDateTime odt3 = odt.withOffsetSameInstant(offset2);

        // String newTime = odt3.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // String newTime = odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // System.out.println("with offset (same instant):\t" + newTime);

        return odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss'Z'"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy'T'HH:mm:ss'Z'"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"));

    public static double calculateDurationInSeconds(String startDateTime, String endDateTime)
            throws FatalFlightFileException {

        for (var dateFormatter : DATE_FORMATTERS) {
            try {
                var start = LocalDateTime.parse(startDateTime, dateFormatter);
                var end = LocalDateTime.parse(endDateTime, dateFormatter);
                return ChronoUnit.SECONDS.between(start, end);
            } catch (DateTimeParseException e) {
                continue;
            }
        }

        throw new FatalFlightFileException("Flight file is using unsupported date time format.");
    }

    public static LocalDateTime parseLocalDateTime(String dateTimeString, String pattern)
            throws FatalFlightFileException {
        LocalDateTime dateTime = null;
        String patterns = "";
        boolean first = true;
        for (var formatter : DATE_FORMATTERS) {
            try {
                dateTime = LocalDateTime.parse(dateTimeString, formatter);
                break;
            } catch (DateTimeParseException e) {
            }

            if (!first) {
                patterns += ", '" + formatter.toString() + "'";
            } else {
                patterns += "'" + formatter.toString() + "'";
            }
            first = false;
        }

        if (dateTime == null) {
            throw new FatalFlightFileException(
                    "Flight had incorrectly formatted date/time values (should be one of " + patterns + ")");
        }

        return dateTime;
    }

    public static double calculateDurationInSeconds(String startDateTime, String endDateTime, String pattern)
            throws FatalFlightFileException {
        LocalDateTime start = parseLocalDateTime(startDateTime, pattern);
        LocalDateTime end = parseLocalDateTime(endDateTime, pattern);

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
     * @param map          map object (created once per upload for efficiency.
     * @param utcDates     time series of UTC dates as strings (e.g., "yyyy-MM-dd")
     * @param utcTimes     time series of UTC times as strings (e.g., "HH:mm:ss")
     * @param latitudes    time series of latitude strings
     * @param longitudes   time series of longitude strings
     * @return a LocalDateTimeResult containing lists of local dates, times, and UTC offsets.
     */
    public static LocalDateTimeResult calculateLocalDateTimeFromTimeSeries(
            TimeZoneMap map,
            StringTimeSeries utcDates,
            StringTimeSeries utcTimes,
            DoubleTimeSeries latitudes,
            DoubleTimeSeries longitudes) throws Exception {

        // Validate that all time series are of the same size.
        if (utcDates.size() != utcTimes.size() || utcDates.size() != latitudes.size() || utcDates.size() != longitudes.size()) {
            throw new IllegalArgumentException("All input time series must have the same size.");
        }

        // Prepare lists for the results.
        ArrayList<String> localDates = new ArrayList<>(utcDates.size());
        ArrayList<String> localTimes = new ArrayList<>(utcDates.size());
        ArrayList<String> utcOffsets = new ArrayList<>(utcDates.size());

        // Iterate over each index and calculate the corresponding local date, time, and offset.
        for (int i = 0; i < utcDates.size(); i++) {
            String dateTimeString = utcDates.get(i) + " " + utcTimes.get(i);
            LocalDateTime utcDateTime = null;

            // Try parsing using a formatter.
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    utcDateTime = LocalDateTime.parse(dateTimeString, formatter);
                    break;
                } catch (DateTimeParseException e) {
                    // Continue trying other formats
                }
            }

            if (utcDateTime == null) {
                throw new IllegalArgumentException("Failed to parse date/time string: " + dateTimeString);
            }

            double latitude = latitudes.get(i);
            double longitude = longitudes.get(i);

            String zoneIdStr = map.getOverlappingTimeZone(latitude, longitude).getZoneId();
            ZoneId zoneId = ZoneId.of(zoneIdStr);

            // Convert the UTC datetime to the local time in the specified zone.
            ZonedDateTime localZonedDateTime = utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId);

            // Format the local date, local time, and UTC offset.
            localDates.add(localZonedDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            localTimes.add(localZonedDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            utcOffsets.add(localZonedDateTime.getOffset().getId());
        }

        return new LocalDateTimeResult(localDates, localTimes, utcOffsets);
    }

    /**
     * Helper method to find the correct DateTimeFormatter by trying to parse the first date string.
     */
    public static DateTimeFormatter findCorrectFormatter(String dateTimeString) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDateTime.parse(dateTimeString, formatter);
                return formatter;  // Return the formatter if parsing succeeds
            } catch (DateTimeParseException e) {
                // Continue trying other formatters
            }
        }
        return null;
    }


    /**
     * Helper class to store the results of the date-time calculation.
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
}
