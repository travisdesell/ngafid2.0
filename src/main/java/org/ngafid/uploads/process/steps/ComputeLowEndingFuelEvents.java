package org.ngafid.uploads.process.steps;

import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.events.CustomEvent.LOW_FUEL_EVENT_DEFINITIONS;
import static org.ngafid.events.CustomEvent.LOW_FUEL_EVENT_THRESHOLDS;
import static org.ngafid.flights.Parameters.*;

public class ComputeLowEndingFuelEvents extends ComputeStep {

    private static final Logger LOG = Logger.getLogger(ComputeLowEndingFuelEvents.class.getName());

    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(TOTAL_FUEL);
    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(LCL_DATE, LCL_TIME);
    private static final Set<String> OUTPUT_COLUMNS = Set.of();

    public ComputeLowEndingFuelEvents(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getRequiredStringColumns() {
        return REQUIRED_STRING_COLUMNS;
    }

    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(Airframes.Airframe airframe) {
        return LOW_FUEL_EVENT_DEFINITIONS.containsKey(airframe.getId());
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        int airframeNameID = builder.meta.airframe.getId();

        EventDefinition eventDef = LOW_FUEL_EVENT_DEFINITIONS.get(airframeNameID);
        double threshold = LOW_FUEL_EVENT_THRESHOLDS.get(airframeNameID);

        DoubleTimeSeries fuel = builder.getDoubleTimeSeries(TOTAL_FUEL);
        StringTimeSeries date = builder.getStringTimeSeries(LCL_DATE);
        StringTimeSeries time = builder.getStringTimeSeries(LCL_TIME);

        String[] lastValidDateAndIndex = date.getLastValidAndIndex();
        int i = Integer.parseInt(lastValidDateAndIndex[1]);
        LOG.info("last valid date and index: " + i);

        String endTime = lastValidDateAndIndex[0] + " " + time.getLastValid();

        String currentTime = endTime;
        double duration = 0;
        double fuelSum = 0;
        int fuelValues = 0;

        for (; duration <= 15 && i >= 0; i--) {
            currentTime = date.get(i) + " " + time.get(i);
            fuelSum += fuel.get(i);
            fuelValues++;

            if (currentTime.equals(" ")) {
                continue;
            }

            LOG.info("DATE = " + currentTime);
            duration = TimeUtils.calculateDurationInSeconds(currentTime, endTime, "yyyy-MM-dd HH:mm:ss");
        }

        if (duration >= 15) {
            double average = (fuelSum / fuelValues);
            if (average < threshold) {
                builder.emitEvent(new CustomEvent(currentTime, endTime, i, i + fuelValues, average, null, eventDef));
            }
        }
    }
}
