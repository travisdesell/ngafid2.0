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
 * Computes the start and end time for the scaneagle airframe.
 */
public class ComputeScanEagleStartEndTime extends ComputeStep {

    public ComputeScanEagleStartEndTime(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return Set.of();
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Set.of(Parameters.SCAN_EAGLE_GPS_TIME);
    }

    @Override
    public Set<String> getRequiredColumns() {
        return getRequiredStringColumns();
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of("_start_end_time");
    }

    /**
     * TODO: This function will not properly import data if the UTC time crosses midnight, there is a hacky solution.
     *
     * @throws SQLException
     * @throws MalformedFlightFileException
     * @throws FatalFlightFileException
     */
    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        StringTimeSeries times = builder.getStringTimeSeries(Parameters.SCAN_EAGLE_GPS_TIME);

        String[] filenameParts = builder.meta.filename.split("_");
        String startDateTime = filenameParts[0];
        String endDateTime = startDateTime;

        String firstTime = null;
        for (int i = 0; i < times.size(); i++) {
            if (times.get(i) != null && !times.get(i).isEmpty()) {
                firstTime = times.get(i);
                break;
            }
        }

        String lastTime = null;
        for (int i = times.size() - 1; i >= 0; i--) {
            if (times.get(i) != null && !times.get(i).isEmpty()) {
                lastTime = times.get(i);
                break;
            }
        }

        // TODO: can't get time offset from lat/long because they aren't being set
        // correctly

        startDateTime += "T" + firstTime + "Z";
        endDateTime += "T" + lastTime + "Z";

        var startODT = OffsetDateTime.parse(startDateTime, TimeUtils.ISO_8601_FORMAT);
        var endODT = OffsetDateTime.parse(endDateTime, TimeUtils.ISO_8601_FORMAT);

        if (startODT.isAfter(endODT)) {
            endODT = endODT.plusDays(1);
        }

        builder
                .setStartDateTime(startODT)
                .setEndDateTime(endODT);
    }
}
