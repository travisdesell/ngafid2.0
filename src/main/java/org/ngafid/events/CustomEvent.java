package org.ngafid.events;

import org.ngafid.*;
import org.ngafid.flights.Flight;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A CustomEvent is an Event that is not able to be calculated by the NGAFID's
 * standard event calculation process.
 *
 * <a href=mailto:apl1341@rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class CustomEvent extends Event {
    private EventDefinition customEventDefinition;
    private Flight flight;

    public static EventDefinition HIGH_ALTITUDE_SPIN = null;
    public static EventDefinition LOW_ALTITUDE_SPIN = null;
    public static EventDefinition LOW_END_FUEL_PA_28 = null;
    public static EventDefinition LOW_END_FUEL_CESSNA_172 = null;
    public static EventDefinition LOW_END_FUEL_PA_44 = null;

    static {
        try (Connection connection = Database.getConnection()) {
            HIGH_ALTITUDE_SPIN = EventDefinition.getEventDefinition(connection, "High Altitude Spin");
            LOW_ALTITUDE_SPIN = EventDefinition.getEventDefinition(connection, "Low Altitude Spin");
            LOW_END_FUEL_PA_28 = EventDefinition.getEventDefinition(connection, "Low Ending Fuel", 1);
            LOW_END_FUEL_CESSNA_172 = EventDefinition.getEventDefinition(connection, "Low Ending Fuel", 2);
            LOW_END_FUEL_PA_44 = EventDefinition.getEventDefinition(connection, "Low Ending Fuel", 3);
        } catch (IOException | SQLException e) {
            System.err.println("Failed to get connection.");
            System.exit(1);
        }
    }

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight,
            EventDefinition eventDefinition) {
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

    public static EventDefinition getLowEndFuelDefinition(int airframeID) throws IOException, SQLException {
        try (Connection connection = Database.getConnection()) {
            return EventDefinition.getEventDefinition(connection, "Low Ending Fuel", airframeID);
        }
    }

    public EventDefinition getDefinition() {
        return this.customEventDefinition;
    }

    public void updateDatabase(Connection connection) throws IOException, SQLException {
        super.updateDatabase(connection, flight.getFleetId(), flight.getId(), customEventDefinition.getId());
    }
}
