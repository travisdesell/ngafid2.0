package org.ngafid.events;

import org.ngafid.*;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Flight;

import java.sql.Connection;

/**
 * A CustomEvent is an Event that is not able to be calculated by the NGAFID's
 * standard event calculation process.
 *
 * <a href=mailto:apl1341@rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class CustomEvent extends Event {
    private EventDefinition customEventDefinition;
    private static final Connection connection = Database.getConnection();
    private Flight flight;

    public static final EventDefinition SPIN_START = EventDefinition.getEventDefinition(connection, "Spin Start");

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight, EventDefinition customEventDefinition) {
        super(startTime, endTime, startLine, endLine, severity);

        this.flight = flight;
        this.customEventDefinition = customEventDefinition;
    }

    public void updateDatabase(Connection connection) {
        super.updateDatabase(connection, flight.getFleetId(), flight.getId(), customEventDefinition.getId());
    }

    public static EventDefinition getLowFuelDefinition(int airframeID) {
        return EventDefinition.getEventDefinition(connection, -10 - airframeID);
    }
}
