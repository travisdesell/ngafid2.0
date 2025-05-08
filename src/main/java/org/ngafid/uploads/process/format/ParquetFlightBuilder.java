package org.ngafid.uploads.process.format;
import org.ngafid.flights.*;
import org.ngafid.uploads.process.FlightMeta;
import org.ngafid.uploads.process.steps.*;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import static org.ngafid.uploads.process.steps.ComputeStep.required;

public class ParquetFlightBuilder extends FlightBuilder {

    public ParquetFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                                Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    @Override
    protected List<ComputeStep> gatherSteps(Connection connection) {
        List<ComputeStep.Factory> parquetSteps = List.of(
                required(ComputeStartEndTime::new),
                ComputeAltAGL::new,
                ComputeAirportProximity::new,
                ComputeLaggedAltMSL::new
               //Skipping steps:  ComputeTurnToFinal::new, ComputeItinerary::new, ComputeLOCI::new, ComputeTotalFuel::new, ComputeDivergence::new,  ComputeStallIndex::new,
        );

        ArrayList<ComputeStep> steps =
                parquetSteps.stream()
                        .map(factory -> factory.create(connection, this))
                        .filter(step ->
                                step.getOutputColumns().stream()
                                        .noneMatch(x ->
                                                getDoubleTimeSeriesMap().containsKey(x) ||
                                                        getStringTimeSeriesMap().containsKey(x)
                                        )
                        )
                        .collect(Collectors.toCollection(ArrayList::new));

        if (!getDoubleTimeSeriesMap().containsKey(Parameters.UNIX_TIME_SECONDS) ||
                !getStringTimeSeriesMap().containsKey(Parameters.UTC_DATE_TIME)) {
            steps.add(required(ComputeUTCTime::new).create(connection, this));
        }
        return steps;
    }
}
