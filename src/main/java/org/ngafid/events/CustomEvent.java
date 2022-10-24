package org.ngafid.events;

import org.ngafid.*;
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
    private Flight flight;

    public static final EventDefinition HIGH_ALTITUDE_SPIN = EventDefinition.getEventDefinition(Database.getConnection(), "High Altitude Spin");
    public static final EventDefinition LOW_ALTITUDE_SPIN = EventDefinition.getEventDefinition(Database.getConnection(), "Low Altitude Spin");
    public static final EventDefinition LOW_END_FUEL_PA_28 = EventDefinition.getEventDefinition(Database.getConnection(), "Low Ending Fuel", 1);
    public static final EventDefinition LOW_END_FUEL_CESSNA_172 = EventDefinition.getEventDefinition(Database.getConnection(), "Low Ending Fuel", 2);
    public static final EventDefinition LOW_END_FUEL_PA_44 = EventDefinition.getEventDefinition(Database.getConnection(), "Low Ending Fuel", 3);

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
    
    public static EventDefinition getLowEndFuelDefinition(int airframeID) {
        return EventDefinition.getEventDefinition(Database.getConnection(), "Low Ending Fuel", airframeID);
    }

    public EventDefinition getDefinition() {
        return this.customEventDefinition;
    }

    public void updateDatabase(Connection connection) {
        super.updateDatabase(connection, flight.getFleetId(), flight.getId(), customEventDefinition.getId());
    }
}
