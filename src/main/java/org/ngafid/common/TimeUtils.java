package org.ngafid.common;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.ngafid.flights.FatalFlightFileException;


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
        //weird input data
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
        LocalDateTime ldt = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        //fix bad offset values
        offset = updateBadOffset(ldt, offset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(offset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        //System.out.println(zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // create a ZonedDateTime from the OffsetDateTime and use UTC as time zone
        ZonedDateTime utcZdt = odt.atZoneSameInstant(ZoneOffset.UTC);

        // print the date time in UTC using the ISO ZONED DATE TIME format
        //System.out.print(" -- UTC (zoned):\t" + utcZdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

        // and then print it again using your desired format
        //System.out.println(" -- UTC:\t" + utcZdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " -- epoch second: " + utcZdt.toEpochSecond());

        return utcZdt.toEpochSecond();
    }

    public static String toUTC(String date, String time, String offset) {
        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        //fix bad offset values
        offset = updateBadOffset(ldt, offset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(offset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        //System.out.println(zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // create a ZonedDateTime from the OffsetDateTime and use UTC as time zone
        ZonedDateTime utcZdt = odt.atZoneSameInstant(ZoneOffset.UTC);

        return utcZdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }

    /**
     * List of Date -time formats supported.
     */
    private static final List<DateTimeFormatter> dateTimeFormatters = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),  // Expected format
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss")    // Single-digit month/day

    );

    /**
     * Helper method to parse the date and time using the declared formatters
     * @param dateTimeString
     * @return
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        String normalizedDateTime = dateTimeString.replaceAll("\\s+", " ");  // Replaces multiple spaces with a single space
        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(normalizedDateTime, formatter);
            } catch (Exception e) {
                // Continue trying other formats
            }
        }
        // If none of the formats work, throw an exception or handle it appropriately
        throw new IllegalArgumentException("Date format not supported: " + normalizedDateTime);
    }

    public static OffsetDateTime convertToOffset(String originalDate, String originalTime, String originalOffset, String newOffset) {
        //System.out.println("original:   \t" + originalTime + " " + originalOffset + " new offset: "+ newOffset);

        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = parseDateTime(originalDate + " " + originalTime);

        //fix bad offset values
        originalOffset = updateBadOffset(ldt, originalOffset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(originalOffset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        //System.out.println("with offset:\t" + zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        //System.out.println("with offset:               \t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        ZoneOffset offset2 = ZoneOffset.of(newOffset);
        OffsetDateTime odt3 = odt.withOffsetSameInstant(offset2);

        //String newTime = odt3.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        //String newTime = odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //System.out.println("with offset (same instant):\t" + newTime);

        return odt3;
    }

    public static String convertToOffset(String originalDateTime, String originalOffset, String newOffset) {
        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(originalDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        //fix bad offset values
        //originalOffset = updateBadOffset(ldt, originalOffset);

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(originalOffset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        //System.out.println("with offset:\t" + zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        //System.out.println("with offset:               \t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        ZoneOffset offset2 = ZoneOffset.of(newOffset);
        OffsetDateTime odt3 = odt.withOffsetSameInstant(offset2);

        //String newTime = odt3.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        //String newTime = odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //System.out.println("with offset (same instant):\t" + newTime);

        return odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    public static double calculateDurationInSeconds(String startDateTime, String endDateTime) throws FatalFlightFileException {
        LocalDateTime start= null;
        LocalDateTime end = null;
        try {
            start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        } catch (DateTimeParseException e) {
            try {
                start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss'Z'"));
                end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss'Z'"));
            } catch (DateTimeParseException er2) {
                try {
                    start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("MM/dd/yyyy'T'HH:mm:ss'Z'"));
                    end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("MM/dd/yyyy'T'HH:mm:ss'Z'"));
                } catch (DateTimeParseException e3) {
                    try {
                        start = LocalDateTime.parse(startDateTime, DateTimeFormatter.ofPattern("MM/dd/yyyy'T'HH:mm:ss"));
                        end = LocalDateTime.parse(endDateTime, DateTimeFormatter.ofPattern("MM/dd/yyyy'T'HH:mm:ss"));
                    } catch (DateTimeParseException e4) {
                        throw new FatalFlightFileException("Flight had incorrectly formatted date/time values (should be yyyy-MM-dd HH:mm:ss Z or yyyy/MM/dd HH:mm:ss or MM/dd/yyyy HH:mm:ss).");
                    }
                    throw new FatalFlightFileException("Flight had incorrectly formatted date/time values (should be yyyy-MM-dd HH:mm:ss Z or yyyy/MM/dd HH:mm:ss or MM/dd/yyyy HH:mm:ss).");
                }
            }
        }

        return ChronoUnit.SECONDS.between(start, end);
    }

    public static LocalDateTime parseLocalDateTime(String dateTimeString, String pattern) throws FatalFlightFileException {
        List<String> formatStrings = Arrays.asList(pattern, "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy/MM/dd'T'HH:mm:ss'Z'", "MM/dd/yyyy'T'HH:mm:ss'Z'", "MM/dd/yyyy'T'HH:mm:ss");

        LocalDateTime dateTime = null;
        String patterns = "";
        boolean first = true;
        for (String format : formatStrings) {
            try {
                dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(format));
                break;
            }
            catch (DateTimeParseException e) {}
            if (!first) {
                patterns +=  ", '" + format + "'";
            } else {
                patterns +=  "'" + format + "'";
            }
            first = false;
        }

        if (dateTime == null) {
            throw new FatalFlightFileException("Flight had incorrectly formatted date/time values (should be one of " + patterns + ")");
        }

        return dateTime;
    }

    public static double calculateDurationInSeconds(String startDateTime, String endDateTime, String pattern) throws FatalFlightFileException {
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
}
