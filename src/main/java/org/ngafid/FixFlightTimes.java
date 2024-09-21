package org.ngafid;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Conditional;
import org.ngafid.filters.Filter;
import org.ngafid.filters.Pair;

import org.ngafid.airports.Airports;

public class FixFlightTimes {

    public static OffsetDateTime convertToOffset(String originalTime, String originalOffset, String newOffset) {
        // System.out.println("original: \t" + originalTime + " " + originalOffset + "
        // new offset: "+ newOffset);

        // create a LocalDateTime using the date time passed as parameter
        LocalDateTime ldt = LocalDateTime.parse(originalTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // parse the offset
        ZoneOffset zoneOffset = ZoneOffset.of(originalOffset);

        // create an OffsetDateTime using the parsed offset
        OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

        // print the date time with the parsed offset
        // System.out.println("with offset:\t" + zoneOffset.toString() + ":\t" +
        // odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // System.out.println("with offset: \t" +
        // odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        ZoneOffset offset2 = ZoneOffset.of(newOffset);
        OffsetDateTime odt3 = odt.withOffsetSameInstant(offset2);

        // String newTime = odt3.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // String newTime = odt3.format(DateTimeFormatter.ofPattern("yyyy-MM-dd
        // HH:mm:ss"));
        // System.out.println("with offset (same instant):\t" + newTime);

        return odt3;
    }

    public static void main(String[] arguments) {
        try (Connection connection = Database.getConnection()) {
            ArrayList<Flight> flights = Flight.getFlights(connection,
                    "start_time is NULL OR end_time is NULL OR end_time < start_time");

            System.out.println(
                    "found " + flights.size() + " flights with start/end times == NULL or end_time < start_time");

            int count = 0;
            for (Flight flight : flights) {
                int flightId = flight.getId();
                System.out.println("fixing flight id: " + flightId);
                String startDateTime = flight.getStartDateTime();
                String endDateTime = flight.getEndDateTime();

                System.out.println("\tinitial start and end date times: " + startDateTime + " " + endDateTime);

                StringTimeSeries dates = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
                StringTimeSeries times = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
                StringTimeSeries offsets = StringTimeSeries.getStringTimeSeries(connection, flightId, "UTCOfst");

                if (dates == null) {
                    System.out.println("\tdate series was null!");
                    System.out.println();
                    continue;
                }

                if (times == null) {
                    System.out.println("\ttime series was null!");
                    System.out.println();
                    continue;
                }

                if (offsets == null) {
                    System.out.println("\toffset series was null!");
                    System.out.println();
                    continue;
                }

                int dateSize = dates.size();
                int timeSize = times.size();
                int offsetSize = offsets.size();

                System.out.println(
                        "\tdate size: " + dateSize + ", time size: " + timeSize + ", offset size: " + offsetSize);

                // get the minimum sized length of each of these series, they should all be the
                // same but
                // if the last column was cut off it might not be the case
                int minSize = dateSize;
                if (minSize < timeSize)
                    minSize = timeSize;
                if (minSize < offsetSize)
                    minSize = offsetSize;

                // find the first non-null time entry
                int start = 0;
                while (start < minSize &&
                        (dates.get(start) == null || dates.get(start).equals("") ||
                                times.get(start) == null || times.get(start).equals("") ||
                                offsets.get(start) == null || offsets.get(start).equals("")
                                || offsets.get(start).equals("+19:00"))) {

                    start++;
                }
                System.out.println("\tfirst date time and offset not null at index: " + start);

                if (start >= minSize) {
                    System.out.println("series were all NULL, cannot calculate start/end times");
                    System.out.println();
                    continue;
                }

                // find the last full date time offset entry row
                int end = minSize - 1;
                while (end >= 0 &&
                        (dates.get(end) == null || dates.get(end).equals("") ||
                                times.get(end) == null || times.get(end).equals("") ||
                                offsets.get(end) == null || offsets.get(end).equals(""))) {

                    end--;
                }

                String startDate = dates.get(start);
                String startTime = times.get(start);
                String startOffset = offsets.get(start);

                String endDate = dates.get(end);
                String endTime = times.get(end);
                String endOffset = offsets.get(end);

                System.out.println(
                        "\t\t\tfirst not null  " + start + " -- " + startDate + " " + startTime + " " + startOffset);
                System.out.println("\t\t\tlast not null   " + endDate + " " + endTime + " " + endOffset);

                OffsetDateTime startODT = convertToOffset(startDate + " " + startTime, startOffset, endOffset);
                OffsetDateTime endODT = convertToOffset(endDate + " " + endTime, endOffset, endOffset);

                String convertedStart;
                String convertedEnd;
                if (startODT.isAfter(endODT)) {
                    // start time is after the end time -- corrupt time sequence
                    convertedStart = null;
                    convertedEnd = null;
                    endOffset = null;
                    System.out.println("\t\t\tstart time was AFTER the end time -- this is a corrupt flight file!");
                } else {
                    convertedStart = startODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    convertedEnd = endODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }

                System.out.println("\t\t\tconverted start " + convertedStart);
                System.out.println("\t\t\tconverted end   " + convertedEnd);

                PreparedStatement ps = connection.prepareStatement(
                        "UPDATE flights SET start_time = ?, end_time = ?, time_offset = ? WHERE id = ?");
                ps.setString(1, convertedStart);
                ps.setString(2, convertedEnd);
                ps.setString(3, endOffset);
                ps.setInt(4, flightId);
                System.out.println(ps);
                ps.executeUpdate();
                ps.close();

                System.out.println();
                count++;
            }

            System.out.println("processed " + count + " flights");

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("finished!");
        System.exit(1);
    }
}
