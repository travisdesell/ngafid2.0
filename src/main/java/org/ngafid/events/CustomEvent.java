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
   
    public static final EventDefinition HIGH_ALTITUDE_SPIN = EventDefinition.getEventDefinition(connection, "High Altitude Spin");
    public static final EventDefinition LOW_ALTITUDE_SPIN = EventDefinition.getEventDefinition(connection, "Low Altitude Spin");

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight, EventDefinition eventDefinition) {
        super(startTime, endTime, startLine, endLine, severity);

        this.flight = flight;
        this.customEventDefinition = eventDefinition;
    }

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight) {
        this(startTime, endTime, startLine, endLine, severity, flight, null);
    }

    public void setDefinition(EventDefinition eventDefinition) {
        this.customEventDefinition = eventDefinition;
    }
    
    public static EventDefinition getLowFuelDefinition(int airframeID) {
        return EventDefinition.getEventDefinition(connection, -10 - airframeID);
    }

    public EventDefinition getDefinition() {
        return this.customEventDefinition;
    }

    public void updateDatabase(Connection connection) {
        super.updateDatabase(connection, flight.getFleetId(), flight.getId(), customEventDefinition.getId());
    }
}
