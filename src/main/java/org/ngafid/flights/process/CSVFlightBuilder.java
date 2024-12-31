package org.ngafid.flights.process;

import java.sql.Connection;

import java.util.Map;
import java.util.List;

import org.ngafid.flights.*;

/**
 * Flight builder for CSVFlightBuilder
 */
public class CSVFlightBuilder extends FlightBuilder {

    public CSVFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    private static final List<ProcessStep.Factory> PROCESS_STEPS = List.of();

    // This can be overridden.
    protected List<ProcessStep> gatherSteps(Connection connection) {
        // Add all of our processing steps here...
        // The order doesn't matter; the DependencyGraph will resolve
        // the order in the event that there are dependencies.
        List<ProcessStep> steps = super.gatherSteps(connection);
        PROCESS_STEPS.stream().map(factory -> factory.create(connection, this)).forEach(steps::add);
        return steps;
    }
}
