package org.ngafid.flights.process;

import java.time.*;
import java.util.Set;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.logging.Logger;
import java.time.format.DateTimeFormatter;

import static org.ngafid.flights.Parameters.*;
import org.ngafid.common.*;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.FatalFlightFileException;

public class ProcessStartEndTime extends ProcessStep {
    private static final Logger LOG = Logger.getLogger(ProcessStartEndTime.class.getName());

    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(LCL_DATE, LCL_TIME, UTC_OFFSET);

    public ProcessStartEndTime(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return Collections.<String>emptySet();
    }

    public Set<String> getRequiredStringColumns() {
        return REQUIRED_STRING_COLUMNS;
    }

    public Set<String> getRequiredColumns() {
        return REQUIRED_STRING_COLUMNS;
    }

    public Set<String> getOutputColumns() {
        return Collections.<String>emptySet();
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    public boolean entryIsEmpty(String date) {
        return date == null || date.trim() == "";
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        StringTimeSeries dates = builder.getStringTimeSeries(LCL_DATE);
        StringTimeSeries times = builder.getStringTimeSeries(LCL_TIME);
        StringTimeSeries offsets = builder.getStringTimeSeries(UTC_OFFSET);

        int dateSize = dates.size();
        int timeSize = times.size();
        int offsetSize = offsets.size();

        // get the minimum sized length of each of these series, they should all be the same but
        // if the last column was cut off it might not be the case
        int minSize = dateSize;
        if (minSize < timeSize)
            minSize = timeSize;
        if (minSize < offsetSize)
            minSize = offsetSize;

        // find the first non-null time entry
        int start = 0;
        while (start < minSize && (entryIsEmpty(dates.get(start))
                || entryIsEmpty(times.get(start))
                || entryIsEmpty(offsets.get(start)))) {
            start++;
        }

        if (start >= minSize)
            throw new MalformedFlightFileException(
                    "Date, Time or Offset columns were all null! Cannot set start/end times.");

        // find the last full date time offset entry row
        int end = minSize;
        do {
            end--;
        } while (end >= 0 && (entryIsEmpty(dates.get(end))
                || entryIsEmpty(times.get(end))
                || entryIsEmpty(offsets.get(end))));

        String startDate = dates.get(start).trim();
        String startTime = times.get(start).trim();
        String startOffset = offsets.get(start).trim();

        String endDate = dates.get(end).trim();
        String endTime = times.get(end).trim();
        String endOffset = offsets.get(end).trim();

        OffsetDateTime startODT = null;
        try {
            startODT = TimeUtils.convertToOffset(startDate, startTime, startOffset, "+00:00");
        } catch (DateTimeException dte) {
            LOG.severe("Corrupt start time data in flight file: " + dte.getMessage());
            throw new MalformedFlightFileException(
                    "Corrupt start time data in flight file: '" + dte.getMessage() + "'");
        }

        OffsetDateTime endODT = null;
        try {
            endODT = TimeUtils.convertToOffset(endDate, endTime, endOffset, "+00:00");
        } catch (DateTimeException dte) {
            LOG.severe("Corrupt end time data in flight file: " + dte.getMessage());
            throw new MalformedFlightFileException("Corrupt end time data in flight file: '" + dte.getMessage() + "'");
        }

        if (startODT.isAfter(endODT)) {
            builder.setStartDateTime(null);
            builder.setEndDateTime(null);
            throw new MalformedFlightFileException(
                    "Corrupt time data in flight file, start time was after the end time");
        }

        builder.setStartDateTime(startODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        builder.setEndDateTime(endODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
