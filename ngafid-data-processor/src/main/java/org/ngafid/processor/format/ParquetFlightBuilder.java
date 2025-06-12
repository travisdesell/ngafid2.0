package org.ngafid.processor.format;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FlightMeta;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.processor.steps.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.ngafid.processor.steps.ComputeStep.required;


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
                ComputeAirportProximity::new
                //Skipping steps: ComputeLaggedAltMSL::new, ComputeTurnToFinal::new, ComputeItinerary::new, ComputeLOCI::new, ComputeTotalFuel::new, ComputeDivergence::new,  ComputeStallIndex::new,
        );

        ArrayList<ComputeStep> steps = parquetSteps.stream()
                .map(factory -> factory.create(connection, this))
                .collect(Collectors.toCollection(ArrayList::new));

        if (!getDoubleTimeSeriesMap().containsKey(Parameters.UNIX_TIME_SECONDS) ||
                !getStringTimeSeriesMap().containsKey(Parameters.UTC_DATE_TIME)) {
            steps.add(required(ComputeUTCTime::new).create(connection, this));
        }
        return steps;
    }
}
