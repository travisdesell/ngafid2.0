package org.ngafid.uploads.process.steps;

import org.ngafid.events.calculations.TurnToFinal;
import org.ngafid.flights.Parameters;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Performs turn to final analysis on the flight. Relies on the itinerary calculation which outputs a "dummy" column.
 */
public class ComputeTurnToFinal extends ComputeStep {

    public ComputeTurnToFinal(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return Set.of(Parameters.LAT, Parameters.LON, Parameters.ALT_AGL, Parameters.ALT_MSL, Parameters.ROLL, Parameters.GND_SPD, "_itinerary");
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Set.of();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return getRequiredDoubleColumns();
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of();
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        builder.emitTurnToFinals(
                TurnToFinal.calculateFlightTurnToFinals(
                        builder.getDoubleTimeSeriesMap(), builder.getItinerary(), builder.meta.airframe, builder.meta.getStartDateTime()
                )
        );
    }
}
