package org.ngafid.core.event;

import org.ngafid.core.Database;
import org.ngafid.core.flights.Flight;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    // These maps absolutely should not be written to after static initialization.
    public static final Map<Integer, EventDefinition> LOW_FUEL_EVENT_DEFINITIONS = new HashMap<>();
    public static final Map<Integer, Double> LOW_FUEL_EVENT_THRESHOLDS = new HashMap<>();

    static {
        try (Connection connection = Database.getConnection()) {
            HIGH_ALTITUDE_SPIN = EventDefinition.getEventDefinition(connection, "High Altitude Spin");
            LOW_ALTITUDE_SPIN = EventDefinition.getEventDefinition(connection, "Low Altitude Spin");
            LOW_END_FUEL_PA_28 = EventDefinition.getEventDefinition(connection, "Low Ending Fuel", 1);
            LOW_END_FUEL_CESSNA_172 = EventDefinition.getEventDefinition(connection, "Low Ending Fuel", 2);
            LOW_END_FUEL_PA_44 = EventDefinition.getEventDefinition(connection, "Low Ending Fuel", 3);

            LOW_FUEL_EVENT_DEFINITIONS.put(1, LOW_END_FUEL_PA_28);
            LOW_FUEL_EVENT_THRESHOLDS.put(1, getLowFuelEventThreshold(LOW_END_FUEL_PA_28));

            LOW_FUEL_EVENT_DEFINITIONS.put(2, LOW_END_FUEL_CESSNA_172);
            LOW_FUEL_EVENT_THRESHOLDS.put(2, getLowFuelEventThreshold(LOW_END_FUEL_CESSNA_172));

            LOW_FUEL_EVENT_DEFINITIONS.put(3, LOW_END_FUEL_PA_44);
            LOW_FUEL_EVENT_THRESHOLDS.put(3, getLowFuelEventThreshold(LOW_END_FUEL_PA_44));
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* Calling this bad practice is an understatement... */
    private static double getLowFuelEventThreshold(EventDefinition def) {
        String text = def.toHumanReadable();
        return Double.parseDouble(text.substring(text.lastIndexOf(" ") + 1));
    }

    private EventDefinition customEventDefinition;
    private final Flight flight;

    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight,
                       EventDefinition eventDefinition) {
        super(startTime, endTime, startLine, endLine, eventDefinition.getId(), severity);

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
