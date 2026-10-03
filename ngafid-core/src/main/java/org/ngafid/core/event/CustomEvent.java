package org.ngafid.core.event;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.ngafid.core.Database;
import org.ngafid.core.flights.Flight;

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

    /**
     * Constructs a custom event for a flight, delegating the time/line window, event-definition id, and severity to the
     * base {@link Event} and retaining the flight and the full event definition for later persistence.
     *
     * @param startTime the event's start timestamp
     * @param endTime the event's end timestamp
     * @param startLine the index of the first sample in the event window
     * @param endLine the index of the last sample in the event window
     * @param severity the computed event severity
     * @param flight the flight this event belongs to
     * @param eventDefinition the definition describing this custom event; its id is passed to the base event
     */
    public CustomEvent(
            String startTime,
            String endTime,
            int startLine,
            int endLine,
            double severity,
            Flight flight,
            EventDefinition eventDefinition) {
        super(startTime, endTime, startLine, endLine, eventDefinition.getId(), severity);

        this.flight = flight;
        this.customEventDefinition = eventDefinition;
    }

    /**
     * Constructs a custom event with no event definition attached (the definition may be set later via
     * {@link #setDefinition}).
     *
     * @param startTime the event's start timestamp
     * @param endTime the event's end timestamp
     * @param startLine the index of the first sample in the event window
     * @param endLine the index of the last sample in the event window
     * @param severity the computed event severity
     * @param flight the flight this event belongs to
     */
    public CustomEvent(String startTime, String endTime, int startLine, int endLine, double severity, Flight flight) {
        this(startTime, endTime, startLine, endLine, severity, flight, null);
    }

    /**
     * Loads the "Low Ending Fuel" event definition for a given airframe, opening its own database connection.
     *
     * @param airframeID the airframe id to load the definition for
     * @return the low-ending-fuel event definition for that airframe
     * @throws IOException if loading the definition fails
     * @throws SQLException if the query fails
     */
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

    /**
     * Persists this custom event, deriving the fleet id, flight id, and event-definition id from the attached flight
     * and custom definition and delegating to the base {@link Event#updateDatabase} insert.
     *
     * @param connection the database connection
     * @throws IOException if the base persistence step performs I/O that fails
     * @throws SQLException if the insert fails
     */
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
