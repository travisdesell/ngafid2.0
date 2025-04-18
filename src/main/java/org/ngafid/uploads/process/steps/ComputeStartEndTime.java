package org.ngafid.uploads.process.steps;

import org.ngafid.common.TimeUtils;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Computes the start and end time for the flight by looking at the first valid date found in the UTC_DATE_TIME series.
 */
public class ComputeStartEndTime extends ComputeStep {
    public ComputeStartEndTime(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return Set.of();
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Set.of(Parameters.UTC_DATE_TIME);
    }

    @Override
    public Set<String> getRequiredColumns() {
        return getRequiredStringColumns();
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of("_start_end_time");
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {

        StringTimeSeries utc = builder.getStringTimeSeries(Parameters.UTC_DATE_TIME);

        // find the first non-null time entry
        int start = 0;
        while (start < utc.size() && utc.emptyAt(start)) {
            start++;
        }

        if (start >= utc.size())
            throw new MalformedFlightFileException(
                    "Date, Time or Offset columns were all null! Cannot set start/end times.");

        // find the last full date time offset entry row
        int end = utc.size() - 1;
        while (end >= 0 && utc.emptyAt(end)) {
            end--;
        }
        var startODT = OffsetDateTime.parse(utc.get(start), TimeUtils.ISO_8601_FORMAT);
        var endODT = OffsetDateTime.parse(utc.get(end), TimeUtils.ISO_8601_FORMAT);

        builder
                .setStartDateTime(startODT)
                .setEndDateTime(endODT);
    }
}
