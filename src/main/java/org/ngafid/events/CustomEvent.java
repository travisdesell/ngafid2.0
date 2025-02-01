package org.ngafid.events;

import org.ngafid.common.Database;
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
    private static EventDefinition HIGH_ALTITUDE_SPIN = null;
    private static EventDefinition LOW_ALTITUDE_SPIN = null;
    private static EventDefinition LOW_END_FUEL_PA_28 = null;
    private static EventDefinition LOW_END_FUEL_CESSNA_172 = null;
    private static EventDefinition LOW_END_FUEL_PA_44 = null;

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

    private EventDefinition customEventDefinition;
    private final Flight flight;

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight,
                       EventDefinition eventDefinition) {
        super(startTime, endTime, startLine, endLine, severity);

        this.flight = flight;
        this.customEventDefinition = eventDefinition;
    }

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight) {
        this(startTime, endTime, startLine, endLine, severity, flight, null);
    }

    public static EventDefinition getLowEndFuelDefinition(int airframeID) throws IOException, SQLException {
        try (Connection connection = Database.getConnection()) {
            return EventDefinition.getEventDefinition(connection, "Low Ending Fuel", airframeID);
        }
    }

    public EventDefinition getDefinition() {
        return this.customEventDefinition;
    }

    public void setDefinition(EventDefinition eventDefinition) {
        this.customEventDefinition = eventDefinition;
    }

    public void updateDatabase(Connection connection) throws IOException, SQLException {
        super.updateDatabase(connection, flight.getFleetId(), flight.getId(), customEventDefinition.getId());
    }

    public static EventDefinition getHighAltitudeSpin() {
        return HIGH_ALTITUDE_SPIN;
    }

    public static EventDefinition getLowAltitudeSpin() {
        return LOW_ALTITUDE_SPIN;
    }

    public static EventDefinition getLowEndFuelPa28() {
        return LOW_END_FUEL_PA_28;
    }

    public static EventDefinition getLowEndFuelCessna172() {
        return LOW_END_FUEL_CESSNA_172;
    }

    public static EventDefinition getLowEndFuelPa44() {
        return LOW_END_FUEL_PA_44;
    }
}
