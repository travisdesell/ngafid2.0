package org.ngafid.processor.steps;

import org.ngafid.core.event.CustomEvent;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.processor.events.AbstractEventScanner;
import org.ngafid.processor.events.SpinEventScanner;
import org.ngafid.processor.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;

import static org.ngafid.core.flights.Parameters.VSPD;
import static org.ngafid.core.flights.Parameters.VSPD_CALCULATED;

/**
 * There are two event definitions for spin events, high and low altitude spins. The scanner searches for both simultaneously,
 * but two ComputeEvent objects will be created -- one for each definition. This class ensures that only one of these
 * computations is actually done.
 */
public class ComputeSpinEvents extends ComputeEvent {

    public ComputeSpinEvents(Connection connection, FlightBuilder fb, EventDefinition def, AbstractEventScanner scanner) {
        super(connection, fb, def, scanner);
        if (!(scanner instanceof SpinEventScanner))
            throw new IllegalArgumentException("scanner must be a SpinEventScanner");
    }

    @Override
    public boolean applicable() {
        return super.applicable() && (builder.getStringTimeSeries(VSPD_CALCULATED) != null || builder.getStringTimeSeries(VSPD) != null);
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        // High and Low are computed simultaneously, only run the computation once. We choose to only do it if the event definition is High (arbitrarily)
        if (this.definition.getId() == CustomEvent.getHighAltitudeSpin().getId()) {
            super.compute();
        }
    }

}
