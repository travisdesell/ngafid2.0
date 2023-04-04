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

    public static Set<String> REQUIRED_STRING_COLUMNS = Set.of(LCL_DATE, LCL_TIME, UTC_OFFSET);

    public ProcessStartEndTime(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() { return Collections.<String>emptySet(); }
    public Set<String> getRequiredStringColumns() { return REQUIRED_STRING_COLUMNS; }
    public Set<String> getRequiredColumns() { return REQUIRED_STRING_COLUMNS; }
    public Set<String> getOutputColumns() { return Collections.<String>emptySet(); }

    public boolean airframeIsValid(String airframe) { return true; }
    public boolean isRequired() { return true; }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        StringTimeSeries dates = builder.stringTimeSeries.get(LCL_DATE);
        StringTimeSeries times = builder.stringTimeSeries.get(LCL_TIME);
        StringTimeSeries offsets = builder.stringTimeSeries.get(UTC_OFFSET);

        int dateSize = dates.size();
        int timeSize = times.size();
        int offsetSize = offsets.size();

        LOG.info("\tdate size: " + dateSize + ", time size: " + timeSize + ", offset size: " + offsetSize);

        //get the minimum sized length of each of these series, they should all be the same but 
        //if the last column was cut off it might not be the case
        int minSize = dateSize;
        if (minSize < timeSize) minSize = timeSize;
        if (minSize < offsetSize) minSize = offsetSize;

        //find the first non-null time entry
        int start = 0;
        while (start < minSize &&
                (dates.get(start) == null || dates.get(start).equals("") ||
                        times.get(start) == null || times.get(start).equals("") ||
                        offsets.get(start) == null || offsets.get(start).equals("") || offsets.get(start).equals("+19:00"))) {

            start++;
        }

        LOG.info("\tfirst date time and offset not null at index: " + start);

        if (start >= minSize)
            throw new MalformedFlightFileException("Date, Time or Offset columns were all null! Cannot set start/end times.");

        //find the last full date time offset entry row
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

        LOG.info("\t\t\tfirst not null  " + start + " -- " + startDate + " " + startTime + " " + startOffset);
        LOG.info("\t\t\tlast not null   " + endDate + " " + endTime + " " + endOffset);

        OffsetDateTime startODT = null;
        try {
            startODT = TimeUtils.convertToOffset(startDate, startTime, startOffset, "+00:00");
        } catch (DateTimeException dte) {
            LOG.severe("Corrupt start time data in flight file: " + dte.getMessage());
            throw new MalformedFlightFileException("Corrupt start time data in flight file: '" + dte.getMessage() + "'");
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
            throw new MalformedFlightFileException("Corrupt time data in flight file, start time was after the end time");
        }

        builder.setStartDateTime(startODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        builder.setEndDateTime(endODT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
