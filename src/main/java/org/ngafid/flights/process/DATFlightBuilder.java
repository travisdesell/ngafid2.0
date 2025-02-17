package org.ngafid.flights.process;

import java.sql.Connection;

import java.util.Map;
import java.util.List;

import org.ngafid.flights.*;
import static org.ngafid.flights.Parameters.*;

public class DATFlightBuilder extends FlightBuilder {

    public DATFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    // Steps specific to DAT files. None so far
    private static final List<ProcessStep.Factory> PROCESS_STEPS = List.of();

    // This can be overridden.
    protected List<ProcessStep> gatherSteps(Connection connection) {
        // Add all of our processing steps here...
        // The order doesn't matter; the DependencyGraph will resolve
        // the order in the event that there are dependencies.
        List<ProcessStep> steps = super.gatherSteps(connection);
        PROCESS_STEPS.stream().map(factory -> factory.create(connection, this)).forEach(steps::add);

        // Not every DAT file has AGL.
        if (getDoubleTimeSeries(ALT_AGL) == null) {
            steps.add(new ProcessAltAGL(connection, this));
        }

        return steps;
    }
}
