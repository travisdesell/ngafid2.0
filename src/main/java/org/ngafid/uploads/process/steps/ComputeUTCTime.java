package org.ngafid.uploads.process.steps;

import org.ngafid.common.TimeUtils;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.common.TimeUtils.ISO_8601_FORMAT;
import static org.ngafid.flights.Parameters.*;

/**
 * Combines LCL_DATE, LCL_TIME, and UTC_OFFSET into a ISO 8601 extended offset date time. This time coordinate is then
 * translated into a double time series of unix time values, which can be used for fast date comparisons.
 * <p>
 * The start and end time values are then computed, these must be in UTC time (i.e. GMT time, 0 offset) as sql does not
 * support timezones in DATETIME columns.
 * <p>
 * The LCL_DATE column can be formatted in a number of ways and this will be automatically deduced. A lot of the key
 * date-manipulation code is contained in {@link org.ngafid.common.TimeUtils}.
 */
public class ComputeUTCTime extends ComputeStep {
    private static final Logger LOG = Logger.getLogger(ComputeUTCTime.class.getName());

    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(LCL_DATE, LCL_TIME, UTC_OFFSET);

    public ComputeUTCTime(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return Collections.<String>emptySet();
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return REQUIRED_STRING_COLUMNS;
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_STRING_COLUMNS;
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of(UTC_DATE_TIME, UNIX_TIME_SECONDS);
    }

    public boolean stringEmpty(String date) {
        return date == null || date.trim().isEmpty();
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        StringTimeSeries dates = builder.getStringTimeSeries(LCL_DATE);
        StringTimeSeries times = builder.getStringTimeSeries(LCL_TIME);
        StringTimeSeries offsets = builder.getStringTimeSeries(UTC_OFFSET);

        // Walk through the time series until we have a valid date, then search for the right formatter.
        DateTimeFormatter formatter = null;
        try {
            for (int i = 0; i < dates.size(); i++) {
                if (dates.get(i).isEmpty() || times.get(i).isEmpty()) {
                    continue;
                }

                formatter = TimeUtils.findCorrectFormatter(dates.get(i), times.get(i));
                break;
            }
        } catch (TimeUtils.UnrecognizedDateTimeFormatException e) {
            throw new FatalFlightFileException(e.getMessage());
        }

        if (formatter == null) {
            throw new MalformedFlightFileException("Flight file contained no valid dates.");
        }

        StringTimeSeries timestampSeries = new StringTimeSeries(UTC_DATE_TIME, Unit.UTC_DATE_TIME.toString());
        DoubleTimeSeries unixtime = new DoubleTimeSeries(UNIX_TIME_SECONDS, Unit.SECONDS.toString());
        for (int i = 0; i < dates.size(); i++) {
            if (stringEmpty(dates.get(i)) || stringEmpty(times.get(i)) || stringEmpty(offsets.get(i))) {
                timestampSeries.add("");
                unixtime.add(Double.NaN);
                continue;
            }

            LocalDateTime local = LocalDateTime.parse(dates.get(i) + " " + times.get(i), formatter);
            ZoneOffset zoneOffset = ZoneOffset.of(offsets.get(i));
            OffsetDateTime odt = OffsetDateTime.of(local, zoneOffset);
            timestampSeries.add(odt.format(ISO_8601_FORMAT));
            unixtime.add(odt.toEpochSecond());
        }

        builder.addTimeSeries(timestampSeries);
        builder.addTimeSeries(unixtime);

    }
}
