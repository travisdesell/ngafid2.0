package org.ngafid.flights.process;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DateParser {

    // List of date formats to try
    private static final List<SimpleDateFormat> formatters = Arrays.asList(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),  // Expected format
            new SimpleDateFormat("M/d/yyyy HH:mm:ss")     // Secondary format
    );

    /**
     * Parses a date-time string into a Date object.
     *
     * @param dateTimeString the date-time string to parse
     * @return the parsed Date object
     * @throws FlightProcessingException if the date-time string cannot be parsed by any format
     */
    public static Date parseDateTime(String dateTimeString) throws FlightProcessingException {
        // Create a list to collect any exceptions
        List<Exception> exceptions = new ArrayList<>();

        for (SimpleDateFormat formatter : formatters) {
            try {
                // Try parsing the date-time string with the current formatter
                return formatter.parse(dateTimeString);
            } catch (ParseException e) {
                exceptions.add(e);
            }
        }
        throw new FlightProcessingException(exceptions);
    }
}
