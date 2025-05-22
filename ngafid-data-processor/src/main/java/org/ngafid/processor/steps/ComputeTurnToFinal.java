package org.ngafid.processor.steps;

import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.TurnToFinal;
import org.ngafid.processor.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Performs turn to final analysis on the flight. Relies on the itinerary calculation which outputs a "dummy" column.
 */
public class ComputeTurnToFinal extends ComputeStep {
    private static Logger LOG = Logger.getLogger(ComputeTurnToFinal.class.getName());

    public ComputeTurnToFinal(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public boolean applicable() {
        return airframeIsValid(builder.meta.airframe)
                && builder
                .getDoubleTimeSeriesKeySet()
                .containsAll(getRequiredDoubleColumns().stream().filter(c -> !c.equals("_itinerary")).toList());
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
        LOG.info("Compuiting turn to final.");
        var x = TurnToFinal.calculateFlightTurnToFinals(
                builder.getDoubleTimeSeriesMap(), builder.getItinerary(), builder.meta.airframe, builder.meta.getStartDateTime()
        );
        LOG.info("Computed " + x.size() + " turns to final.");
        builder.emitTurnToFinals(x);
    }
}
