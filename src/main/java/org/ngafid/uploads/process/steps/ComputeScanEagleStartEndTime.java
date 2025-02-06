package org.ngafid.uploads.process.steps;

import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

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

        startDateTime += " " + firstTime;
        endDateTime += " " + lastTime;

        builder.meta.startDateTime = startDateTime;
        builder.meta.endDateTime = endDateTime;
    }
}
