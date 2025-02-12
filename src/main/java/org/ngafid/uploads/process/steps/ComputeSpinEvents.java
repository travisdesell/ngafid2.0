package org.ngafid.uploads.process.steps;

import org.ngafid.events.AbstractEventScanner;
import org.ngafid.events.CustomEvent;
import org.ngafid.events.EventDefinition;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;

import static org.ngafid.flights.Parameters.VSPD;
import static org.ngafid.flights.Parameters.VSPD_CALCULATED;

public class ComputeSpinEvents extends ComputeEvent {

    public ComputeSpinEvents(Connection connection, FlightBuilder fb, EventDefinition def, AbstractEventScanner scanner) {
        super(connection, fb, def, scanner);
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
